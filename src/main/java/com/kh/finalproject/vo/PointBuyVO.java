package com.kh.finalproject.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PointBuyVO {
 
    private long buyItemNo;
}