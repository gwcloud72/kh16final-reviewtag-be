package com.kh.finalproject.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class MemberWatchListVO {

	private long watchlistContent;
	private String watchlistMember;
	private String watchlistType;
	private LocalDateTime watchlistTime;
	
	private String contentsTitle;
	private String contentsType;
	private String contentsPosterPath;
	private String contentsDirector;
	private String contentsMainCast;
	private String contentsRuntime;
	
}
