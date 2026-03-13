package com.kh.finalproject.dto;

import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown =true)
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class AttendanceHistoryDto {
	private int attendanceHistoryNo;
    private String attendanceHistoryMemberId;
    private Timestamp attendanceHistoryDate;
}