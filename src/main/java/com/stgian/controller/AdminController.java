package com.stgian.controller;

import com.stgian.dto.AdminDTOs;
import com.stgian.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
@PreAuthorize("hasRole('OWNER')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/team")
    public ResponseEntity<List<AdminDTOs.AdminResponse>> listTeam() {
        return ResponseEntity.ok(adminService.listTeam());
    }

    @GetMapping
    public ResponseEntity<List<AdminDTOs.AdminResponse>> listAdmins() {
        return ResponseEntity.ok(adminService.listAdmins());
    }

    @PostMapping
    public ResponseEntity<AdminDTOs.AdminResponse> create(
            @Valid @RequestBody AdminDTOs.CreateAdminRequest req) {
        return ResponseEntity.status(201).body(adminService.createAdmin(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        adminService.removeAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
