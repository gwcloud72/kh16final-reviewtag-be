package com.kh.finalproject.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizReportStatsVO {
    private int quizId;                 // 신고된 퀴즈 아이디
    private String quizQuestion;        // 신고된 퀴즈 질문
    private String quizCreatorId;       // 신고된 퀴즈 작성자 아이디
    private String creatorNickname;     // 신고된 퀴즈 작성자 닉네임
    private String quizStatus;          // 퀴즈 신고 종류
    
    private int totalReportCount;       // 총 신고 수
    
    // 유형별 카운트
    private int countError;             // 문제 오류
    private int countAbusive;            // 욕설 / 비하
    private int countSpam;              // 스팸
    private int countEtc;               // 기타
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastReportedAt;   // 마지막 신고 날짜
}