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

    @Value("${stgian.mp-seller-email:}")
    private String sellerEmail;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
        log.info("Mercado Pago configurado. Sandbox=" + accessToken.startsWith("TEST-"));
    }

    public PreferenceResult createPreference(Order order) {
        try {
            boolean isSandbox = accessToken.startsWith("TEST-");
            String chosenMethod = order.getPaymentMethod(); // "pix", "credit_card" ou "debit_card"
            log.info("Metodo de pagamento escolhido: " + chosenMethod);

            // ── Itens ─────────────────────────────────────────────────────
            List<PreferenceItemRequest> mpItems = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                BigDecimal unitPrice = new BigDecimal(item.getUnitPrice());
                log.info("Item: " + item.getProduct().getName()
                    + " | Preco: R$" + unitPrice + " | Qtd: " + item.getQuantity());

                mpItems.add(PreferenceItemRequest.builder()
                    .id(String.valueOf(item.getProduct().getId()))
                    .title(item.getProduct().getName() + " · Tam." + item.getSize())
                    .quantity(item.getQuantity())
                    .unitPrice(unitPrice)
                    .currencyId("BRL")
                    .build());
            }

            // ── Payer ─────────────────────────────────────────────────────
            String buyerEmail = order.getUser().getEmail();
            boolean isSelfPurchase = !sellerEmail.isBlank()
                && sellerEmail.equalsIgnoreCase(buyerEmail);

            // ── Configurar métodos de pagamento baseado na escolha ────────
            // A API do MP não tem "ir direto para PIX" — a solução é EXCLUIR
            // os outros métodos, deixando apenas o escolhido disponível.
            PreferencePaymentMethodsRequest paymentMethods = buildPaymentMethods(chosenMethod);

            // ── Builder base ──────────────────────────────────────────────
            PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                .items(mpItems)
                .paymentMethods(paymentMethods)
                .externalReference(order.getOrderCode())
                .statementDescriptor("STGIAN");

            // Adiciona payer só se não for o próprio vendedor
            if (!isSelfPurchase) {
                builder.payer(PreferencePayerRequest.builder()
                    .email(buyerEmail)
                    .build());
                log.info("Payer: " + buyerEmail);
            } else {
                log.warning("Auto-compra detectada — payer omitido.");
            }

            // ── Webhook ───────────────────────────────────────────────────
            boolean isLocalBackend = baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1");
            if (!isLocalBackend) {
                builder.notificationUrl(baseUrl + "/api/payments/webhook");
                log.info("Webhook ativo: " + baseUrl + "/api/payments/webhook");
            } else {
                log.warning("BASE_URL é localhost — webhook desativado. Use ngrok!");
            }

            // ── Back URLs ─────────────────────────────────────────────────
            boolean isLocalFrontend = frontendUrl.contains("localhost")
                || frontendUrl.contains("127.0.0.1")
                || frontendUrl.startsWith("file://");

            if (!isLocalFrontend) {
                builder.backUrls(PreferenceBackUrlsRequest.builder()
                    .success(frontendUrl + "?payment=success&order=" + order.getOrderCode())
                    .failure(frontendUrl + "?payment=failure&order=" + order.getOrderCode())
                    .pending(frontendUrl + "?payment=pending&order=" + order.getOrderCode())
                    .build())
                    .autoReturn("approved");
                log.info("BackUrls: " + frontendUrl);
            }

            // ── Criar preferência no MP ───────────────────────────────────
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(builder.build());

            String checkoutUrl = (isSandbox && preference.getSandboxInitPoint() != null)
                ? preference.getSandboxInitPoint()
                : preference.getInitPoint();

            log.info("Preferencia criada! ID=" + preference.getId()
                + " | Pedido=" + order.getOrderCode()
                + " | Metodo=" + chosenMethod
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

    /**
     * Constrói a configuração de métodos de pagamento baseado na escolha do cliente.
     *
     * A API do MP não permite redirecionar direto para um método — a solução
     * é excluir os outros, deixando apenas o escolhido disponível.
     *
     * Tipos de pagamento na API do MP:
     *   - "credit_card"  → cartão de crédito
     *   - "debit_card"   → cartão de débito
     *   - "bank_transfer" → PIX e transferências bancárias
     *   - "ticket"       → boleto bancário
     *   - "atm"          → pagamento em lotérica
     */
    private PreferencePaymentMethodsRequest buildPaymentMethods(String chosenMethod) {
        PreferencePaymentMethodsRequest.PreferencePaymentMethodsRequestBuilder builder =
            PreferencePaymentMethodsRequest.builder()
                .installments(12)
                .defaultInstallments(1);

        if (chosenMethod == null || chosenMethod.isBlank()) {
            // Nenhum método específico — aceita tudo
            log.info("Sem metodo especifico — todos disponiveis.");
            return builder.build();
        }

        List<PreferencePaymentTypeRequest> excluded = new ArrayList<>();

        switch (chosenMethod) {
            case "pix" -> {
                // PIX = bank_transfer → excluir cartões e boleto
                excluded.add(PreferencePaymentTypeRequest.builder().id("credit_card").build());
                excluded.add(PreferencePaymentTypeRequest.builder().id("debit_card").build());
                excluded.add(PreferencePaymentTypeRequest.builder().id("ticket").build());
                excluded.add(PreferencePaymentTypeRequest.builder().id("atm").build());
                // PIX não tem parcelamento
                builder.installments(1).defaultInstallments(1);
                log.info("Metodo: PIX — cartoes e boleto excluidos.");
            }
            case "credit_card" -> {
                // Crédito → excluir PIX, débito e boleto
                excluded.add(PreferencePaymentTypeRequest.builder().id("bank_transfer").build());
                excluded.add(PreferencePaymentTypeRequest.builder().id("debit_card").build());
                excluded.add(PreferencePaymentTypeRequest.builder().id("ticket").build());
                excluded.add(PreferencePaymentTypeRequest.builder().id("atm").build());
                log.info("Metodo: Cartao de Credito — PIX e debito excluidos.");
            }
            case "debit_card" -> {
                // Débito → excluir PIX, crédito e boleto
                excluded.add(PreferencePaymentTypeRequest.builder().id("bank_transfer").build());
                excluded.add(PreferencePaymentTypeRequest.builder().id("credit_card").build());
                excluded.add(PreferencePaymentTypeRequest.builder().id("ticket").build());
                excluded.add(PreferencePaymentTypeRequest.builder().id("atm").build());
                // Débito não tem parcelamento
                builder.installments(1).defaultInstallments(1);
                log.info("Metodo: Cartao de Debito — PIX e credito excluidos.");
            }
            default -> log.warning("Metodo desconhecido: " + chosenMethod + " — aceitando todos.");
        }

        if (!excluded.isEmpty()) {
            builder.excludedPaymentTypes(excluded);
        }

        return builder.build();
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
