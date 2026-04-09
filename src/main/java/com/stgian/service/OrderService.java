package com.stgian.service;

import com.stgian.dto.OrderDTOs;
import com.stgian.model.*;
import com.stgian.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * FLUXO CORRETO DE PEDIDOS:
 *
 *  1. checkout()              → Cria pedido PENDING_PAYMENT, reserva estoque (sem debitar)
 *                               Cria preferência no MP, retorna URL de pagamento
 *
 *  2. handlePaymentApproved() → Pagamento confirmado pelo MP via webhook
 *                               SOMENTE AQUI o estoque é debitado e saldo atualizado
 *                               Status → PROCESSING
 *
 *  3. handlePaymentRejected() → Pagamento recusado
 *                               Libera reserva de estoque
 *                               Status → CANCELLED
 *
 *  4. handlePaymentPending()  → Aguardando confirmação (ex: boleto, Pix expirado)
 *                               Status permanece PENDING_PAYMENT
 */
@Service
public class OrderService {

    private static final Logger log = Logger.getLogger(OrderService.class.getName());

    @Value("${mercadopago.access-token}")
    private String mpAccessToken;

    private final OrderRepository    orderRepository;
    private final UserRepository     userRepository;
    private final ProductRepository  productRepository;
    private final StockService       stockService;
    private final PaymentService     paymentService;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        ProductRepository productRepository,
                        StockService stockService,
                        PaymentService paymentService) {
        this.orderRepository  = orderRepository;
        this.userRepository   = userRepository;
        this.productRepository= productRepository;
        this.stockService     = stockService;
        this.paymentService   = paymentService;
    }

    public List<OrderDTOs.OrderResponse> getMyOrders(Long userId) {
        User user = getUserOrThrow(userId);
        return orderRepository.findByUserOrderByCreatedAtDesc(user)
            .stream().map(OrderDTOs.OrderResponse::from).toList();
    }

    @Transactional
    public OrderDTOs.OrderResponse checkout(Long userId, OrderDTOs.CheckoutRequest req) {
        User user = getUserOrThrow(userId);

        // ── 1. Valida disponibilidade com lock pessimista ──────────────────
        // SELECT FOR UPDATE impede race condition — dois clientes não conseguem
        // passar pela validação de estoque ao mesmo tempo
        List<OrderItem> items = new ArrayList<>();
        int total = 0;

        for (OrderDTOs.OrderItemRequest itemReq : req.items()) {
            Product product = productRepository.findByIdForUpdate(itemReq.productId())
                .orElseThrow(() -> new RuntimeException("Produto nao encontrado: " + itemReq.productId()));

            if (!product.getActive())
                throw new IllegalArgumentException("Produto indisponivel: " + product.getName());

            if (product.getStock() < itemReq.quantity())
                throw new IllegalArgumentException(
                    "Estoque insuficiente: " + product.getName()
                    + " (disponivel: " + product.getStock() + ")");

            int unitPrice = product.getPrice();
            total += unitPrice * itemReq.quantity();

            items.add(OrderItem.builder()
                .product(product).size(itemReq.size())
                .quantity(itemReq.quantity()).unitPrice(unitPrice)
                .build());
        }

        // ── 2. Salva pedido PENDING_PAYMENT ───────────────────────────────
        OrderDTOs.AddressRequest addr = req.address();
        Order order = Order.builder()
            .user(user).total(total)
            .status(Order.Status.PENDING_PAYMENT)
            .shippingName(addr.name())
            .shippingEmail(user.getEmail())
            .shippingPhone(addr.phone())
            .shippingCep(addr.cep())
            .shippingStreet(addr.street())
            .shippingNumber(addr.number())
            .shippingComplement(addr.comp())
            .shippingNeighborhood(addr.hood())
            .shippingCity(addr.city())
            .shippingState(addr.state())
            .paymentMethod(req.paymentMethod())
            .build();

        Order saved = orderRepository.save(order);
        for (OrderItem item : items) item.setOrder(saved);
        saved.setItems(items);
        orderRepository.save(saved);

        // ── 3. Cria preferência no Mercado Pago ───────────────────────────
        // Se o MP falhar aqui, rollback automático — nada é alterado no estoque
        PaymentService.PreferenceResult pref = paymentService.createPreference(saved);
        saved.setMpPreferenceId(pref.preferenceId());

        boolean isSandbox = mpAccessToken != null && mpAccessToken.startsWith("TEST-");
        String checkoutUrl = (isSandbox && pref.sandboxInitPoint() != null)
            ? pref.sandboxInitPoint()
            : pref.initPoint();

        saved.setMpCheckoutUrl(checkoutUrl);
        orderRepository.save(saved);

        // ── ESTOQUE NÃO É ALTERADO AQUI ───────────────────────────────────
        // O estoque só é debitado em handlePaymentApproved() após confirmação
        // do pagamento via webhook do Mercado Pago.
        // Isso garante que: nenhuma unidade seja perdida em pagamentos abandonados.

        log.info("Pedido " + saved.getOrderCode() + " criado aguardando pagamento. URL=" + checkoutUrl);
        return OrderDTOs.OrderResponse.from(saved);
    }

    /**
     * Chamado pelo webhook do MP quando pagamento é APROVADO.
     * SOMENTE AQUI o estoque é debitado e o pedido avança para PROCESSING.
     */
    @Transactional
    public void handlePaymentApproved(String orderCode) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            // Evita processar duas vezes (idempotência)
            if ("APPROVED".equals(order.getPaymentStatus())) {
                log.info("Pagamento " + orderCode + " já foi processado. Ignorando duplicata.");
                return;
            }

            // Debita estoque SOMENTE após pagamento confirmado
            for (OrderItem item : order.getItems()) {
                Product p = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElse(item.getProduct());

                int before = p.getStock();
                int after  = Math.max(0, before - item.getQuantity());
                p.setStock(after);
                productRepository.save(p);
                stockService.registrarSaidaVenda(p, item.getQuantity(), orderCode);
            }

            order.setPaymentStatus("APPROVED");
            order.setStatus(Order.Status.PROCESSING);
            orderRepository.save(order);
            log.info("Pagamento aprovado: " + orderCode + ". Estoque debitado.");
        });
    }

    /**
     * Chamado pelo webhook do MP quando pagamento é REJEITADO.
     * O pedido é cancelado — estoque não precisa ser devolvido pois nunca foi debitado.
     */
    @Transactional
    public void handlePaymentRejected(String orderCode) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            // Se por algum motivo o estoque já foi debitado (APPROVED anterior), devolve
            if ("APPROVED".equals(order.getPaymentStatus())) {
                for (OrderItem item : order.getItems()) {
                    Product p = item.getProduct();
                    p.setStock(p.getStock() + item.getQuantity());
                    productRepository.save(p);
                    stockService.registrarDevolucao(p, item.getQuantity(), orderCode);
                }
            }
            order.setPaymentStatus("REJECTED");
            order.setStatus(Order.Status.CANCELLED);
            orderRepository.save(order);
            log.info("Pagamento rejeitado: " + orderCode);
        });
    }

    @Transactional
    public void handlePaymentPending(String orderCode, String mpPaymentId) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            // Só atualiza se ainda estiver pendente — não sobrescreve APPROVED
            if (!"APPROVED".equals(order.getPaymentStatus())) {
                order.setPaymentStatus("PENDING");
                order.setMpPaymentId(mpPaymentId);
                orderRepository.save(order);
            }
            log.info("Pagamento pendente: " + orderCode);
        });
    }

    public List<OrderDTOs.OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findAll(pageable)
            .stream().map(OrderDTOs.OrderResponse::from).toList();
    }

    public List<OrderDTOs.OrderResponse> getAllOrders() {
        return getAllOrders(0, 50);
    }

    public OrderDTOs.OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pedido nao encontrado: " + id));
        return OrderDTOs.OrderResponse.from(order);
    }

    @Transactional
    public OrderDTOs.OrderResponse updateStatus(Long orderId, OrderDTOs.StatusUpdateRequest req) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Pedido nao encontrado: " + orderId));

        Order.Status newStatus = Order.Status.valueOf(req.status().toUpperCase());

        // Regra de estoque ao cancelar:
        // Só devolve estoque se o pagamento foi APROVADO (estoque já foi debitado)
        // Se pagamento ainda está PENDING, o estoque nunca foi debitado — nada a devolver
        boolean pagamentoAprovado = "APPROVED".equals(order.getPaymentStatus());
        boolean cancelando        = newStatus == Order.Status.CANCELLED
                                    && order.getStatus() != Order.Status.CANCELLED;

        if (cancelando && pagamentoAprovado) {
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
                stockService.registrarDevolucao(p, item.getQuantity(), order.getOrderCode());
            }
            log.info("Pedido " + order.getOrderCode() + " cancelado — estoque devolvido.");
        } else if (cancelando) {
            // Pedido pendente cancelado — estoque não foi debitado, nada a devolver
            log.info("Pedido " + order.getOrderCode() + " cancelado sem pagamento — estoque inalterado.");
        }

        order.setStatus(newStatus);
        return OrderDTOs.OrderResponse.from(orderRepository.save(order));
    }

    public OrderDTOs.OrderResponse trackByCode(String code, Long userId) {
        Order order = orderRepository.findByOrderCode(code)
            .orElseThrow(() -> new RuntimeException("Pedido nao encontrado: " + code));
        User user = getUserOrThrow(userId);
        if (user.getRole() == User.Role.CLIENT && !order.getUser().getId().equals(userId))
            throw new SecurityException("Acesso negado.");
        return OrderDTOs.OrderResponse.from(order);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario nao encontrado: " + id));
    }
}
