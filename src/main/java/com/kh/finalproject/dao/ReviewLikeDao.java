package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewLikeDao {
	
	@Autowired
	private SqlSession sqlSession;
	
	public void insert(String memberId, Long reviewNo) {
		Map<String, Object> params = new HashMap<>();
		params.put("memberId", memberId);
		params.put("reviewNo", reviewNo);
		sqlSession.insert("reviewLike.insert", params);
	}
	
	public boolean check (String memberId, Long reviewNo) {
		Map<String, Object> params = new HashMap<>();
		params.put("memberId", memberId);
		params.put("reviewNo", reviewNo);
		Long count =  sqlSession.selectOne("reviewLike.countForCheck", params);
		return count != null && count > 0;
	}
	
	public void delete (String memberId, Long reviewNo) {
		Map<String, Object> params = new HashMap<>();
		params.put("memberId", memberId);
		params.put("reviewNo", reviewNo);
		sqlSession.delete("reviewLike.delete", params);
	}
	
	public Long countByReviewNo (long reviewNo) {
		return sqlSession.selectOne("reviewLike.countByReviewNo", reviewNo);
	}
	
	public Long countByMemberId (String memberId) {
		return sqlSession.selectOne("reviewLike.countByMemberId", memberId);
	}
	
	public List<Long> selectListByMemberId (String memberId) {
		return sqlSession.selectList("reviewLike.selectListByMemberId", memberId);
	} 
	
//	좋아요 신뢰도
	public int countTotalLikeByWriter (String writer) {
		return sqlSession.selectOne("reviewLike.countTotalLikeByWriter", writer);
	}
	
}