package com.stgian.dto;

import com.stgian.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class AdminDTOs {

    public record CreateAdminRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password
    ) {}

    public record AdminResponse(
        Long id,
        String name,
        String email,
        String role,
        LocalDate createdAt
    ) {
        public static AdminResponse from(User u) {
            return new AdminResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole().name(),
                u.getCreatedAt()
            );
        }
    }
}
