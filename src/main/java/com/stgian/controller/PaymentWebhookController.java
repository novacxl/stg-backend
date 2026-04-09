package com.stgian.controller;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.stgian.repository.OrderRepository;
import com.stgian.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private static final Logger log = Logger.getLogger(PaymentWebhookController.class.getName());

    private final OrderService     orderService;
    private final OrderRepository  orderRepository;

    @Value("${stgian.mp-webhook-secret:}")
    private String webhookSecret;

    public PaymentWebhookController(OrderService orderService,
                                    OrderRepository orderRepository) {
        this.orderService    = orderService;
        this.orderRepository = orderRepository;
    }

    /**
     * FIX: Endpoint de status para o polling do frontend.
     * Público — não requer autenticação (o orderCode já é suficientemente opaco).
     * Retorna apenas status/paymentStatus — sem dados sensíveis.
     */
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<Map<String, String>> status(@PathVariable String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
            .map(order -> ResponseEntity.ok(Map.of(
                "orderCode",     order.getOrderCode(),
                "status",        order.getStatus().name(),
                "paymentStatus", order.getPaymentStatus() != null ? order.getPaymentStatus() : "PENDING"
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "x-signature",  required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "type",           required = false) String type,
            @RequestParam(value = "id",             required = false) String id,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {

        log.info("Webhook MP. type=" + type + " id=" + id);

        try {
            if (webhookSecret != null && !webhookSecret.isBlank() && xSignature != null) {
                String dataId = id;
                if (dataId == null && body != null && body.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    if (data != null) dataId = String.valueOf(data.get("id"));
                }
                if (!validateSignature(xSignature, xRequestId, dataId)) {
                    log.warning("Assinatura MP inválida. Ignorando.");
                    return ResponseEntity.ok().build();
                }
            }

            String eventType = type;
            String paymentId = id;

            if (body != null) {
                if (eventType == null) eventType = (String) body.get("type");
                if (paymentId == null && body.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) body.get("data");
                    if (data != null) paymentId = String.valueOf(data.get("id"));
                }
            }

            if ("payment".equals(eventType) && paymentId != null) {
                processPaymentNotification(paymentId);
            }

        } catch (Exception e) {
            log.severe("Erro webhook: " + e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/success")
    public ResponseEntity<Map<String, String>> paymentSuccess(
            @RequestParam(value = "payment_id",         required = false) String paymentId,
            @RequestParam(value = "status",             required = false) String status,
            @RequestParam(value = "external_reference", required = false) String orderCode) {

        log.info("Retorno MP: paymentId=" + paymentId + " status=" + status + " order=" + orderCode);
        if (paymentId != null && "approved".equals(status)) processPaymentNotification(paymentId);

        return ResponseEntity.ok(Map.of(
            "message",   "Pagamento processado",
            "orderCode", orderCode != null ? orderCode : "",
            "status",    status    != null ? status    : ""
        ));
    }

    private boolean validateSignature(String xSignature, String xRequestId, String dataId) {
        try {
            String timestamp = null, hashRecebido = null;
            for (String part : xSignature.split(",")) {
                if (part.startsWith("ts=")) timestamp    = part.substring(3);
                if (part.startsWith("v1=")) hashRecebido = part.substring(3);
            }
            if (timestamp == null || hashRecebido == null) return false;

            String template = "id:" + dataId
                + ";request-id:" + (xRequestId != null ? xRequestId : "")
                + ";ts:" + timestamp + ";";

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hashBytes = mac.doFinal(template.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString().equals(hashRecebido);
        } catch (Exception e) {
            log.severe("Erro validação assinatura: " + e.getMessage());
            return false;
        }
    }

    private void processPaymentNotification(String mpPaymentId) {
        try {
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.parseLong(mpPaymentId));
            String orderCode    = payment.getExternalReference();
            String paymentStatus = payment.getStatus();
            log.info("Pagamento " + mpPaymentId + ": status=" + paymentStatus + " order=" + orderCode);
            if (orderCode == null || orderCode.isBlank()) return;
            switch (paymentStatus) {
                case "approved"              -> orderService.handlePaymentApproved(orderCode);
                case "rejected"              -> orderService.handlePaymentRejected(orderCode);
                case "pending", "in_process" -> orderService.handlePaymentPending(orderCode, mpPaymentId);
                default -> log.info("Status MP ignorado: " + paymentStatus);
            }
        } catch (Exception e) {
            log.severe("Erro ao consultar pagamento " + mpPaymentId + ": " + e.getMessage());
        }
    }
}
