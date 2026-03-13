package com.kh.finalproject.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PointItemStoreDto {
    private Long pointItemNo;
    private String pointItemName;
    private String pointItemContent;
    private long pointItemPrice;
    private int pointItemStock;
    private String pointItemType;
    private String pointItemReqLevel;
    private int pointItemIsLimitedPurchase; // 한정판 여부
    private int pointItemDailyLimit;        // <--- 추가: 일일 구매 제한 횟수
    private String pointItemSrc;
}