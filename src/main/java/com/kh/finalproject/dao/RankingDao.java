package com.kh.finalproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.vo.ContentsRankingVO;
import com.kh.finalproject.vo.RealTimeQuizVO;
import com.kh.finalproject.vo.UserRankingVO;

@Repository
public class RankingDao {
	
	@Autowired
	private SqlSession sqlSession;
	
	//이달의 퀴즈왕
	public List<UserRankingVO> selectQuizRanking(int limit){
		return sqlSession.selectList("ranking.selectQuizRanking", limit);
	}
	
	//이달의 리뷰왕
	public List<UserRankingVO> selectReviewRanking(int limit){
		return sqlSession.selectList("ranking.selectReviewRanking", limit);
	}
	
	//최근 가장 핫한 컨텐츠 랭킹
	public List<ContentsRankingVO> selectHotContentsRanking(int limit){
		return sqlSession.selectList("ranking.selectHotContentsRanking", limit);
	}
	
	//가장 최근 등록된 퀴즈
	public List<RealTimeQuizVO> selectRealtimeQuiz(int limit){
		return sqlSession.selectList("ranking.selectRealtimeQuiz", limit);
	}
}
