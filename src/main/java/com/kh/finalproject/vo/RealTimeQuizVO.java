package com.kh.finalproject.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//가장 최근에 작성된 퀴즈 랭킹
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RealTimeQuizVO {
    private String contentsTitle;
    private String memberNickname;
    private String quizCreatedAt;
    private int contentsId;
}
