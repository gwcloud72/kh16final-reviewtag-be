package com.kh.finalproject.vo;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class PointItemPageVO {
    private int page = 1;      // 현재 페이지 번호
    private int size = 10;     // 한 페이지에 보여줄 개수
    private String type;       // 필터링할 유형 (DECO_BG, CHANGE_NICK 등)

    // MyBatis RowBounds 대신 사용할 시작/종료 번호 계산 (Oracle 기준)
    public int getBegin() {
        return (page - 1) * size + 1;
    }
    public int getEnd() {
        return page * size;
    }
}