package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyQuizVO {
    private int quizNo;          // 퀴즈 번호
    private String quizQuestion; // 문제 내용
    private String quizAnswer;   // 정답 (사용자에게는 안 보여주고 검증용으로 씀)
}