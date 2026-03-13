package com.kh.finalproject.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data  @NoArgsConstructor @AllArgsConstructor @Builder
public class WatchlistDto {

	private Long watchlistContent; 
	private String watchlistMember;
	private String watchlistType;
	private LocalDateTime watchlistTime;

}
