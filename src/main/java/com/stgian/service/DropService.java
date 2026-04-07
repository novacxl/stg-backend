package com.stgian.service;

import com.stgian.dto.DropDTOs;
import com.stgian.model.Drop;
import com.stgian.repository.DropRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DropService {

    private final DropRepository dropRepository;

    public DropService(DropRepository dropRepository) {
        this.dropRepository = dropRepository;
    }

    public DropDTOs.DropResponse getActiveDrop() {
        return dropRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
            .map(DropDTOs.DropResponse::from)
            .orElse(null);
    }

    @Transactional
    public DropDTOs.DropResponse create(DropDTOs.DropRequest req) {
        // UPDATE em batch — uma única query ao invés de loop com N saves
        dropRepository.deactivateAll();

        Drop drop = new Drop();
        applyRequest(drop, req);
        drop.setActive(true);
        return DropDTOs.DropResponse.from(dropRepository.save(drop));
    }

    @Transactional
    public DropDTOs.DropResponse update(Long id, DropDTOs.DropRequest req) {
        Drop drop = dropRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Drop nao encontrado: " + id));
        applyRequest(drop, req);
        return DropDTOs.DropResponse.from(dropRepository.save(drop));
    }

    private void applyRequest(Drop drop, DropDTOs.DropRequest req) {
        drop.setTag(req.tag());
        drop.setTitle1(req.title1());
        drop.setTitle2(req.title2());
        drop.setDescription(req.description());
        drop.setBgNum(req.bgNum());
        drop.setLaunchDate(req.launchDate());
        drop.setMini1Name(req.mini1Name());
        drop.setMini1Price(req.mini1Price());
        drop.setMini1Desc(req.mini1Desc());
        drop.setMini1Tag(req.mini1Tag());
        drop.setMini2Name(req.mini2Name());
        drop.setMini2Price(req.mini2Price());
        drop.setMini2Desc(req.mini2Desc());
        drop.setMini2Tag(req.mini2Tag());
    }
}
