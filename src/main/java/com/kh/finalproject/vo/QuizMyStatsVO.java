package com.kh.finalproject.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizMyStatsVO {
	
	private int totalSolved;   // 푼 문제 수
    private int myScore;       // 내 점수 (정답 수 * 20)
    private int myRank;        // 내 등수
    private int totalUsers;    // 전체 참여자 수 (상위 % 계산용)
    
    private double accuracy;   // 정답률
}
