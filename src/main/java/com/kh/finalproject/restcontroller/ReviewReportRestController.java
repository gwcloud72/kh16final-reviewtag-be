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

import com.kh.finalproject.dao.ReviewDao;
import com.kh.finalproject.dao.ReviewReportDao;
import com.kh.finalproject.dto.ReviewDto;
import com.kh.finalproject.dto.ReviewReportDto;
import com.kh.finalproject.error.NeedPermissionException;
import com.kh.finalproject.service.ReviewService;
import com.kh.finalproject.vo.ReviewReportListVO;
import com.kh.finalproject.vo.TokenVO;

@CrossOrigin
@RestController
@RequestMapping("/review/report")
public class ReviewReportRestController {
	
	@Autowired
	private ReviewReportDao reviewReportDao;
	@Autowired
	private ReviewService reviewService;
	
	
	@PostMapping("/")
	public void insertReviewReport(
			@RequestBody ReviewReportDto reviewReportDto,
			@RequestAttribute TokenVO tokenVO
			) {
		//신고자 아이디 설정
		String loginId = tokenVO.getLoginId();
		if(loginId == null ) throw new NeedPermissionException();
		reviewReportDto.setReviewReportMemberId(loginId);

		// 중복 신고 체크 할 것인가?
		
		//신고 타입이 'OTHER'가 아닐 경우 content null 처리
		if (!"OTHER".equals(reviewReportDto.getReviewReportType())) {
			reviewReportDto.setReviewReportContent(null);
		}

		// 리뷰 신고시 신뢰도 -1
		Long reviewId = reviewReportDto.getReviewReportReviewId();
		reviewService.reportReview(reviewId);

		//등록 실행
		reviewReportDao.insertReviewReport(reviewReportDto);
	}
	
	//신고 전체 목록 조회 (관리자용)
	@GetMapping("/list")
	public List<ReviewReportListVO> getList(
			@RequestAttribute TokenVO tokenVO
			) {
		if(tokenVO.getLoginLevel().equals("관리자")==false) throw new NeedPermissionException();
		return reviewReportDao.selectList();
	}
	
	//신고 삭제 (관리자용)
	@DeleteMapping("/{reviewReportId}")
	public void deleteReport(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable long reviewReportId) {
		
		String loginLevel = tokenVO.getLoginLevel();
		
		reviewService.DeleteReview(loginLevel, reviewReportId);
	}
	
	// 신고 유형별 횟수
	@GetMapping("/stats/{reviewReportId}")
	public List<Map<String, Object>> count(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable long reviewReportReviewId){
		return reviewReportDao.countByType(reviewReportReviewId);
	}

	
}