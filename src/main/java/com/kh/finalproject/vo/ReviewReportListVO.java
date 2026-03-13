package com.kh.finalproject.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewReportListVO {

	private Long reviewReportId;      // 리뷰신고 ID(시퀀스 번호)
	private String reviewReportMemberId;  // 신고자 ID
	private Long reviewReportReviewId; // 신고된 리뷰 ID
	private String reviewReportType; // 신고사유
	private String reviewReportContent; // 신고사유 기타일때 설명
	private LocalDateTime reviewReportDate; 
	private String reviewWriter; // 신고된 리뷰작성자
	private String reviewText; // 신고된 리뷰 내용
	private String contentsTitle; // 신고된 컨텐츠 제목
	private Long reviewReportCount;
	
	private String memberNickname; // 신고된 리뷰자 닉네임 (+추가)

}
