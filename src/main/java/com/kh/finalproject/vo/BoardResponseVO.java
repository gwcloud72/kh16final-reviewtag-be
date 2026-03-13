package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BoardResponseVO {
	private boolean response;
	private String responseType;
	private int likeCount;
	private int unlikeCount;
}
