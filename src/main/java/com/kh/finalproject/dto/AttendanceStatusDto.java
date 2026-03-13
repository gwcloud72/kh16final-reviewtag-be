package com.kh.finalproject.dto;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown =true)
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class AttendanceStatusDto {
    private int attendanceStatusNo;
    private String attendanceStatusMemberId;
    private int attendanceStatusCurrent; // 현재 연속
    private int attendanceStatusMax;     // 최고 기록
    private int attendanceStatusTotal;   // 총 횟수
    private Timestamp attendanceStatusLastdate;
}