package com.stgian.service;

import com.stgian.dto.AdminDTOs;
import com.stgian.model.User;
import com.stgian.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminDTOs.AdminResponse> listAdmins() {
        return userRepository.findByRole(User.Role.ADMIN)
            .stream().map(AdminDTOs.AdminResponse::from).toList();
    }

    public List<AdminDTOs.AdminResponse> listTeam() {
        List<User> owners = userRepository.findByRole(User.Role.OWNER);
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        return Stream.concat(owners.stream(), admins.stream())
            .map(AdminDTOs.AdminResponse::from)
            .toList();
    }

    public AdminDTOs.AdminResponse createAdmin(AdminDTOs.CreateAdminRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            // FIX-MEDIO-3: Mensagem genérica — não confirma ao atacante que o email existe
            throw new IllegalArgumentException("Nao foi possivel criar o administrador com esses dados.");
        }

        User admin = User.builder()
            .name(req.name())
            .email(req.email())
            .password(passwordEncoder.encode(req.password()))
            .role(User.Role.ADMIN)
            .build();

        return AdminDTOs.AdminResponse.from(userRepository.save(admin));
    }

    public void removeAdmin(Long adminId) {
        User user = userRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Usuario nao encontrado: " + adminId));

        if (user.getRole() == User.Role.OWNER)
            throw new IllegalArgumentException("Nao e possivel remover o dono.");

        if (user.getRole() != User.Role.ADMIN)
            throw new IllegalArgumentException("Usuario nao e um administrador.");

        user.setRole(User.Role.CLIENT);
        userRepository.save(user);
    }
}
