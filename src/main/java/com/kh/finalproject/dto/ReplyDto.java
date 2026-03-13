package com.kh.finalproject.dto;


import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @Builder @AllArgsConstructor
public class ReplyDto {
	private int replyNo; //시퀀스로 생성 (댓글 번호)
	private String replyWriter; //댓글 작성자 (member_id)
	private int replyTarget; //댓글 단 게시물의 번호 (board_no)
	private String replyContent; //댓글 내용
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime replyWtime, replyEtime;
}
