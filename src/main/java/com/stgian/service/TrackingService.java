package com.stgian.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stgian.dto.TrackingDTOs;
import com.stgian.model.Order;
import com.stgian.model.User;
import com.stgian.repository.OrderRepository;
import com.stgian.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class TrackingService {

    private static final Logger log = Logger.getLogger(TrackingService.class.getName());
    private static final String CORREIOS_API   = "https://api.correios.com.br/srorastro/v1/objetos/";
    private static final String LINK_RASTREIO  = "https://rastreamento.correios.com.br/app/index.php?P_LINGUA=001&P_TIPO=001&P_COD_UNI=";

    private final OrderRepository orderRepository;
    private final UserRepository  userRepository;
    private final ObjectMapper    objectMapper = new ObjectMapper();

    public TrackingService(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository  = userRepository;
    }

    @Transactional
    public TrackingDTOs.TrackingResponse setTracking(Long orderId, TrackingDTOs.SetTrackingRequest req) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + orderId));

        // Valida formato básico do código (Correios: AA999999999BR)
        String code = req.trackingCode().trim().toUpperCase();
        if (code.length() < 8 || code.length() > 20) {
            throw new IllegalArgumentException("Código de rastreio inválido.");
        }

        String carrier = (req.carrier() != null && !req.carrier().isBlank()) ? req.carrier() : "Correios";
        String url     = LINK_RASTREIO + code;

        order.setTrackingCode(code);
        order.setTrackingCarrier(carrier);
        order.setTrackingUrl(url);
        order.setTrackingStatus("Código registrado. Aguardando postagem.");
        order.setTrackingUpdatedAt(LocalDateTime.now());

        if (order.getStatus() == Order.Status.PROCESSING ||
            order.getStatus() == Order.Status.PENDING_PAYMENT) {
            order.setStatus(Order.Status.SHIPPED);
        }

        orderRepository.save(order);
        log.info("Rastreio definido: " + order.getOrderCode() + " → " + code);
        return buildResponse(order, List.of());
    }

    public TrackingDTOs.TrackingResponse getTracking(String orderCode, Long userId) {
        Order order = orderRepository.findByOrderCode(orderCode)
            .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + orderCode));

        // FIX: CLIENT só acessa seus próprios pedidos
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + userId));
        if (user.getRole() == User.Role.CLIENT && !order.getUser().getId().equals(userId)) {
            throw new SecurityException("Acesso negado.");
        }

        if (order.getTrackingCode() == null || order.getTrackingCode().isBlank()) {
            return buildResponse(order, List.of());
        }

        List<TrackingDTOs.TrackingEvent> events = fetchCorreiosEvents(order.getTrackingCode());

        if (!events.isEmpty()) {
            String latestStatus = events.get(0).description();
            order.setTrackingStatus(latestStatus);
            order.setTrackingUpdatedAt(LocalDateTime.now());

            if (latestStatus.toLowerCase().contains("entregue") ||
                latestStatus.toLowerCase().contains("objeto entregue")) {
                order.setStatus(Order.Status.DELIVERED);
            }
            orderRepository.save(order);
        }

        return buildResponse(order, events);
    }

    private List<TrackingDTOs.TrackingEvent> fetchCorreiosEvents(String code) {
        List<TrackingDTOs.TrackingEvent> events = new ArrayList<>();
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CORREIOS_API + code + "?resultado=T"))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root    = objectMapper.readTree(response.body());
                JsonNode objetos = root.path("objetos");

                if (objetos.isArray() && objetos.size() > 0) {
                    JsonNode evArr = objetos.get(0).path("eventos");
                    if (evArr.isArray()) {
                        for (JsonNode ev : evArr) {
                            String descricao = ev.path("descricao").asText("");
                            String detalhe   = ev.path("detalhe").asText("");
                            String desc      = descricao + (detalhe.isBlank() ? "" : " — " + detalhe);
                            String dtHr      = ev.path("dtHrCriado").asText("");
                            String date      = dtHr.length() >= 10 ? dtHr.substring(0, 10) : dtHr;
                            String time      = dtHr.length() >= 16 ? dtHr.substring(11, 16) : "";
                            JsonNode unidade = ev.path("unidade");
                            String local     = "";
                            if (!unidade.isMissingNode()) {
                                String cidade = unidade.path("endereco").path("cidade").asText("");
                                String uf     = unidade.path("endereco").path("uf").asText("");
                                local = cidade + (uf.isBlank() ? "" : "/" + uf);
                            }
                            events.add(new TrackingDTOs.TrackingEvent(date, time, desc, local));
                        }
                    }
                }
            } else {
                log.warning("Correios HTTP " + response.statusCode() + " para " + code);
            }
        } catch (Exception e) {
            log.warning("Erro Correios para " + code + ": " + e.getMessage());
        }
        return events;
    }

    private TrackingDTOs.TrackingResponse buildResponse(Order order, List<TrackingDTOs.TrackingEvent> events) {
        return new TrackingDTOs.TrackingResponse(
            order.getOrderCode(), order.getTrackingCode(), order.getTrackingCarrier(),
            order.getTrackingUrl(), order.getTrackingStatus(), order.getTrackingUpdatedAt(), events
        );
    }
}
