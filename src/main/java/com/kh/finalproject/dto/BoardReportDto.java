package com.kh.finalproject.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardReportDto {
	
	public Long boardReportId; //기본키
	public String boardReportMemberId; //신고자 아이디
	public int boardReportBoardNo; //신고된 퀴즈 아이디
															//[도배/광고]  / 부적절한 컨텐츠 / [혐오/비방]  / 기타
	public String boardReportType; //신고 종류 (SPAM/INAPPOSITE/HATE/ECT)
	public String boardReportContent; //신고 내용(신고 종류가 ETC일 경우만 작성)
	public LocalDateTime boardReportDate; //신고 시간
}
