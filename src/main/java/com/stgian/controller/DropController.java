package com.stgian.controller;

import com.stgian.dto.DropDTOs;
import com.stgian.service.DropService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/drops")
public class DropController {

    private final DropService dropService;

    public DropController(DropService dropService) {
        this.dropService = dropService;
    }

    // Público — retorna o drop ativo
    @GetMapping("/active")
    public ResponseEntity<DropDTOs.DropResponse> getActive() {
        DropDTOs.DropResponse drop = dropService.getActiveDrop();
        if (drop == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(drop);
    }

    // OWNER — cria novo drop (desativa os anteriores)
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<DropDTOs.DropResponse> create(
            @Valid @RequestBody DropDTOs.DropRequest req) {
        return ResponseEntity.status(201).body(dropService.create(req));
    }

    // ADMIN ou OWNER — atualiza drop existente
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public ResponseEntity<DropDTOs.DropResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DropDTOs.DropRequest req) {
        return ResponseEntity.ok(dropService.update(id, req));
    }
}
