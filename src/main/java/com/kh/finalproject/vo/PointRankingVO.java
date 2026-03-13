package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PointRankingVO {
    private String memberId;
    private String nickname;
    private int point;
    private String level;
    private int ranking; // 반드시 'rank'가 아닌 'ranking'이어야 에러가 안 납니다.
}