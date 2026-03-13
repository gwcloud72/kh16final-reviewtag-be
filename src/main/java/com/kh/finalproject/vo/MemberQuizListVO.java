package com.kh.finalproject.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class MemberQuizListVO {
	
	private String quizLogMemberId;
	private int quizLogQuizId;
	private String quizLogIsCorrect;
	private String quizQuestion;
	private int quizSolveCount;
	private int quizContentsId;
	private String contentsTitle;
	private int correctCount;
	
	public double getCorrectRate() {
		if(quizSolveCount==0) {
			return 0;
		} else {
			return (double) correctCount / quizSolveCount;
		}
	}


}
