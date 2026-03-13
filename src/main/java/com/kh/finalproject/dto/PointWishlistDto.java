package com.kh.finalproject.dto;

import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class PointWishlistDto {
    // DB 컬럼명(POINT_WISHLIST_...)에 맞춰 필드명 수정 & long 타입 적용
    private long pointWishlistNo;         // POINT_WISHLIST_NO
    private String pointWishlistMemberId; // POINT_WISHLIST_MEMBER_ID
    private long pointWishlistItemNo;     // POINT_WISHLIST_ITEM_NO (long)
    private Timestamp pointWishlistTime;  // POINT_WISHLIST_TIME
    
    // 조인(JOIN)을 통해 가져올 상품 정보
    private String pointItemName;
    private String pointItemSrc;    
    private long pointItemPrice;          // 가격도 long으로 통일
}