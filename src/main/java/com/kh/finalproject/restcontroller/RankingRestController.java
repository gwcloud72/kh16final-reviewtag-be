package com.kh.finalproject.restcontroller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dao.RankingDao;
import com.kh.finalproject.vo.ContentsRankingVO;
import com.kh.finalproject.vo.RealTimeQuizVO;
import com.kh.finalproject.vo.UserRankingVO;

@CrossOrigin
@RestController
@RequestMapping("/ranking")
public class RankingRestController {
	
	@Autowired
	private RankingDao rankingDao;
	
	//이달의 퀴즈왕
	@GetMapping("/quiz")
	public List<UserRankingVO> getQuizRanking(
			@RequestParam(defaultValue = "5") int limit) {
		return rankingDao.selectQuizRanking(limit);
	}
	
	//이달의 리뷰왕
	@GetMapping("/review")
	public List<UserRankingVO> getReviewRanking(
			@RequestParam(defaultValue = "5") int limit) {
		return rankingDao.selectReviewRanking(limit);
	}
	
	//핫한 콘텐츠 랭킹
	@GetMapping("/hotContents")
	public List<ContentsRankingVO> getHotContentsRanking(
			@RequestParam(defaultValue = "5") int limit) {
		return rankingDao.selectHotContentsRanking(limit);
	}
	
	//실시간 등록된 퀴즈
	@GetMapping("/realtimeQuiz")
	public List<RealTimeQuizVO> getRealtimeQuiz(
			@RequestParam(defaultValue = "5") int limit) {
		return rankingDao.selectRealtimeQuiz(limit);
	}
}
