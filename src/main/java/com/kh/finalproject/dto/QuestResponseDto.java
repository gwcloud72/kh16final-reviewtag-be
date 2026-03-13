package com.kh.finalproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestResponseDto {
    private String type;         // "REVIEW", "QUIZ" (영어)
    private String title;        // "Write a Review" (영어)
    private int currentCount;    // 현재 수행 횟수
    private int targetCount;     // 목표 횟수
    private int reward;          // 보상 포인트
    private boolean isDone;      // 달성 여부 (current >= target)
    private boolean isClaimed;   // 보상 수령 여부
}