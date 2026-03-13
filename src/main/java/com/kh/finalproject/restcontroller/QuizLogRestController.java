package com.kh.finalproject.restcontroller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dto.QuizLogDto;
import com.kh.finalproject.service.QuizLogService;
import com.kh.finalproject.vo.QuizMyStatsVO;
import com.kh.finalproject.vo.RankVO;
import com.kh.finalproject.vo.TokenVO;

@CrossOrigin
@RestController
@RequestMapping("/quiz/log")
public class QuizLogRestController {
	
	@Autowired
	private QuizLogService quizLogService;
	
	//퀴즈 기록 등록
	@PostMapping("/submit")
	public int submitQuiz(
			@RequestAttribute TokenVO tokenVO, 
			@RequestBody List<QuizLogDto> logList
			) {
		
		return quizLogService.submitQuizSession(logList, tokenVO.getLoginId());
	}
	
	//마이페이지 조회
	@GetMapping("/mypage")
	public List<QuizLogDto> getMyLogs(
			@RequestAttribute TokenVO tokenVO
			) {
		
		return quizLogService.myQuizLogList(tokenVO.getLoginId());
	}
	
	//퀴즈 기록 상세 정보 조회
	@GetMapping("/{quizLogId}")
	public QuizLogDto detail(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable long quizLogId
			) {
		
		return quizLogService.quizLogDetail(quizLogId, tokenVO.getLoginId(), tokenVO.getLoginLevel());
	}
	
	
	//내 랭킹(점수) 확인
	@GetMapping("/score")
	public int getMyScore(
			@RequestAttribute TokenVO tokenVO
			) {
		
		return quizLogService.getMyScore(tokenVO.getLoginId());
	}
	
	//내 랭킹 통계 확인
	@GetMapping("/stats/{contentsId}/{memberId}")
    public QuizMyStatsVO getMyStats(
            @PathVariable int contentsId, 
            @PathVariable String memberId) {
        
        return quizLogService.getMyStats(contentsId, memberId);
    }
	
	//영화별 전체 랭킹 확인
	@GetMapping("/list/ranking/{contentsId}")
    public List<RankVO> getRanking(@PathVariable int contentsId) {
        return quizLogService.getRanking(contentsId);
    }
	
	//어떤 사람이 해당 문제를 풀었는지 확인(관리자용)
	@GetMapping("/quiz/{quizLogQuizId}")
	public List<QuizLogDto> getLogsByQuiz(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable long quizLogQuizId
			) {
		
		return quizLogService.quizLogList(quizLogQuizId, tokenVO.getLoginLevel());
	}
	
}





