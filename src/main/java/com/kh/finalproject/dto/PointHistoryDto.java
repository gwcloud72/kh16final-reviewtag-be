package com.kh.finalproject.dto;

import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PointHistoryDto {
    private Long pointHistoryId;         
    private String pointHistoryMemberId; 
    private int pointHistoryAmount;        
    private String pointHistoryTrxType;  // GET, USE, SEND, RECEIVED
    private String pointHistoryReason;   // <--- 추가된 필드 (적립/사용 사유)
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private Timestamp pointHistoryCreatedAt; 

}