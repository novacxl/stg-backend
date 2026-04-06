package com.stgian.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class AuthDTOs {

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 1, max = 128) String password
    ) {}

    public record RegisterRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 128) String password
    ) {}

    public record AuthResponse(String token, UserDTO user) {}

    public record UserDTO(
        Long id,
        String name,
        String email,
        String role,
        LocalDate createdAt,
        int totalOrders,
        int totalSpent
    ) {}
}
