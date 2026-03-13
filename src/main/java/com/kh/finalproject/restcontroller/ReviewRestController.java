package com.kh.finalproject.restcontroller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dao.ReviewDao;
import com.kh.finalproject.dao.ReviewLikeDao;
import com.kh.finalproject.dto.ReviewDto;
import com.kh.finalproject.error.TargetNotfoundException;
import com.kh.finalproject.service.DailyQuestService;
import com.kh.finalproject.service.ReviewService;
import com.kh.finalproject.vo.ReviewLikeVO;
import com.kh.finalproject.vo.ReviewVO;

@CrossOrigin
@RestController
@RequestMapping("/review")
public class ReviewRestController {

	private final ReviewService reviewService;
	@Autowired
	private ReviewDao reviewDao;
	@Autowired
	private ReviewLikeDao reviewLikeDao;

	@Autowired
	private DailyQuestService dailyQuestService;

	ReviewRestController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	// 등록
	@PostMapping("/")
	public void insert(@RequestBody ReviewDto reviewDto) {

//		reviewDao.insert(reviewDto);
		reviewService.addReview(reviewDto);
		if (reviewDto.getReviewWriter() != null) {
			dailyQuestService.questProgress(reviewDto.getReviewWriter(), "REVIEW");

		}
	}

	// 전체 리뷰 조회
	@GetMapping("/reviewContents/{reviewContents}")
	public List<ReviewVO> selectByContents(@PathVariable Long reviewContents) {
		return reviewDao.selectByContents(reviewContents);
	}

	// 로그인 리뷰 조회 (닉네임+신뢰도)
	@GetMapping("/user/{reviewContents}/{reviewWriter}")
	public ReviewVO selectByUserAndContents(@PathVariable String reviewWriter, @PathVariable Long reviewContents) {
		ReviewVO reviewVO = reviewDao.selectByUserAndContents(reviewWriter, reviewContents);
		return reviewVO;
	}

	// 단일 리뷰 조회 (닉네임+신뢰도)
		@GetMapping("/{reviewContents}/{reviewNo}")
		public ReviewVO selectOne(@PathVariable Long reviewContents, @PathVariable Long reviewNo) {
			return reviewDao.selectOne(reviewContents, reviewNo);
		}

	// 컨텐츠 아이디로 조회 (닉네임+신뢰도)
	@GetMapping("/list/{contentsId}")
	public List<ReviewDto> selectById(@PathVariable Long contentsId) {
		List<ReviewDto> reviewList = reviewDao.selectListByContentsId(contentsId);
		if (reviewList == null || reviewList.isEmpty())
			throw new TargetNotfoundException();
		return reviewList;
	}

	// 수정
	@PatchMapping("/{reviewContents}/{reviewNo}")
	public void updateUnit(@RequestBody ReviewDto reviewDto, @PathVariable Long reviewContents,
			@PathVariable Long reviewNo) {

		reviewDto.setReviewContents(reviewContents);
		reviewDto.setReviewNo(reviewNo);

		boolean success = reviewDao.updateUnit(reviewDto);
		if (!success) {
			throw new TargetNotfoundException(); // 수정실패
		}
	}

	//삭제 (닉네임+신뢰도)
	@DeleteMapping("/{reviewContents}/{reviewNo}")
	public void delete(@PathVariable("reviewContents") Long reviewContents, @PathVariable("reviewNo") Long reviewNo) {

		ReviewVO originVO = reviewDao.selectOne(reviewContents, reviewNo);

		if (originVO == null)
			throw new TargetNotfoundException();
		reviewService.deleteReview(reviewContents, reviewNo);
	}

	// 좋아요 관련

	@PostMapping("/check")
	public ReviewLikeVO check(@RequestParam String loginId, @RequestParam Long reviewNo) {
		boolean like = reviewLikeDao.check(loginId, reviewNo);
		Long count = reviewLikeDao.countByReviewNo(reviewNo);
		return ReviewLikeVO.builder().like(like).count(count).build();
	}

	@PostMapping("/action/{reviewNo}/{loginId}")
	public ReviewLikeVO action(@PathVariable String loginId, @PathVariable Long reviewNo) {
		boolean before = reviewLikeDao.check(loginId, reviewNo);
		if (before) {// 좋아요 한 상태면
			reviewLikeDao.delete(loginId, reviewNo);
		} else {// 좋아요 안한 상태면
			reviewLikeDao.insert(loginId, reviewNo);
		}

		reviewService.LikeReviewRel(reviewNo); // 좋아요 상태 = 신뢰도 반영

		Long count = reviewLikeDao.countByReviewNo(reviewNo);

		reviewDao.updateReviewReviewLike(reviewNo, count);
		return ReviewLikeVO.builder().like(!before).count(count).build();
	}
	

}
