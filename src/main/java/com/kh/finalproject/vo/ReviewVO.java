package com.kh.finalproject.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class ReviewVO {
	private String memberNickname; //멤버 닉네임 (+추가)
	@NotNull
	private Integer memberReliability; //멤버 신뢰도 (+추가)
	private Long reviewNo; 
	private Long reviewContents;
	private String reviewWriter;
	@Min(0)
	@Max(5)
	private Integer reviewRating; //금액제면 컬럼+ 필요
	@NotBlank
	private String reviewSpoiler; 
	@NotBlank
	private String reviewText;
	@PositiveOrZero
	@NotNull
	private Integer reviewLike;
	@PositiveOrZero
	@NotNull
	private Integer reviewRealiability;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime reviewWtime; 
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime reviewEtime;
	private Integer reviewPrice; //영화 가치
}
