package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.QuizReportDto;
import com.kh.finalproject.vo.QuizReportDetailVO;
import com.kh.finalproject.vo.QuizReportStatsVO;

@Repository
public class QuizReportDao {
	
	@Autowired
	private SqlSession sqlSession;
	
	//퀴즈 신고 등록
	public QuizReportDto insert(QuizReportDto quizReportDto) {
		long sequence = sqlSession.selectOne("quizReport.sequence");
		quizReportDto.setQuizReportId(sequence);
		sqlSession.insert("quizReport.insert", quizReportDto);
		
		return sqlSession.selectOne("quizReport.detail", sequence);
	}
	
	//관리자용 신고 목록 조회?
	//이건 그냥 퀴즈 별 신고 목록 조회
	public List<QuizReportDto> selectListByQuiz(long quizReportQuizId){
		return sqlSession.selectList("quizReport.selectListByQuiz", quizReportQuizId);
	}
	
	//그냥 전체 신고 목록 조회(추가해야되나)
	public List<QuizReportDto> selectList(){
		return sqlSession.selectList("quizReport.selectList");
	}
	
	//신고 유형 별 횟수 조회
	public List<Map<String, Object>> countByType(long quizReportQuizId){
		return sqlSession.selectList("quizReport.countByType", quizReportQuizId);
	}
	
	//중복 신고인지 조회
	public boolean checkHistory(String quizReportMemberId, long quizReportQuizId) {
	    Map<String, Object> params = new HashMap<>();
	    params.put("quizReportMemberId", quizReportMemberId);
	    params.put("quizReportQuizId", quizReportQuizId);
	    int count = sqlSession.selectOne("quizReport.checkHistory", params);
	    return count > 0;
	}
	
	// 상세 조회
	public QuizReportDto selectOne(long quizReportId) {
	    return sqlSession.selectOne("quizReport.detail", quizReportId);
	}
	
	// 신고 내역 삭제
	public boolean delete(long quizReportId) {
		
	    return sqlSession.delete("quizReport.delete", quizReportId) > 0;
	}
	
	//신고된 퀴즈 목록 조회
	public List<QuizReportStatsVO> selectReportedQuizList(Map<String, Object> params) {
		return sqlSession.selectList("quizReport.selectQuizReportList", params);
    }
	
	//특정 퀴즈의 기타 신고 상세 내역 조회
	public List<QuizReportDetailVO> selectReportDetails(int quizId) {
        return sqlSession.selectList("quizReport.selectReportEtcDetails", quizId);
    }

}
