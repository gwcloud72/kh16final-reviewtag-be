package com.kh.finalproject.vo;

import com.kh.finalproject.dto.MemberDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MemberMypageResponseVO {
    private MemberDto member;      // 기본 정보 (이메일, 연락처 등)
    private MemberPointVO point;   // 치장 정보 및 포인트
    
    private int reviewCount;
    private int wishCount;
    private int quizCount;
}