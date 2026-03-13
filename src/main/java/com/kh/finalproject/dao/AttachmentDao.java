package com.kh.finalproject.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.AttachmentDto;

@Repository
public class AttachmentDao {
	
	@Autowired
	private SqlSession sqlSession;
	
	//등록
	public int sequence() {
		return sqlSession.selectOne("attachment.sequence");
	}
	
	public void insert(AttachmentDto attachmentDto) {
		sqlSession.insert("attachment.insert", attachmentDto);
	}
	
	//상세
	public AttachmentDto selectOne(int attachmentNo) {
		return sqlSession.selectOne("attachment.selectOne", attachmentNo);
	}
	
	//삭제
	public void delete(int attachmentNo) {
		sqlSession.delete("attachment.delete", attachmentNo);
	}
	
}
