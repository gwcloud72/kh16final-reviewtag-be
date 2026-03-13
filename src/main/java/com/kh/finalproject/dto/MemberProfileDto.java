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
public class MemberProfileDto {

	private String memberId;
	private String memberNickname;
	private String memberLevel;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
	private LocalDateTime memberJoin;
	private int memberReliability;
	
}
