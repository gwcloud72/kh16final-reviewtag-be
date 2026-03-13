package com.kh.finalproject.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardReportDetailVO {
    private String memberNickname;      //신고자
    private String boardTitle;
    private String boardReportContent;   //신고 내용
    private LocalDateTime boardReportDate;   //신고일
}
