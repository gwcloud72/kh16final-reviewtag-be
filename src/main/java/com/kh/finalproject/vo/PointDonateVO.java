package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class PointDonateVO {
    private String targetId;
    private int amount;
}