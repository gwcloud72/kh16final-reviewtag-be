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
public class MemberDto {

	private String memberId;
	private String memberPw;
	private String memberNickname;
	private String memberBirth;
	private String memberContact;
	private String memberEmail;
	private String memberLevel;
	private int memberPoint;
	private String memberPost;
	private String memberAddress1;
	private String memberAddress2;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
	private LocalDateTime memberJoin;
	private int memberReliability;
	
}
