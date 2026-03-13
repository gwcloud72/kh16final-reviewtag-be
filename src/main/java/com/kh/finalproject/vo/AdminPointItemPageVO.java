package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor @Builder

public class AdminPointItemPageVO {
    private String itemType; // POINT_ITEM_TYPE 필터
    private String keyword;  // 상품명 검색용 (선택)
    private int page = 1;    // 현재 페이지 번호
    private int size = 10;   // 한 페이지에 보여줄 개수

    // MyBatis에서 사용할 시작/끝 번호 계산
    public int getBegin() {
        return (page - 1) * size + 1;
    }
    public int getEnd() {
        return page * size;
    }
}