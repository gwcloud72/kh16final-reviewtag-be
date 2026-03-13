package com.kh.finalproject.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor 
public class PointCancelVO {
    // 프론트엔드에서 보낼 때 "inventoryNo": 10 형식으로 보내야 함
    private long inventoryNo;  
}