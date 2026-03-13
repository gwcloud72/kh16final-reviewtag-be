package com.kh.finalproject.vo.contents;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.kh.finalproject.dto.contents.SearchResultDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @Builder @AllArgsConstructor
public class TmdbSearchResponseVO {
	 // TMDB 응답 JSON의 "results" 키와 매핑됩니다.
    @JsonAlias({"results"})
    private List<SearchResultDto> searchResults; // 검색 결과 목록
    
    // 이 필드들을 추가하면 TmdbApiResponseDto를 대체할 수 있습니다.
    private Integer page;
    @JsonAlias({"total_pages"})
    private Integer totalPages;
    @JsonAlias({"total_results"})
    private Integer totalResults;
}
