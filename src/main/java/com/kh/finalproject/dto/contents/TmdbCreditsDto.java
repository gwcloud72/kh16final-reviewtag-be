package com.kh.finalproject.dto.contents;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/// *** 배우 및 감독 목록 ***///

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TmdbCreditsDto {
	private List<CastMemberDto> cast; // 배우 목록
    private List<CrewMemberDto> crew; // 감독/스태프 목록
}
