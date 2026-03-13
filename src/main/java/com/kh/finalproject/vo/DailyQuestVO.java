package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DailyQuestVO {
    private String type;     // 퀘스트 타입 (ROULETTE, REVIEW 등)
    private String title;    // 화면에 보여줄 제목
    private int current;     // 현재 횟수
    private int target;      // 목표 횟수
    private int reward;      // 보상 포인트
    private boolean done;    // 목표 달성 여부
    private boolean claimed; // 보상 수령 여부
    
    // 프론트 아이콘/액션 매핑용 (Service에서 처리하거나 프론트에서 처리)
    private String icon;     
    private String desc;
    private String action;
}