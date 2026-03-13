package com.kh.finalproject.vo.contents;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/// *** DB저장 VO *** ///

@Data @NoArgsConstructor @Builder @AllArgsConstructor
public class ContentsVO {
	private Long contentsId; //TBDB에서 제공하는 고유 id
	private String contentsTitle; //제목 // 영화는 title, TV는 name
	private String contentsType; //컨텐츠 유형 (movie / tv)
	private String contentsOverview; //영화/시리즈 개요
	private String contentsPosterPath; //포스터 이미지 경로
	private String contentsBackdropPath; //배경 이미지 경로
	private Double contentsVoteAverage; //평균평점
	private String contentsReleaseDate; // 개봉일/최초 방영일 // 영화는 release_date, TV는 first_air_date
	private String contentsLanguage = "ko" ; // 한국어
	private Integer contentsRuntime;
    private String contentsDirector; 
    private String contentsMainCast;
    
}
