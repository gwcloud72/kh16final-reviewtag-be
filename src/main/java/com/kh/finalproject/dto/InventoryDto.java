package com.kh.finalproject.dto;

import java.sql.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class InventoryDto {
    private long inventoryNo;          // INVENTORY_NO
    private String inventoryMemberId;  // INVENTORY_MEMBER_ID
    private long inventoryItemNo;      // INVENTORY_ITEM_NO
    private Timestamp inventoryCreatedAt; // INVENTORY_CREATED_AT
    private int inventoryQuantity;     // INVENTORY_QUANTITY
    private String inventoryEquipped;  // INVENTORY_EQUIPPED ('Y' or 'N')
    
    // 조인용 필드 (상품 이름 등 필요시)
    private String pointItemName;
    private String pointItemSrc;
    private String pointItemType;
    private String pointItemContent;
}