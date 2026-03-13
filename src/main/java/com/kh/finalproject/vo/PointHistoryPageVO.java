package com.kh.finalproject.vo;

import java.util.List;
import com.kh.finalproject.dto.PointHistoryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PointHistoryPageVO {
    private List<PointHistoryDto> list; // 현재 페이지 데이터 목록
    private int totalPage;              // 전체 페이지 수
    private int currentPage;            // 현재 페이지 번호
    private int totalCount;             // 전체 게시글 개수
}