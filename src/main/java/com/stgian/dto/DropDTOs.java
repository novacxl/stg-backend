package com.stgian.dto;

import com.stgian.model.Drop;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class DropDTOs {

    public record DropRequest(
        @NotBlank String tag,
        @NotBlank String title1,
        String title2,
        String description,
        String bgNum,
        LocalDateTime launchDate,
        String mini1Name,
        Integer mini1Price,
        String mini1Desc,
        String mini1Tag,
        String mini2Name,
        Integer mini2Price,
        String mini2Desc,
        String mini2Tag
    ) {}

    public record DropResponse(
        Long id,
        Boolean active,
        String tag,
        String title1,
        String title2,
        String description,
        String bgNum,
        LocalDateTime launchDate,
        String mini1Name,
        Integer mini1Price,
        String mini1Desc,
        String mini1Tag,
        String mini2Name,
        Integer mini2Price,
        String mini2Desc,
        String mini2Tag,
        LocalDateTime updatedAt
    ) {
        public static DropResponse from(Drop d) {
            return new DropResponse(
                d.getId(),
                d.getActive(),
                d.getTag(),
                d.getTitle1(),
                d.getTitle2(),
                d.getDescription(),
                d.getBgNum(),
                d.getLaunchDate(),
                d.getMini1Name(),
                d.getMini1Price(),
                d.getMini1Desc(),
                d.getMini1Tag(),
                d.getMini2Name(),
                d.getMini2Price(),
                d.getMini2Desc(),
                d.getMini2Tag(),
                d.getUpdatedAt()
            );
        }
    }
}
