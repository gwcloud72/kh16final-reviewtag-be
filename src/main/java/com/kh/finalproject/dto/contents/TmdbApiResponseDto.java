package com.kh.finalproject.dto.contents;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/// *** 
///TMDB API 응답의 최상위 DTO (results 필드를 포함)
/// ***

@NoArgsConstructor @Data @AllArgsConstructor @Builder
public class TmdbApiResponseDto {
	private List<ContentsDto> results;
	private Integer totalPages;
}
