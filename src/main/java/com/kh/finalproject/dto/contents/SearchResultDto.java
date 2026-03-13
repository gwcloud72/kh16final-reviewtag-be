package com.kh.finalproject.dto.contents;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SearchResultDto {
	 @JsonAlias({"id"})
	  private Long contentsId; // TMDB 고유 ID
	    
	  // 영화는 title, TV는 name
	  @JsonAlias({"title", "name"})
	  private String title; 
	    
	  // TMDB는 검색 결과에 media_type을 제공 (movie, tv, person 등)
	  @JsonAlias({"media_type"})
	  private String type; 

	  @JsonAlias({"release_date", "first_air_date"})
	  private String releaseDate; // 개봉일/최초 방영일

	  @JsonAlias({"poster_path"})
	  private String posterPath; // 포스터 경로
}
