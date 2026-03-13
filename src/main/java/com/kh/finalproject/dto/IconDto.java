package com.kh.finalproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class IconDto {
    private int iconId;
    private String iconName;
    private String iconRarity; // COMMON, RARE, EPIC, LEGENDARY
    private String iconCategory;
    private String iconSrc;
    private Long iconContents;
}