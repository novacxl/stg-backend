package com.stgian.service;

import com.stgian.dto.AuthDTOs;
import com.stgian.dto.ProductDTOs;
import com.stgian.model.Product;
import com.stgian.model.User;
import com.stgian.repository.OrderRepository;
import com.stgian.repository.ProductRepository;
import com.stgian.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository    userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository   orderRepository;

    public UserService(UserRepository userRepository,
                       ProductRepository productRepository,
                       OrderRepository orderRepository) {
        this.userRepository    = userRepository;
        this.productRepository = productRepository;
        this.orderRepository   = orderRepository;
    }

    // FIX: removido u.getCpf() (campo não existe no model User)
    // FIX: countByUser e sumTotalByUser agora existem no OrderRepository
    public AuthDTOs.UserDTO getProfile(Long userId) {
        User u = getOrThrow(userId);
        int totalOrders = orderRepository.countByUser(u);
        Long totalSpent = orderRepository.sumTotalByUser(u);
        return new AuthDTOs.UserDTO(
            u.getId(), u.getName(), u.getEmail(),
            u.getRole().name(), u.getCreatedAt(),
            totalOrders, totalSpent != null ? totalSpent.intValue() : 0
        );
    }

    public List<ProductDTOs.ProductResponse> getWishlist(Long userId) {
        User u = getOrThrow(userId);
        return u.getWishlist().stream()
            .map(ProductDTOs.ProductResponse::from)
            .toList();
    }

    @Transactional
    public List<ProductDTOs.ProductResponse> addToWishlist(Long userId, Long productId) {
        User u = getOrThrow(userId);
        Product p = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Produto nao encontrado: " + productId));
        boolean alreadyIn = u.getWishlist().stream().anyMatch(x -> x.getId().equals(productId));
        if (!alreadyIn) {
            u.getWishlist().add(p);
            userRepository.save(u);
        }
        return getWishlist(userId);
    }

    @Transactional
    public List<ProductDTOs.ProductResponse> removeFromWishlist(Long userId, Long productId) {
        User u = getOrThrow(userId);
        u.getWishlist().removeIf(p -> p.getId().equals(productId));
        userRepository.save(u);
        return getWishlist(userId);
    }

    private User getOrThrow(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario nao encontrado: " + id));
    }
}
