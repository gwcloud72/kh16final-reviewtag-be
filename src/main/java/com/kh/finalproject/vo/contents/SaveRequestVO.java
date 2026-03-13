package com.kh.finalproject.vo.contents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @Builder @AllArgsConstructor
public class SaveRequestVO {
	private Long contentsId; // 선택한 컨텐츠 ID
    private String type;     // 선택한 컨텐츠 타입 ("movie" or "tv")
}
