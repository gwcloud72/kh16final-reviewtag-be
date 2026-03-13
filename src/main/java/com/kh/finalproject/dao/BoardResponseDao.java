package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.BoardResponseDto;

@Repository
public class BoardResponseDao {
	
	@Autowired
	private SqlSession sqlSession;
	
	public void insert(BoardResponseDto boardResponseDto) {
		sqlSession.insert("boardResponse.insert", boardResponseDto);
	}
	
	public void delete(String memberId, int boardNo) {
		Map<String, Object> params = new HashMap<>();
		
		params.put("memberId", memberId);
		params.put("boardNo", boardNo);
		
		sqlSession.delete("boardResponse.delete", params);
	}
	
	public boolean countForCheck(String memberId, int boardNo) {
		Map<String, Object> params = new HashMap<>();
		
		params.put("memberId", memberId);
		params.put("boardNo", boardNo);
		Integer count = sqlSession.selectOne("boardResponse.countForCheck", params);
		return  count != null && count > 0;
	}
	
	public boolean isLike(String memberId, int boardNo) {
		Map<String, Object> params = new HashMap<>();
		
		params.put("memberId", memberId);
		params.put("boardNo", boardNo);
		Integer count = sqlSession.selectOne("boardResponse.isLike", params);
		return  count != null && count > 0;
	}
	
	public boolean isUnlike(String memberId, int boardNo) {
		Map<String, Object> params = new HashMap<>();
		
		params.put("memberId", memberId);
		params.put("boardNo", boardNo);
		Integer count = sqlSession.selectOne("boardResponse.isUnlike", params);
		return  count != null && count > 0;
	}
	
	public String selectMemberResponse(String memberId, int boardNo) {
		Map<String, Object> params = new HashMap<>();
		
		params.put("memberId", memberId);
		params.put("boardNo", boardNo);
		return sqlSession.selectOne("boardResponse.selectMemberResponse", params);
	}
	
	public int countLikeByboardNo(int boardNo) {
		return sqlSession.selectOne("boardResponse.countLikeByBoardNo", boardNo);
	}
	
	public int countUnlikeByboardNo(int boardNo) {
		return sqlSession.selectOne("boardResponse.countUnlikeByBoardNo", boardNo);
	}
	
	
	public void updateBoardLike(int boardNo) {
		sqlSession.update("boardResponse.updateBoardLike", boardNo);
	}
	
	public void updateBoardUnlike(int boardNo) {
		sqlSession.update("boardResponse.updateBoardUnlike", boardNo);
	}
}
