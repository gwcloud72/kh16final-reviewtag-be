package com.kh.finalproject.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.dao.BoardDao;
import com.kh.finalproject.dao.BoardReportDao;
import com.kh.finalproject.dto.BoardReportDto;
import com.kh.finalproject.dto.QuizReportDto;
import com.kh.finalproject.error.NeedPermissionException;

@Service
public class BoardReportService {

	@Autowired
	private BoardReportDao boardReportDao;

	@Autowired
	private BoardDao boardDao;
	
	//신고하기
	@Transactional
	public String insertReport(BoardReportDto boardReportDto, String memberId) {
		
		//로그인 검사
		if(memberId == null) throw new NeedPermissionException();
		
		//토큰에서 뽑은 ID를 DTO에 세팅
		boardReportDto.setBoardReportMemberId(memberId);
		
		//중복 신고 체크
		boolean isReported = boardReportDao.checkHistory(
				boardReportDto.getBoardReportMemberId(), 
				boardReportDto.getBoardReportBoardNo()
		);
		
		if (isReported) {
			return "DUPLICATE"; //화면에 보낼 값(중단)
		}
		
		//신고 상세 내용 저장
		boardReportDao.insert(boardReportDto);
		
		//신고 횟수 증가
		boardDao.increaseBoardReportCount(boardReportDto.getBoardReportBoardNo());
		
		//화면에 보낼 값(성공)
		return "SUCCESS";
	}
	
	// 전체 신고 목록
	public List<BoardReportDto> getReportList(String memberLevel) {
		
		//관리자인지 검사
		if(!"관리자".equals(memberLevel)) throw new NeedPermissionException();
		
		return boardReportDao.selectList();
		
	}

	//특정 게시글 신고 내역 (관리자용)
	public List<BoardReportDto> getReportListByBoard(int boardNo, String memberLevel) {
		
		//관리자인지 검사
		if(!"관리자".equals(memberLevel)) throw new NeedPermissionException();
		
		return boardReportDao.selectListByBoard(boardNo);
		
	}

	//신고 유형별 통계 inapposite(부적절한 컨텐츠), hate(혐오/비방), SPAM(도배/광고), ETC(기타)
	public List<Map<String, Object>> getReportStats(int boardNo, String memberLevel) {
		
		//관리자인지 검사
		if(!"관리자".equals(memberLevel)) throw new NeedPermissionException();
		
		return boardReportDao.countByType(boardNo);
		
	}
	
	//신고 삭제(관리자용)
	@Transactional
	public boolean deleteReport(long boardReportId, String memberLevel) {
		
		//관리자인지 검사
		if(!"관리자".equals(memberLevel)) throw new NeedPermissionException();
		
		//게시글 정보 조회
		BoardReportDto boardReportDto = boardReportDao.selectOne(boardReportId);
		
		//게시글이 없으면 반환
		if(boardReportDto == null) return false; 
		
		//게시글 삭제
		boolean isDeleted = boardReportDao.delete(boardReportId);
		
		//게시글 삭제 완료 되었으면 신고 횟수 차감
		if (isDeleted) {
	        boardDao.decreaseReportCount(boardReportDto.getBoardReportBoardNo());
	    }
	    
	    return isDeleted;
	}
}
