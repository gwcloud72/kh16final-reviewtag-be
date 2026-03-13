package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.HeartDto;

@Repository
public class HeartDao {
	
	@Autowired
	private SqlSession sqlSession;
	
	//하트 충전
	public void createHeartWallet(String memberId) {
		sqlSession.insert("heart.createHeartWallet", memberId);
	}
	
	//하트 조회
	public HeartDto selectHeart(String memberId) {
		return sqlSession.selectOne("heart.selectHeart", memberId);
	}
	
	//하트 구매시 증가
	public void increaseHeart(String memberId, int amount) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("amount", amount);
        
        sqlSession.update("heart.increaseHeart", params);
    }
	
	//하트 감소
	public int decreaseHeart(String memberId) {
		return sqlSession.update("heart.decreaseHeart", memberId);
	}
	
	//하트 리필(5개 미만인 경우만)
	public boolean dailyRefill(String memberId) {
		return sqlSession.update("heart.dailyRefill", memberId) > 0;
	}
	
	//하트 리필 날짜만 변경(5개 이상인 경우만)
	public boolean updateRefillDateOnly(String memberId) {
		return sqlSession.update("heart.updateRefillDateOnly", memberId) > 0;
	}
	
}



