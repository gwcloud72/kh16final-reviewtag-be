package com.kh.finalproject.vo;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PointHistoryVo {
    // PointHistoryDto의 기본 필드들
    private int pointHistoryId;
    private String pointHistoryMemberId;
    private int pointHistoryAmount;
    private String pointHistoryTrxType;
    private Timestamp pointHistoryCreatedAt;

    // 테이블에는 없지만 결과로 보여줄 필드
    private String content; 
}