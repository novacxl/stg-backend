package com.stgian.service;

import com.stgian.dto.OrderDTOs;
import com.stgian.model.*;
import com.stgian.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class OrderService {

    private static final Logger log = Logger.getLogger(OrderService.class.getName());

    @Value("${mercadopago.access-token}")
    private String mpAccessToken;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final PaymentService paymentService;

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

        // 1. Valida e monta itens usando @Lock PESSIMISTIC_WRITE
        // FIX-CRITICO-2: findByIdForUpdate usa SELECT FOR UPDATE — impede que dois
        // checkouts simultâneos leiam o mesmo estoque antes de qualquer um decrementar
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

        // 2. Salva pedido com status PENDING_PAYMENT
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

        // FIX-CRITICO-1: Cria preferência MP ANTES de decrementar o estoque
        // Se o MP retornar erro, a transação faz rollback e o estoque não é alterado
        PaymentService.PreferenceResult pref = paymentService.createPreference(saved);
        saved.setMpPreferenceId(pref.preferenceId());

        boolean isSandbox = mpAccessToken != null && mpAccessToken.startsWith("TEST-");
        String checkoutUrl = (isSandbox && pref.sandboxInitPoint() != null)
            ? pref.sandboxInitPoint()
            : pref.initPoint();

        saved.setMpCheckoutUrl(checkoutUrl);
        orderRepository.save(saved);

        // 3. Decrementa estoque SOMENTE após preferência MP criada com sucesso
        for (OrderItem item : saved.getItems()) {
            Product p = item.getProduct();
            p.setStock(p.getStock() - item.getQuantity());
            productRepository.save(p);
            stockService.registrarSaidaVenda(p, item.getQuantity(), saved.getOrderCode());
        }

        log.info("Pedido " + saved.getOrderCode() + " criado. Sandbox=" + isSandbox + " URL=" + checkoutUrl);
        return OrderDTOs.OrderResponse.from(saved);
    }

    @Transactional
    public void handlePaymentApproved(String orderCode) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            order.setPaymentStatus("APPROVED");
            order.setStatus(Order.Status.PROCESSING);
            orderRepository.save(order);
            log.info("Pagamento aprovado: " + orderCode);
        });
    }

    @Transactional
    public void handlePaymentRejected(String orderCode) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            order.setPaymentStatus("REJECTED");
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
                stockService.registrarDevolucao(p, item.getQuantity(), orderCode);
            }
            order.setStatus(Order.Status.CANCELLED);
            orderRepository.save(order);
            log.info("Pagamento rejeitado: " + orderCode + ". Estoque devolvido.");
        });
    }

    @Transactional
    public void handlePaymentPending(String orderCode, String mpPaymentId) {
        orderRepository.findByOrderCode(orderCode).ifPresent(order -> {
            order.setPaymentStatus("PENDING");
            order.setMpPaymentId(mpPaymentId);
            orderRepository.save(order);
            log.info("Pagamento pendente: " + orderCode);
        });
    }

    // FIX-MEDIO-2: Paginação — nunca mais carrega todos os pedidos em memória
    public List<OrderDTOs.OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findAll(pageable)
            .stream().map(OrderDTOs.OrderResponse::from).toList();
    }

    // Mantém compatibilidade para chamadas internas que precisam de todos
    public List<OrderDTOs.OrderResponse> getAllOrders() {
        return getAllOrders(0, 50);
    }

    public OrderDTOs.OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pedido nao encontrado: " + id));
        return OrderDTOs.OrderResponse.from(order);
    }

    public OrderDTOs.OrderResponse getByCode(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
            .orElseThrow(() -> new RuntimeException("Pedido nao encontrado: " + orderCode));
        return OrderDTOs.OrderResponse.from(order);
    }

    @Transactional
    public OrderDTOs.OrderResponse updateStatus(Long orderId, OrderDTOs.StatusUpdateRequest req) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Pedido nao encontrado: " + orderId));

        Order.Status newStatus = Order.Status.valueOf(req.status().toUpperCase());

        if (newStatus == Order.Status.CANCELLED && order.getStatus() != Order.Status.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product p = item.getProduct();
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
                stockService.registrarDevolucao(p, item.getQuantity(), order.getOrderCode());
            }
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
