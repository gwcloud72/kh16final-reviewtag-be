
package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberPointVO {
    private String memberId;    // 회원 ID
    private String nickname;    // 닉네임
    private int point;          // 현재 포인트
    private String level;       // 회원 등급 (일반회원, 우수회원, 관리자)
    
    // 치장 관련 정보 (없으면 null 또는 빈 문자열)
    private String nickStyle;   // 닉네임 꾸미기 클래스명 (예: "nick-rainbow")
    private String iconSrc;     // 장착한 아이콘 이미지 경로 (예: "cat.png")
    private String bgSrc;       // 배경 이미지 경로 (DECO_BG)
    private String frameSrc;    // 테두리 이미지 경로 (DECO_FRAME)
}
