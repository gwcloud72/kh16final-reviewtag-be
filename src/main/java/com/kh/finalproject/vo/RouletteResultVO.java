package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouletteResultVO {
    private int prizePoint; // 당첨 포인트
    private String message; // 결과 메시지
    private int index;      // 룰렛 위치 인덱스 (0~5)
}