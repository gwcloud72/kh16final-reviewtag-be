package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PointUseVO {
    private long inventoryNo;   // 사용할 아이템의 인벤토리 번호 (PK)
    private String extraValue;  // (옵션) 닉네임 변경권 등에 쓰일 입력값
}