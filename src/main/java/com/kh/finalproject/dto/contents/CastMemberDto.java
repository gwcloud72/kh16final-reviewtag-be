package com.kh.finalproject.dto.contents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/// *** 출연진 Dto ***///

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CastMemberDto {
	private String name;
    private Integer order;
}
