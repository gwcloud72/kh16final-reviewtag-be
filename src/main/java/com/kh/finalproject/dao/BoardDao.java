package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.BoardDto;
import com.kh.finalproject.dto.QuizDto;
import com.kh.finalproject.vo.PageVO;

@Repository
public class BoardDao {

	@Autowired

	private SqlSession sqlSession;


	// 등록
	public int insert(BoardDto boardDto) {
		int boardNo = sqlSession.selectOne("board.sequence");
		boardDto.setBoardNo(boardNo);
		sqlSession.insert("board.insert",boardDto);
		return boardNo;
	}

	// 조회 : 전체 조회
	public List<BoardDto> selectList(){
		return sqlSession.selectList("board.selectList");
	}

	// 조회 : 전체 조회 (페이지네이션)
	public List<BoardDto> selectListWithPage(PageVO pageVO){
		return sqlSession.selectList("board.selectListWithPage", pageVO);
	}

	public int countBoard(String column, String keyword) {
		Map<String, Object> param = new HashMap<>();
		param.put("column", column);
		param.put("keyword", keyword);
		return sqlSession.selectOne("board.countBoard", param);
	}

	public List<BoardDto> selectWithPage(PageVO pageVO, String column, String keyword) {
		Map<String, Object> param = new HashMap<>();
		//param.put("pageVO", pageVO);
		param.put("begin", pageVO.getBegin());
		param.put("end", pageVO.getEnd());
		param.put("column", column);
		param.put("keyword", keyword);
		return sqlSession.selectList("board.selectList", param);
	}


	// 조회 : 컨텐츠별 조회
	// - 페이지네이션 O
	public List<BoardDto> selectListByContents(Long boardContentsId,PageVO pageVO){
		Map<String, Object> param = new HashMap<>();
		param.put("boardContentsId", boardContentsId);
		param.put("pageVO", pageVO);
		return sqlSession.selectList("board.selectListByContents", param);
	}

	public int countContentsBoard(long boardContentsId) {
		return sqlSession.selectOne("board.countContentsBoard", boardContentsId);
	}


	// 조회 : 컨텐츠별 5개 항목조회
	// - contents detail 적용 / 페이지네이션 X
	public List<BoardDto> selectListBy5Contents(Long boardContentsId){
		return sqlSession.selectList("board.selectListBy5Contents", boardContentsId);
	}

	// 상세조회
	public BoardDto selectOne(int boardNo) {
		return sqlSession.selectOne("board.detail", boardNo);
	}


	// 수정
	public boolean update(BoardDto boardDto) {
		return sqlSession.update("board.update",boardDto)>0;
	}

	// 삭제
	public boolean delete(int boardNo) {
		return sqlSession.delete("board.delete", boardNo)>0;
	}
	
	//신고 누적 횟수 변경
	public boolean increaseBoardReportCount(int boardNo) {
		int result = sqlSession.update("board.updateBoardReportCount", boardNo);
		
		return result > 0;
	}
		
	
		
	//신고 삭제시 카운트 1 감소 메소드
	public boolean decreaseReportCount(int boardNo) {
		return sqlSession.update("board.decreaseReportCount", boardNo) > 0;
	}	



	////////////////////첨부파일
	public void connect(int boardNo, int attachmentNo) {
		Map<String, Object> params = new HashMap<>();
		params.put("boardNo", boardNo);
		params.put("attachmentNo", attachmentNo);
		sqlSession.insert("board.connect", params);
	}

	public int findAttachment(int boardNo) {
		return sqlSession.selectOne("board.findAttachment", boardNo);
	}


	//조회수
	public int increaseViewCount(int boardNo) {
		sqlSession.update("board.increaseViewCount", boardNo);
		return sqlSession.selectOne("board.selectViewCount", boardNo);
	}
}
