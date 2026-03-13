package com.kh.finalproject.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.AttendanceStatusDto;

@Repository
public class AttendanceStatusDao {

	@Autowired
	private SqlSession sqlSession;

	public int insert(AttendanceStatusDto attendanceStatusDto) {
		return sqlSession.insert("attendanceStatus.insert", attendanceStatusDto);
	}

	public boolean update(AttendanceStatusDto attendanceStatusDto) {
		return sqlSession.update("attendanceStatus.update", attendanceStatusDto) > 0;
	}
	
	public boolean delete(String memberId) {
		return sqlSession.delete("attendanceStatus.delete", memberId) > 0;
	}


	public AttendanceStatusDto selectOne(String memberId) {
		return sqlSession.selectOne("attendanceStatus.selectOne", memberId);
	}
}