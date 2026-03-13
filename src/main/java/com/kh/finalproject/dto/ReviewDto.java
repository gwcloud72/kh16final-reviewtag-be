package com.kh.finalproject.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewDto {
	private Long reviewNo; //리뷰 넘버
	private Long reviewContents ; //영화id 외래키 (DB의 review_contents지만, 매핑을 위해 이름 변경)
	private String reviewWriter; //리뷰 작성자(멤버id 외래키)
	@Min(0)
	@Max(5)
	private Integer reviewRating; //리뷰 별점
	@NotBlank
	private String reviewSpoiler; //리뷰 스포체크
	@NotBlank
	private String reviewText; //리뷰 내용
	@PositiveOrZero
	@NotNull
	private Integer reviewLike; //리뷰 좋아요
	@PositiveOrZero
	@NotNull
	private Integer reviewRealiability; //리뷰 신뢰도
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime reviewWtime; //리뷰 작성시간
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime reviewEtime; //리뷰 수정시간
	@NotNull
	private Integer reviewPrice; //영화 가치
}
