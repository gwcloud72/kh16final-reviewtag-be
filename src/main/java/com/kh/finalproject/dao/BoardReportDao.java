package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.BoardDto;
import com.kh.finalproject.dto.BoardReportDto;
import com.kh.finalproject.vo.BoardReportDetailVO;
import com.kh.finalproject.vo.BoardReportStatsVO;

@Repository
public class BoardReportDao {
	
	@Autowired
	private SqlSession sqlSession;
	
	//게시글 신고 등록
	public BoardReportDto insert(BoardReportDto boardReportDto) {
		long sequence = sqlSession.selectOne("quizReport.sequence");
		boardReportDto.setBoardReportId(sequence);
		sqlSession.insert("boardReport.insert", boardReportDto);
		
		return sqlSession.selectOne("boardReport.detail", sequence);
	}
	
	//관리자용 신고 목록 조회?
	//이건 그냥 게시글 별 신고 목록 조회
	public List<BoardReportDto> selectListByBoard(int boardReportBoardNo){
		return sqlSession.selectList("boardReport.selectListByBoard", boardReportBoardNo);
	}
	
	//그냥 전체 신고 목록 조회(추가해야되나)
	public List<BoardReportDto> selectList(){
		return sqlSession.selectList("boardReport.selectList");
	}
	
	//신고 유형 별 횟수 조회
	public List<Map<String, Object>> countByType(int boardReportBoardNo){
		return sqlSession.selectList("boardReport.countByType", boardReportBoardNo);
	}
	
	//중복 신고인지 조회
	public boolean checkHistory(String boardReportMemberId, int boardReportBoardNo) {
	    Map<String, Object> params = new HashMap<>();
	    params.put("boardReportMemberId", boardReportMemberId);
	    params.put("boardReportBoardNo", boardReportBoardNo);
	    int count = sqlSession.selectOne("boardReport.checkHistory", params);
	    return count > 0;
	}
	
	// 상세 조회
	public BoardReportDto selectOne(long boardReportId) {
	    return sqlSession.selectOne("boardReport.detail", boardReportId);
	}
	
	// 신고 내역 삭제
	public boolean delete(long boardReportId) {
	    return sqlSession.delete("boardReport.delete", boardReportId) > 0;
	}
	
	//신고된 게시글 목록 조회
	public List<BoardReportStatsVO> selectReportedBoardList(Map<String, Object> params) {
		return sqlSession.selectList("boardReport.selectBoardReportList", params);
    }
	
	//특정 게시글의 기타 신고 상세 내역 조회
	public List<BoardReportDetailVO> selectReportDetails(int boardNo) {
        return sqlSession.selectList("boardReport.selectReportEtcDetails", boardNo);
    }
	
	//신고된 게시글 내용 조회
	public BoardDto selectBoardText(int boardReportBoardNo) {
		return sqlSession.selectOne("boardReport.selectBoardText", boardReportBoardNo);
	}

}
