package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//이달의 퀴즈왕, 이달의 리뷰왕
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserRankingVO {
    private String memberId;
    private String memberNickname;
    private int count; // 정답 수 or 좋아요 수
    private int reviewCount;//리뷰왕 전용
    private int rank;  // 순위
}
