package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.QuizDto;
import com.kh.finalproject.vo.MemberAddQuizListVO;
import com.kh.finalproject.vo.MemberQuizListVO;
import com.kh.finalproject.vo.MemberQuizRateVO;
import com.kh.finalproject.vo.PageVO;

@Repository
public class MemberQuizDao {

	@Autowired
	private SqlSession sqlSession;
	
	//내가 등록한 퀴즈 목록 조회
	public List<MemberAddQuizListVO> selectMyQuizListWithPage(String loginId, PageVO pageVO) {
		Map<String, Object> param = new HashMap<>();
		param.put("loginId", loginId);
		param.put("pageVO", pageVO);
	    return sqlSession.selectList("memberQuiz.listByMyQuizWithPage", param);
	}
	public int countMyQuiz(String loginId) {
		return sqlSession.selectOne("memberQuiz.countMyQuiz", loginId);
	}
	
	
	/// 내가 푼 퀴즈 관련 항목
	// 푼 퀴즈 리스트
	public List<MemberQuizListVO> selectAnswerListWithPage(String loginId, PageVO pageVO){
		Map<String, Object> param = new HashMap<>();
		param.put("loginId", loginId);
		param.put("pageVO", pageVO);
		return sqlSession.selectList("memberQuiz.listByAnswerQuizWithPage",param);
	}
	public int countAnswerQuiz(String loginId) {
		return sqlSession.selectOne("memberQuiz.countAnswerQuiz", loginId);
	}
	
	
	// 푼 퀴즈 정답률
	public List<MemberQuizRateVO> selectAnswerQuizRate(String loginId){
		return sqlSession.selectList("memberQuiz.listByAnswerQuizRate", loginId);
	}
	
	
	
}
