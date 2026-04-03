package com.stgian.controller;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import com.stgian.dto.OrderDTOs;
import com.stgian.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private static final Logger log = Logger.getLogger(PaymentWebhookController.class.getName());

    private final OrderService orderService;

    public PaymentWebhookController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ── Webhook automático do MP ───────────────────────────────────────────
    // O MP chama este endpoint quando o status muda
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(value = "type",  required = false) String type,
            @RequestParam(value = "id",    required = false) String id,
            @RequestBody(required = false) Map<String, Object> body) {

        log.info("Webhook MP recebido. type=" + type + " id=" + id);

        try {
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
            log.severe("Erro ao processar webhook: " + e.getMessage());
        }

        // Sempre retorna 200 — se retornar erro o MP fica retentando
        return ResponseEntity.ok().build();
    }

    // ── Endpoint chamado pelo frontend ao retornar do MP ──────────────────
    // Garante atualização mesmo quando o webhook não chegou
    // Público — não exige token (o cliente acabou de pagar e pode não estar logado)
    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmPayment(
            @RequestParam(value = "paymentId",  required = false) String paymentId,
            @RequestParam(value = "orderCode",  required = false) String orderCode,
            @RequestParam(value = "status",     required = false) String status) {

        log.info("Confirmação frontend: paymentId=" + paymentId
            + " orderCode=" + orderCode + " status=" + status);

        try {
            if (paymentId != null && !paymentId.isBlank()) {
                // Consulta diretamente no MP para status real
                processPaymentNotification(paymentId);
                log.info("Pagamento confirmado via frontend: " + paymentId);
            } else if (orderCode != null && "approved".equals(status)) {
                // Fallback: se não tem paymentId mas o MP disse approved
                orderService.handlePaymentApproved(orderCode);
                log.info("Pedido aprovado via fallback: " + orderCode);
            }
        } catch (Exception e) {
            log.warning("Erro na confirmação frontend: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of("ok", "true"));
    }

    // ── Consulta status atual de um pedido pelo orderCode ─────────────────
    // O frontend faz polling neste endpoint para atualizar a tela
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<Map<String, String>> getPaymentStatus(
            @PathVariable String orderCode) {
        try {
            OrderDTOs.OrderResponse order = orderService.getByCode(orderCode);
            return ResponseEntity.ok(Map.of(
                "orderCode",    order.orderCode(),
                "status",       order.status(),
                "paymentStatus", order.paymentStatus() != null ? order.paymentStatus() : "PENDING"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "orderCode",     orderCode,
                "status",        "PENDING_PAYMENT",
                "paymentStatus", "PENDING"
            ));
        }
    }

    // ── Retorno do MP via back_url (GET) ───────────────────────────────────
    @GetMapping("/success")
    public ResponseEntity<Map<String, String>> paymentSuccess(
            @RequestParam(value = "payment_id",        required = false) String paymentId,
            @RequestParam(value = "status",            required = false) String status,
            @RequestParam(value = "external_reference",required = false) String orderCode) {

        log.info("Retorno MP: paymentId=" + paymentId + " status=" + status + " order=" + orderCode);

        if (paymentId != null && "approved".equals(status)) {
            processPaymentNotification(paymentId);
        }

        return ResponseEntity.ok(Map.of(
            "message",   "Pagamento processado",
            "orderCode", orderCode != null ? orderCode : "",
            "status",    status    != null ? status    : ""
        ));
    }

    // ── Lógica central: consulta MP e atualiza pedido ─────────────────────
    private void processPaymentNotification(String mpPaymentId) {
        try {
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.parseLong(mpPaymentId));

            String orderCode     = payment.getExternalReference();
            String paymentStatus = payment.getStatus();

            log.info("MP pagamento " + mpPaymentId + ": status=" + paymentStatus
                + " orderCode=" + orderCode);

            if (orderCode == null || orderCode.isBlank()) return;

            switch (paymentStatus) {
                case "approved"              -> orderService.handlePaymentApproved(orderCode);
                case "rejected"              -> orderService.handlePaymentRejected(orderCode);
                case "pending", "in_process" -> orderService.handlePaymentPending(orderCode, mpPaymentId);
                default -> log.info("Status MP ignorado: " + paymentStatus);
            }

        } catch (Exception e) {
            log.severe("Erro ao consultar MP paymentId=" + mpPaymentId + ": " + e.getMessage());
        }
    }
}
