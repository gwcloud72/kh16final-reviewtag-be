package com.kh.finalproject.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kh.finalproject.dao.BoardReportDao;
import com.kh.finalproject.dao.QuizReportDao;
import com.kh.finalproject.dao.ReviewReportDao;
import com.kh.finalproject.error.NeedPermissionException;
import com.kh.finalproject.vo.BoardReportDetailVO;
import com.kh.finalproject.vo.BoardReportStatsVO;
import com.kh.finalproject.vo.QuizReportDetailVO;
import com.kh.finalproject.vo.QuizReportStatsVO;

@Service
public class AdminService {
	
	@Autowired
	private QuizReportDao quizReportDao;
	
	@Autowired
	private BoardReportDao boardReportDao;
	
	@Autowired
	private ReviewReportDao reviewReportDao;
	
	//신고된 퀴즈 목록 조회
	public List<QuizReportStatsVO> getReportedQuizList(Map<String, Object> params){
		
		//관리자인지 검사
		String loginLevel = (String) params.get("loginLevel");
		if(loginLevel == null || !"관리자".equals(loginLevel))
			throw new NeedPermissionException();
		
		return quizReportDao.selectReportedQuizList(params);
	}
	
	//신고 상세 내역 조회(신고 종류가 기타일때만)
	public List<QuizReportDetailVO> getReportDetails(String loginLevel, int quizId){
		
		//관리자인지 검사
		if(loginLevel == null || !"관리자".equals(loginLevel))
			throw new NeedPermissionException();
				
		return quizReportDao.selectReportDetails(quizId);
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////////////
	//신고된 게시글 목록 조회
	public List<BoardReportStatsVO> getReportedBoardList(Map<String, Object> params){
			
		//관리자인지 검사
		String loginLevel = (String) params.get("loginLevel");
		if(loginLevel == null || !"관리자".equals(loginLevel))
			throw new NeedPermissionException();
			
		return boardReportDao.selectReportedBoardList(params);
	}
		
	//신고 상세 내역 조회(신고 종류가 기타일때만)
	public List<BoardReportDetailVO> getReportBDetails(String loginLevel, int boardNo){
			
		//관리자인지 검사
		if(loginLevel == null || !"관리자".equals(loginLevel))
			throw new NeedPermissionException();
					
		return boardReportDao.selectReportDetails(boardNo);
	}	
	

	//리뷰 신고 조회(관리자 페이지)
	
	//리뷰 신고 삭제(관리자 페이지)
}





