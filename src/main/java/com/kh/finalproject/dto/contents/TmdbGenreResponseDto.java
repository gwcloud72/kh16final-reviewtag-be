package com.kh.finalproject.dto.contents;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/// *** TMDB 장르 API 응답의 최상위 DTO (genres 필드를 포함) ***///

@NoArgsConstructor @Data @AllArgsConstructor @Builder
public class TmdbGenreResponseDto {
	private List<ContentsGenreDto> genres;
}
