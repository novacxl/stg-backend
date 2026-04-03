package com.stgian.service;

import com.stgian.dto.StockDTOs;
import com.stgian.model.Product;
import com.stgian.model.StockMovement;
import com.stgian.model.User;
import com.stgian.repository.ProductRepository;
import com.stgian.repository.StockMovementRepository;
import com.stgian.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockService {

    private static final int LOW_STOCK_THRESHOLD = 3;

    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public StockService(StockMovementRepository movementRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.movementRepository = movementRepository;
        this.productRepository  = productRepository;
        this.userRepository     = userRepository;
    }

    public List<StockDTOs.MovementResponse> listAll() {
        return movementRepository.findAllByOrderByCreatedAtDesc()
            .stream().map(StockDTOs.MovementResponse::from).toList();
    }

    public List<StockDTOs.MovementResponse> listByProduct(Long productId) {
        Product p = getProductOrThrow(productId);
        return movementRepository.findByProductOrderByCreatedAtDesc(p)
            .stream().map(StockDTOs.MovementResponse::from).toList();
    }

    public List<StockDTOs.MovementResponse> listRecent(int limit) {
        return movementRepository.findRecent(PageRequest.of(0, limit))
            .stream().map(StockDTOs.MovementResponse::from).toList();
    }

    // FIX-MEDIO-1: N+1 corrigido — busca totais de todos os produtos em 2 queries
    // em vez de 2 queries por produto (N*2 queries → sempre 2 queries fixas)
    public List<StockDTOs.StockSummary> getSummary() {
        List<Product> products = productRepository.findByActiveTrue();

        // 1 query para todas as entradas agrupadas por produto
        Map<Long, Integer> entradas = movementRepository.totalEntradasGrouped()
            .stream().collect(Collectors.toMap(
                r -> (Long) r[0],
                r -> ((Number) r[1]).intValue()
            ));

        // 1 query para todas as saídas agrupadas por produto
        Map<Long, Integer> saidas = movementRepository.totalSaidasGrouped()
            .stream().collect(Collectors.toMap(
                r -> (Long) r[0],
                r -> ((Number) r[1]).intValue()
            ));

        return products.stream().map(p -> new StockDTOs.StockSummary(
            p.getId(), p.getName(), p.getCategory().name(),
            p.getStock(),
            entradas.getOrDefault(p.getId(), 0),
            saidas.getOrDefault(p.getId(), 0),
            p.getStock() <= LOW_STOCK_THRESHOLD
        )).toList();
    }

    @Transactional
    public StockDTOs.MovementResponse registerEntry(Long userId, StockDTOs.StockEntryRequest req) {
        User user  = getUserOrThrow(userId);
        Product p  = getProductOrThrow(req.productId());
        int before = p.getStock();
        int after  = before + req.quantity();
        p.setStock(after);
        productRepository.save(p);
        return save(p, StockMovement.Type.ENTRADA, req.quantity(), before, after, req.reason(), user, null);
    }

    @Transactional
    public StockDTOs.MovementResponse registerExit(Long userId, StockDTOs.StockExitRequest req) {
        User user  = getUserOrThrow(userId);
        Product p  = getProductOrThrow(req.productId());
        int before = p.getStock();
        if (before < req.quantity())
            throw new IllegalArgumentException(
                "Estoque insuficiente. Disponível: " + before + ", solicitado: " + req.quantity());
        int after = before - req.quantity();
        p.setStock(after);
        productRepository.save(p);
        return save(p, StockMovement.Type.SAIDA, req.quantity(), before, after, req.reason(), user, null);
    }

    @Transactional
    public void registrarSaidaVenda(Product product, int quantity, String orderCode) {
        int after  = product.getStock();
        int before = after + quantity;
        save(product, StockMovement.Type.VENDA, quantity, before, after,
             "Venda automática — pedido " + orderCode, null, orderCode);
    }

    @Transactional
    public void registrarDevolucao(Product product, int quantity, String orderCode) {
        int before = product.getStock() - quantity;
        int after  = product.getStock();
        save(product, StockMovement.Type.ENTRADA, quantity, before, after,
             "Devolução — pedido cancelado " + orderCode, null, orderCode);
    }

    private StockDTOs.MovementResponse save(Product product, StockMovement.Type type,
                                             int quantity, int before, int after,
                                             String reason, User registeredBy, String orderCode) {
        StockMovement m = StockMovement.builder()
            .product(product).type(type).quantity(quantity)
            .stockBefore(before).stockAfter(after).reason(reason)
            .registeredBy(registeredBy).orderCode(orderCode).build();
        return StockDTOs.MovementResponse.from(movementRepository.save(m));
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + id));
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + id));
    }
}
