package com.kh.finalproject.dto.contents;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// ***
///개별 상세 조회 API (/{type}/{id})에서 runtime 정보를 받기 위한 최소 DTO
/// ***

@Data @NoArgsConstructor @AllArgsConstructor
public class DetailRuntimeDto {
	// 1. 영화 런타임 (단일 Integer, JSON 필드명: "runtime")
    @JsonProperty("runtime")
    private Integer movieRuntime; 

    // 2. TV 에피소드 런타임 (List<Integer>, JSON 필드명: "episode_run_time")
    @JsonProperty("episode_run_time")
    private List<Integer> tvEpisodeRunTime; 
 
    /**
     * 최종 런타임 값 반환 (영화 또는 TV 시리즈 중 하나만 값을 가짐)
     */
    public Integer getSingleRuntime() {
        if (movieRuntime != null && movieRuntime > 0) { // 영화인 경우
            return movieRuntime;
        }
        // TV 시리즈인 경우, 리스트의 첫 번째 값 사용 (대부분의 에피소드 길이)
        if (tvEpisodeRunTime != null && !tvEpisodeRunTime.isEmpty()) {
            return tvEpisodeRunTime.get(0);
        }
        return 0; // 런타임 정보가 없으면 0 반환
    }
}