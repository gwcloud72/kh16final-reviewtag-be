package com.kh.finalproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.ReplyDto;

@Repository
public class ReplyDao {

	@Autowired
	private SqlSession sqlSession;
	
	public int insert(ReplyDto replyDto) {
		int replyNo = sqlSession.selectOne("reply.sequence");
		replyDto.setReplyNo(replyNo);
		sqlSession.insert("reply.insert", replyDto);
		return replyNo;
	}
	
	public void update(ReplyDto replyDto) {
		sqlSession.update("reply.update", replyDto);
	}
	
	public void delete(int replyNo) {
		sqlSession.delete("reply.delete", replyNo);
	}
	
	public ReplyDto selectOne(int replyNo) {
		return sqlSession.selectOne("reply.selectOne", replyNo);
	}
	
	public List<ReplyDto> selectByBoardNo(int boardNo) {
		return sqlSession.selectList("reply.selectByBoardNo", boardNo);
	}
	
	public List<ReplyDto> selectByWriter(String replyWriter) {
		return sqlSession.selectList("reply.selectByWriter", replyWriter);
	}
	
	public void updateBoardReplyCount(int boardNo) {
		sqlSession.update("reply.updateBoardReplyCount", boardNo);
	}
}
