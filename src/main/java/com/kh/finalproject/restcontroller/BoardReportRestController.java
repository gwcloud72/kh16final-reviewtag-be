package com.kh.finalproject.restcontroller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dto.BoardReportDto;
import com.kh.finalproject.service.BoardReportService;
import com.kh.finalproject.vo.TokenVO;

@CrossOrigin
@RestController
@RequestMapping("/board/report")
public class BoardReportRestController {
	
	@Autowired
	private BoardReportService boardReportService;
	
	//신고 등록
	@PostMapping("/")
	public String insertReport(
			@RequestAttribute TokenVO tokenVO,
			@RequestBody BoardReportDto boardReportDto
			) {
		
		return boardReportService.insertReport(boardReportDto, tokenVO.getLoginId());
	}
	
	//신고 전체 목록 조회 (관리자용)
	@GetMapping("/list")
	public List<BoardReportDto> getList(
			@RequestAttribute TokenVO tokenVO
			) {
		return boardReportService.getReportList(tokenVO.getLoginLevel());
	}
	
	//신고 상세 내역 조회 (관리자용)
	@GetMapping("/list/{boardNo}")
	public List<BoardReportDto> getListByQuiz(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable int boardNo) {
		
		return boardReportService.getReportListByBoard(boardNo, tokenVO.getLoginLevel());
	}
	
	//신고 유형별 통계 (관리자용)
	@GetMapping("/stats/{boardNo}")
	public List<Map<String, Object>> getStats(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable int boardNo) {
		
		return boardReportService.getReportStats(boardNo, tokenVO.getLoginLevel());
	}
	
	//신고 삭제 (관리자용)
	@DeleteMapping("/{boardReportId}")
	public boolean deleteReport(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable long boardReportId) {
		return boardReportService.deleteReport(boardReportId, tokenVO.getLoginLevel());
	}
}
