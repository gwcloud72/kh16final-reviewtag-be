package com.kh.finalproject.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 찜/해제 시 입력 파라미터를 담는 VO
@JsonIgnoreProperties(ignoreUnknown = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PointItemWishVO {
    private String memberId;
    private long itemNo;
}