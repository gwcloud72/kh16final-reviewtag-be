package com.kh.finalproject.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RankVO {
	
	private int rank;
    private String memberId;
    private String memberNickname;
    //private String memberImg;
    private int score;
}
