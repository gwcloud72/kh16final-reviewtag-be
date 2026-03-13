package com.kh.finalproject.vo.contents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/// *** 장르 매핑 정보 저장 VO *** ///
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ContentsGenreMapVO {
	private Long contentsId;
	private Integer genreId;
}
