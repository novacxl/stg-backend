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
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @Value("${stgian.mp-webhook-secret:}")
    private String webhookSecret;

    public PaymentWebhookController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService; this.orderRepository = orderRepository;
    }

    // Endpoint para o polling do frontend — PUBLIC
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<Map<String, String>> status(@PathVariable String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
            .map(o -> ResponseEntity.ok(Map.of(
                "orderCode",     o.getOrderCode(),
                "status",        o.getStatus().name(),
                "paymentStatus", o.getPaymentStatus() != null ? o.getPaymentStatus() : "PENDING"
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "x-signature",  required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "id",   required = false) String id,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {

        log.info("Webhook MP. type=" + type + " id=" + id);
        try {
            if (webhookSecret != null && !webhookSecret.isBlank() && xSignature != null) {
                String dataId = id;
                if (dataId == null && body != null && body.containsKey("data")) {
                    @SuppressWarnings("unchecked") Map<String,Object> d = (Map<String,Object>) body.get("data");
                    if (d != null) dataId = String.valueOf(d.get("id"));
                }
                if (!validateSignature(xSignature, xRequestId, dataId)) {
                    log.warning("Assinatura MP inválida."); return ResponseEntity.ok().build();
                }
            }
            String eventType = type; String paymentId = id;
            if (body != null) {
                if (eventType == null) eventType = (String) body.get("type");
                if (paymentId == null && body.containsKey("data")) {
                    @SuppressWarnings("unchecked") Map<String,Object> d = (Map<String,Object>) body.get("data");
                    if (d != null) paymentId = String.valueOf(d.get("id"));
                }
            }
            if ("payment".equals(eventType) && paymentId != null) processPaymentNotification(paymentId);
        } catch (Exception e) { log.severe("Erro webhook: " + e.getMessage()); }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/success")
    public ResponseEntity<Map<String, String>> paymentSuccess(
            @RequestParam(value = "payment_id",          required = false) String paymentId,
            @RequestParam(value = "status",              required = false) String status,
            @RequestParam(value = "external_reference",  required = false) String orderCode) {
        if (paymentId != null && "approved".equals(status)) processPaymentNotification(paymentId);
        return ResponseEntity.ok(Map.of("orderCode", orderCode != null ? orderCode : "", "status", status != null ? status : ""));
    }

    private boolean validateSignature(String xSig, String xReqId, String dataId) {
        try {
            String ts = null, hash = null;
            for (String p : xSig.split(",")) {
                if (p.startsWith("ts=")) ts = p.substring(3);
                if (p.startsWith("v1=")) hash = p.substring(3);
            }
            if (ts == null || hash == null) return false;
            String tmpl = "id:" + dataId + ";request-id:" + (xReqId != null ? xReqId : "") + ";ts:" + ts + ";";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hb = mac.doFinal(tmpl.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hb) sb.append(String.format("%02x", b));
            return sb.toString().equals(hash);
        } catch (Exception e) { return false; }
    }

    private void processPaymentNotification(String mpPaymentId) {
        try {
            Payment payment = new PaymentClient().get(Long.parseLong(mpPaymentId));
            String code = payment.getExternalReference(), status = payment.getStatus();
            if (code == null || code.isBlank()) return;
            switch (status) {
                case "approved"              -> orderService.handlePaymentApproved(code);
                case "rejected"              -> orderService.handlePaymentRejected(code);
                case "pending", "in_process" -> orderService.handlePaymentPending(code, mpPaymentId);
                default -> log.info("Status MP ignorado: " + status);
            }
        } catch (Exception e) { log.severe("Erro pagamento " + mpPaymentId + ": " + e.getMessage()); }
    }
}
