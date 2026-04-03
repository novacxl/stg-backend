package com.stgian.controller;

import com.stgian.dto.AuthDTOs;
import com.stgian.dto.ProductDTOs;
import com.stgian.security.JwtUtil;
import com.stgian.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthDTOs.UserDTO> me(@RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(userService.getProfile(extractUserId(bearer)));
    }

    @GetMapping("/me/wishlist")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductDTOs.ProductResponse>> getWishlist(
            @RequestHeader("Authorization") String bearer) {
        return ResponseEntity.ok(userService.getWishlist(extractUserId(bearer)));
    }

    @PostMapping("/me/wishlist/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductDTOs.ProductResponse>> addToWishlist(
            @RequestHeader("Authorization") String bearer,
            @PathVariable Long productId) {
        return ResponseEntity.ok(userService.addToWishlist(extractUserId(bearer), productId));
    }

    @DeleteMapping("/me/wishlist/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductDTOs.ProductResponse>> removeFromWishlist(
            @RequestHeader("Authorization") String bearer,
            @PathVariable Long productId) {
        return ResponseEntity.ok(userService.removeFromWishlist(extractUserId(bearer), productId));
    }

    private Long extractUserId(String bearer) {
        return jwtUtil.extractUserId(bearer.substring(7));
    }
}
