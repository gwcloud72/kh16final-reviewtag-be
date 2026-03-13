package com.kh.finalproject.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AttachmentDto {
	private int attachmentNo; //시퀀스로 생성(첨부파일 번호)
	private String attachmentName;
	private Long attachmentSize;
	private String attachmentType;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime attachmentTime;
}
