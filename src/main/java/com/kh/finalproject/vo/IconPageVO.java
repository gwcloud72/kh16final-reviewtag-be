package com.kh.finalproject.vo;

import java.util.List;

import com.kh.finalproject.dto.IconDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IconPageVO {
    private List<IconDto> list; // 아이콘 목록
    private int totalPage;      // 전체 페이지 수
    private int currentPage;    // 현재 페이지
    private int totalCount;     // 전체 개수
}