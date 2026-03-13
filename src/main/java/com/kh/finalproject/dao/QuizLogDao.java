package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.QuizLogDto;
import com.kh.finalproject.vo.QuizMyStatsVO;
import com.kh.finalproject.vo.RankVO;

@Repository
public class QuizLogDao {

	@Autowired
	private SqlSession sqlSession;
	
	//퀴즈 기록 등록 메소드
	public QuizLogDto insert(QuizLogDto quizLogDto) {
		long sequence = sqlSession.selectOne("quizLog.sequence");
		quizLogDto.setQuizLogId(sequence);
		sqlSession.insert("quizLog.insert", quizLogDto);
		
		return sqlSession.selectOne("quizLog.detail", sequence);
	}
	
	//퀴즈 기록 상세 정보 조회
	public QuizLogDto selectOne(long quizLogId){
		return sqlSession.selectOne("quizLog.detail", quizLogId);
	}
	
	//마이페이지 전용 조회
	public List<QuizLogDto> selectListByMember(String quizLogMemberId){
		return sqlSession.selectList("quizLog.selectListByMember", quizLogMemberId);
	}
	
	//해당 영화의 전체 랭킹을 위한 조회
	public List<RankVO> getRanking(int contentsId) {
		return sqlSession.selectList("quizLog.selectRanking", contentsId);
	}
	
	//개인 랭킹을 위한 조회
	public int countCorrectAnswer(String quizLogMemberId) {
		return sqlSession.selectOne("quizLog.countCorrectAnswer", quizLogMemberId);
	}

	//개인 랭킹 통계 조회
	public QuizMyStatsVO getMyStats(int contentsId, String memberId) {

		Map<String, Object> params = new HashMap<>();
		params.put("contentsId", contentsId);
		params.put("memberId", memberId);
		
		return sqlSession.selectOne("quizLog.selectMyStats", params);
	}
	
	//어떤 사람이 해당 문제를 풀었는지 조회(관리자용)
	public List<QuizLogDto> selectList(long quizLogQuizId){
		return sqlSession.selectList("quizLog.selectList", quizLogQuizId);
	}
}





