package com.kh.finalproject.dto.contents;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


///*** TMDB API 응답 DTO ****///

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ContentsDto {
	@JsonAlias({"id"})
	private Long contentsId; //TMDB에서 제공하는 고유 id
	
	// 영화는 title, TV는 name
	@JsonAlias({"title", "name"})
	private String contentsTitle; //제목
	
    @JsonAlias({"media_type"})
	private String contentsType; //컨텐츠 유형 (movie / tv)
	
	@JsonAlias({"overview"})
	private String contentsOverview; //영화/시리즈 개요
	
	@JsonAlias({"poster_path"})
	private String contentsPosterPath; //포스터 이미지 경로
	
	@JsonAlias({"backdrop_path"})
	private String contentsBackdropPath; //배경 이미지 경로
	
	@JsonAlias({"vote_average"})
	private Double contentsVoteAverage; //평균평점
	
	private Integer contentsRuntime; //재생시간(분단위)
	
	// 영화는 release_date, TV는 first_air_date
    @JsonAlias({"release_date", "first_air_date"})
	private String contentsReleaseDate; // 개봉일/최초 방영일
    
    private String contentsDirector; //감독
    
    private String contentsMainCast;
    
    @JsonAlias({"genres"})
    private List<ContentsGenreDto> genres;

}
