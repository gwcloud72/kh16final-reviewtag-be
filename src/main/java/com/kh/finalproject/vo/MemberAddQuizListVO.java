package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberAddQuizListVO {
	private Long quizId;
	private Long quizContentsId;
	private String quizQuestion;
	private String quizQuestionType;
	private String quizQuestionOption1;
	private String quizQuestionOption2;
	private String quizQuestionOption3;
	private String quizQuestionOption4;
	private String quizAnswer;
	private int quizSolveCount;
	
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
