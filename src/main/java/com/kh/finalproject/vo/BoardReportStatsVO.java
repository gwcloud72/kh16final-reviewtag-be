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
public class BoardReportStatsVO {
    private int boardNo;                 // 신고된 게시글 번호
    private String writerNickname;     // 신고된 게시글 작성자 닉네임
    private int boardReportId;
    private int totalReportCount;       // 총 신고 수
    
    // 유형별 카운트
    private int countInapposite;             // 부적절한 컨텐츠
    private int countHate;            // 욕설 / 비하
    private int countSpam;              // 스팸
    private int countEtc;               // 기타
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastReportedAt;   // 마지막 신고 날짜
}