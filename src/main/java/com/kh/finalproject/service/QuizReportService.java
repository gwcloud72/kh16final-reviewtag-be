package com.kh.finalproject.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.dao.QuizDao;
import com.kh.finalproject.dao.QuizReportDao;
import com.kh.finalproject.dto.QuizReportDto;
import com.kh.finalproject.error.NeedPermissionException;

@Service
public class QuizReportService {

	@Autowired
	private QuizReportDao quizReportDao;

	@Autowired
	private QuizDao quizDao;
	
	//신고하기
	@Transactional
	public String insertReport(QuizReportDto quizReportDto, String memberId) {
		
		//로그인 검사
		if(memberId == null) throw new NeedPermissionException();
		
		//토큰에서 뽑은 ID를 DTO에 세팅
		quizReportDto.setQuizReportMemberId(memberId);
		
		//중복 신고 체크
		boolean isReported = quizReportDao.checkHistory(
				quizReportDto.getQuizReportMemberId(), 
				quizReportDto.getQuizReportQuizId()
		);
		
		if (isReported) {
			return "DUPLICATE"; //화면에 보낼 값(중단)
		}
		
		//신고 상세 내용 저장
		quizReportDao.insert(quizReportDto);
		
		//신고 횟수 증가
		quizDao.increaseQuizReportCount(quizReportDto.getQuizReportQuizId());
		
		//화면에 보낼 값(성공)
		return "SUCCESS";
	}
	
	// 전체 신고 목록
	public List<QuizReportDto> getReportList(String memberLevel) {
		
		//관리자인지 검사
		if(!"관리자".equals(memberLevel)) throw new NeedPermissionException();
		
		return quizReportDao.selectList();
		
	}

	//특정 퀴즈 신고 내역 (관리자용)
	public List<QuizReportDto> getReportListByQuiz(long quizId, String memberLevel) {
		
		//관리자인지 검사
		if(!"관리자".equals(memberLevel)) throw new NeedPermissionException();
		
		return quizReportDao.selectListByQuiz(quizId);
		
	}

	//신고 유형별 통계 ERROR(문제오류), ANSWER(정답오류), SPAM(도배/광고), ETC(기타)
	public List<Map<String, Object>> getReportStats(long quizId, String memberLevel) {
		
		//관리자인지 검사
		if(!"관리자".equals(memberLevel)) throw new NeedPermissionException();
		
		return quizReportDao.countByType(quizId);
		
	}
	
	//신고 삭제(관리자용)
	@Transactional
	public boolean deleteReport(long quizReportId, String memberLevel) {
		
		//관리자인지 검사
		if(!"관리자".equals(memberLevel)) throw new NeedPermissionException();
		
		//퀴즈 정보 조회
		QuizReportDto quizReportDto = quizReportDao.selectOne(quizReportId);
		
		//퀴즈가 없으면 반환
		if(quizReportDto == null) return false; 
		
		//퀴즈 삭제
		boolean isDeleted = quizReportDao.delete(quizReportId);
		
		//퀴즈 삭제 완료 되었으면 신고 횟수 차감
		if (isDeleted) {
	        quizDao.decreaseReportCount(quizReportDto.getQuizReportQuizId());
	    }
	    
	    return isDeleted;
	}
}
