
package com.kh.finalproject.dto.contents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/// *** 연출진 Dto ***///

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CrewMemberDto {
	private String name;
	private String job;
}
