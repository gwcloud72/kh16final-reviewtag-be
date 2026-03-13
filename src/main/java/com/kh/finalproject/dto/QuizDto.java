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
public class QuizDto {
	
	//기본키 설정
	private Long quizId;
	
	//외래키 설정
	private Long quizContentsId;
	private String quizCreatorId;
	
	//퀴즈 제목과 타입 설정
	private String quizQuestion;
	@Builder.Default
	private String quizQuestionType = "MULTI";
	
	//퀴즈 보기(4지 선다는 4번까지 or OX 퀴즈는 2번까지 사용)
	private String quizQuestionOption1;
	private String quizQuestionOption2;
	private String quizQuestionOption3;
	private String quizQuestionOption4;
	
	//퀴즈 정답 관련
	private String quizAnswer;
	private Long quizReportCount; //신고 누적 횟수
	@Builder.Default
	private String quizStatus = "ACTIVE";
	
	//퀴즈를 푼 사람 수
	private int quizSolveCount;
	
	//시간 관련
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime quizCreatedAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime quizUpdatedAt;
	
	
	
}
