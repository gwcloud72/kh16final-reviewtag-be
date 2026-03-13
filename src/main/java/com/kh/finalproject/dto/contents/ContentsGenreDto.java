package com.kh.finalproject.dto.contents;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/// *** 장르 목록 API 응답 Dto *** /// 

@Data @NoArgsConstructor @Builder @AllArgsConstructor
public class ContentsGenreDto {
	@JsonAlias({"id"})
	private Long genreId;
	
	@JsonAlias({"name"})
	private String genreName;
}
