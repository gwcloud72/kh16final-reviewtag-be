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

import com.kh.finalproject.dto.QuizReportDto;
import com.kh.finalproject.service.QuizReportService;
import com.kh.finalproject.vo.TokenVO;

@CrossOrigin
@RestController
@RequestMapping("/quiz/report")
public class QuizReportRestController {
	
	@Autowired
	private QuizReportService quizReportService;
	
	//신고 등록
	@PostMapping("/")
	public String insertReport(
			@RequestAttribute TokenVO tokenVO,
			@RequestBody QuizReportDto quizReportDto
			) {
		
		return quizReportService.insertReport(quizReportDto, tokenVO.getLoginId());
	}
	
	//신고 전체 목록 조회 (관리자용)
	@GetMapping("/list")
	public List<QuizReportDto> getList(
			@RequestAttribute TokenVO tokenVO
			) {
		return quizReportService.getReportList(tokenVO.getLoginLevel());
	}
	
	//신고 상세 내역 조회 (관리자용)
	@GetMapping("/list/{quizId}")
	public List<QuizReportDto> getListByQuiz(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable long quizId) {
		
		return quizReportService.getReportListByQuiz(quizId, tokenVO.getLoginLevel());
	}
	
	//신고 유형별 통계 (관리자용)
	@GetMapping("/stats/{quizId}")
	public List<Map<String, Object>> getStats(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable long quizId) {
		
		return quizReportService.getReportStats(quizId, tokenVO.getLoginLevel());
	}
	
	//신고 삭제 (관리자용)
	@DeleteMapping("/{quizReportId}")
	public boolean deleteReport(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable long quizReportId) {
		return quizReportService.deleteReport(quizReportId, tokenVO.getLoginLevel());
	}
}
