package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//최근 가장 많이 리뷰가 달린 컨텐츠 랭킹
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ContentsRankingVO {
    private int contentsId;
    private String contentsTitle;
    private String contentsPosterPath;
    private int reviewCount;
}
