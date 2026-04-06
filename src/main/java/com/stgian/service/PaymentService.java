package com.stgian.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.*;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.resources.preference.Preference;
import com.stgian.model.Order;
import com.stgian.model.OrderItem;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class PaymentService {

    private static final Logger log = Logger.getLogger(PaymentService.class.getName());

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${stgian.base-url}")
    private String baseUrl;

    @Value("${stgian.frontend-url}")
    private String frontendUrl;

    // E-mail da sua conta MP vendedora — preencha no application.properties
    @Value("${stgian.mp-seller-email:}")
    private String sellerEmail;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
        boolean isSandbox = accessToken.startsWith("TEST-");
        log.info("Mercado Pago configurado. Sandbox=" + isSandbox);
    }

    public PreferenceResult createPreference(Order order) {
        try {
            boolean isSandbox = accessToken.startsWith("TEST-");

            // ── Itens ─────────────────────────────────────────────────────
            List<PreferenceItemRequest> mpItems = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                BigDecimal unitPrice = new BigDecimal(item.getUnitPrice());
                log.info("Item: " + item.getProduct().getName()
                    + " | Preco: R$" + unitPrice
                    + " | Qtd: " + item.getQuantity());

                mpItems.add(PreferenceItemRequest.builder()
                    .id(String.valueOf(item.getProduct().getId()))
                    .title(item.getProduct().getName() + " · Tam." + item.getSize())
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .currencyId("BRL")
                    .build());
            }

            // ── Payer ─────────────────────────────────────────────────────
            // Só envia o payer se o e-mail do comprador for DIFERENTE
            // do e-mail do vendedor — MP bloqueia auto-pagamento em produção
            String buyerEmail = order.getShippingEmail() != null
                ? order.getShippingEmail()
                : order.getUser().getEmail();

            PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                .items(mpItems)
                .externalReference(order.getOrderCode())
                .statementDescriptor("STGIAN");

            // Só adiciona payer se não for o próprio vendedor
            boolean isSelfPurchase = !sellerEmail.isBlank()
                && sellerEmail.equalsIgnoreCase(buyerEmail);

            if (!isSelfPurchase) {
                builder.payer(PreferencePayerRequest.builder()
                    .email(buyerEmail)
                    .build());
                log.info("Payer definido: " + buyerEmail);
            } else {
                log.warning("Comprador é o vendedor — payer omitido para evitar bloqueio do MP.");
            }

            // ── Métodos de pagamento ──────────────────────────────────────
            // Sem exclusões = aceita PIX, crédito, débito e boleto
            // installments só se aplica a crédito
            builder.paymentMethods(
                PreferencePaymentMethodsRequest.builder()
                    .installments(12)
                    .defaultInstallments(1)
                    .build()
            );

            // ── URLs de retorno ───────────────────────────────────────────
            boolean isLocalFrontend = frontendUrl.contains("localhost")
                || frontendUrl.contains("127.0.0.1")
                || frontendUrl.startsWith("file://");

            boolean isLocalBackend = baseUrl.contains("localhost")
                || baseUrl.contains("127.0.0.1");

            if (!isLocalFrontend) {
                // Tenta identificar o nome do arquivo HTML para o retorno correto
                String htmlFile = "index.html";
                builder.backUrls(PreferenceBackUrlsRequest.builder()
                    .success(frontendUrl + "?payment=success&order=" + order.getOrderCode())
                    .failure(frontendUrl + "?payment=failure&order=" + order.getOrderCode())
                    .pending(frontendUrl + "?payment=pending&order=" + order.getOrderCode())
                    .build())
                    .autoReturn("approved");
                log.info("BackUrls configuradas: " + frontendUrl);
            } else {
                log.info("Frontend local — backUrls omitidas.");
            }

            if (!isLocalBackend) {
                builder.notificationUrl(baseUrl + "/api/payments/webhook");
            } else {
                log.info("Backend local — webhook omitido.");
            }

            // ── Criar preferência ─────────────────────────────────────────
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(builder.build());

            // Sandbox usa sandboxInitPoint; produção usa initPoint
            String checkoutUrl = (isSandbox && preference.getSandboxInitPoint() != null)
                ? preference.getSandboxInitPoint()
                : preference.getInitPoint();

            log.info("Preferencia MP criada! ID=" + preference.getId()
                + " | Pedido=" + order.getOrderCode()
                + " | Sandbox=" + isSandbox
                + " | URL=" + checkoutUrl);

            return new PreferenceResult(
                preference.getId(),
                preference.getInitPoint(),
                preference.getSandboxInitPoint()
            );

        } catch (MPApiException e) {
            String detail = "";
            try { detail = e.getApiResponse().getContent(); } catch (Exception ignored) {}
            log.severe("Erro MP API | Status: " + e.getStatusCode() + " | " + detail);
            throw new RuntimeException("Erro ao iniciar pagamento: " + detail);
        } catch (Exception e) {
            log.severe("Erro inesperado: " + e.getMessage());
            throw new RuntimeException("Erro ao iniciar pagamento: " + e.getMessage());
        }
    }

    public String resolvePaymentStatus(String mpStatus) {
        return switch (mpStatus) {
            case "approved"   -> "APPROVED";
            case "pending"    -> "PENDING";
            case "in_process" -> "IN_PROCESS";
            case "rejected"   -> "REJECTED";
            case "cancelled"  -> "CANCELLED";
            case "refunded"   -> "REFUNDED";
            default           -> "UNKNOWN";
        };
    }

    public record PreferenceResult(String preferenceId, String initPoint, String sandboxInitPoint) {}
}
