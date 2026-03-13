package com.kh.finalproject.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class MemberQuizRateVO {

	private String quizLogMemberId;
	private int quizContentsId;
	private String contentsTitle;
	private int correctCount;
	private int wrongCount;
	
	@JsonProperty("correctRate")
	public double getCorrectRate() {
		return (double) correctCount / (correctCount+wrongCount);
	}
}
