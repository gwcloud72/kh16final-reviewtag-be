package com.kh.finalproject.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown=true)
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class PointGiftVO {
    private long itemNo;
    private String targetId; // ★ 중요: 소문자로 시작해야 함
}	