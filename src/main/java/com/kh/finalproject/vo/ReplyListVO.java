package com.kh.finalproject.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReplyListVO {
	private int replyNo;
	private String replyContent;
	private String replyWriter;
	private int replyTarget;
	private LocalDateTime replyWtime;
	private LocalDateTime replyEtime;
	private boolean writer;//댓글 작성자가 게시글 작성자인지 여부
	private boolean owner;//댓글 작성자가 현재 사용자인지 여부
}
