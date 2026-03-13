package com.kh.finalproject.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizLogDto {
		
	//기본키 설정
	private Long quizLogId;
	
	//외래키 설정
	private String quizLogMemberId;
	private Long quizLogQuizId;
	
	//퀴즈 정답 처리 및 푼 시간 설정
	@Builder.Default
	private String quizLogIsCorrect = "N";
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime quizLogSolvedAt;
	
	
}
