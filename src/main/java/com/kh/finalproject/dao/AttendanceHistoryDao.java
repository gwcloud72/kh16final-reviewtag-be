package com.kh.finalproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.AttendanceHistoryDto;



@Repository
public class AttendanceHistoryDao {

	@Autowired
	private SqlSession sqlSession;

	public int insert(AttendanceHistoryDto attendanceHistoryDto) {
		return sqlSession.insert("attendanceHistory.insert", attendanceHistoryDto);
	}

	public boolean delete(int historyNo) {
		return sqlSession.delete("attendanceHistory.delete", historyNo) > 0;
	}

	public boolean deleteAll(String memberId) {
		return sqlSession.delete("attendanceHistory.deleteAll", memberId) > 0;
	}

	public List<AttendanceHistoryDto> selectList(String memberId) {
		return sqlSession.selectList("attendanceHistory.selectList", memberId);
	}
	public List<String> selectCalendarDates(String memberId) {
        return sqlSession.selectList("attendanceHistory.selectCalendarDates", memberId);
    }
}