package com.kh.finalproject.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown =  true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewReportDto {
	
	private Long reviewReportId;
	private String reviewReportMemberId;
	private Long reviewReportReviewId;
	private String reviewReportType;
	private String reviewReportContent;
	private LocalDateTime reviewReportDate;
}
