package com.kh.finalproject.dto.contents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GenreDto {
	private Integer genreId;
	private String genreName;
}
