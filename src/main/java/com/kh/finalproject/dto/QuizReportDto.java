package com.kh.finalproject.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizReportDto {
	
	public Long quizReportId; //기본키
	public String quizReportMemberId; //신고자 아이디
	public Long quizReportQuizId; //신고된 퀴즈 아이디
	
	public String quizReportType; //신고 종류
	public String quizReportContent; //신고 내용(신고 종류가 ETC일 경우만 작성)
	public LocalDateTime quizReportDate; //신고 시간
}
