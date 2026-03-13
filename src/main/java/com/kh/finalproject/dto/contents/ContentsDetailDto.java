package com.kh.finalproject.dto.contents;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/// *** DB 최종 조회 Dto ***///

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ContentsDetailDto {
	private Long contentsId; 
	private String contentsTitle; 
	private String contentsType; 
	private String contentsOverview; 
	private String contentsPosterPath;
	private String contentsBackdropPath;
	private Double contentsVoteAverage;
	private Integer contentsRuntime; 
	private String contentsReleaseDate; 
	private String contentsDirector; 
    private String contentsMainCast; 
	private List<String> genreNames;
	
	private Long contentsLike;
	
	private double contentsRateAvg;
	private double contentsPriceAvg;
	
	private Integer contentsRateRank;
	private Integer contentsPriceRank;
	
}
