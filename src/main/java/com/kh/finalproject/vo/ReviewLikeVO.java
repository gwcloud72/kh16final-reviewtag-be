package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewLikeVO {
	
	//화면에 좋아요 여부와 좋아요 개수를 알려주기 위한 클래스
	
	private boolean like;
	private Long count;
}
