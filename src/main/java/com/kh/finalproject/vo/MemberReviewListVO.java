package com.kh.finalproject.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class MemberReviewListVO {

	private Long reviewNo; //리뷰 넘버
	private String contentsTitle;
	private Long reviewContents; //영화id 외래키
	private String reviewWriter; //리뷰 작성자(멤버id 외래키)
	private Integer reviewRating; //리뷰 별점
	private Integer reviewPrice; //리뷰 가격
	@NotBlank
	private String reviewText; //리뷰 내용
	@PositiveOrZero
	@NotNull
	private Integer reviewLike; //리뷰 좋아요
	@PositiveOrZero
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime reviewWtime; //리뷰 작성시간
	private String contentsPosterPath;

	
}
