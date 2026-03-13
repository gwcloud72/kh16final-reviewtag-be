package com.kh.finalproject.dto;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberIconDto {
    private int memberIconId;       // PK
   
    private String memberId;        // 회원 ID
    private int iconId;             // 아이콘 ID 번호
    
    private Timestamp memberIconObtainTime;
    private String isEquipped;      // 장착 여부 (Y/N)
    
    // 조인해서 가져올 아이콘 상세 정보
    private String iconName;
    private String iconRarity;
    private String iconSrc;
}