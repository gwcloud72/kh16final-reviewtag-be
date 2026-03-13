package com.kh.finalproject.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardDto {

	private int boardNo;
	private String boardTitle;
	private String boardWriter;
	private Long boardContentsId;
	private LocalDateTime boardWtime;
	private LocalDateTime boardEtime;
	private String boardText;
	private int boardViewCount;
	private int boardLike;
	private int boardUnlike;
	private int boardReplyCount;
	private String boardNotice;
	private int boardReportCount;
	
	private List<Integer> attachmentNoList;
 
}
