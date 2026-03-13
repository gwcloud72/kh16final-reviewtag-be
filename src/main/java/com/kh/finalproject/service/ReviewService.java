package com.kh.finalproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.dao.MemberDao;
import com.kh.finalproject.dao.ReviewDao;
import com.kh.finalproject.dao.ReviewLikeDao;
import com.kh.finalproject.dao.ReviewReportDao;
import com.kh.finalproject.dto.ReviewDto;
import com.kh.finalproject.dto.ReviewReportDto;
import com.kh.finalproject.error.NeedPermissionException;
import com.kh.finalproject.error.TargetNotfoundException;
import com.kh.finalproject.vo.ReviewVO;

@Service
public class ReviewService {

	@Autowired
	private ReviewDao reviewDao;
	@Autowired
	private MemberDao memberDao;
	@Autowired
	private ReviewLikeDao reviewLikeDao;
	@Autowired
	private ReviewReportDao reviewReportDao;
	

	@Transactional
	// 리뷰 작성시 신뢰도 +1
	public void addReview(ReviewDto reviewDto) {
		reviewDao.insert(reviewDto);
		String writer = reviewDto.getReviewWriter();

		memberDao.updateReliability(writer, 1);
	}

	// 리뷰 삭제시 신뢰도 -1
	@Transactional
	public void deleteReview(Long reviewContents, Long reviewNo) {
		ReviewVO review = reviewDao.selectOne(reviewContents, reviewNo);
		if (review == null)
			throw new TargetNotfoundException();

		String writer = review.getReviewWriter();

		int reliability = memberDao.selectReliability(writer);
		if (reliability > 0) {
			memberDao.updateReliability(writer, -1);
		}

		reviewDao.delete(reviewContents, reviewNo);
	}
	
	// 리뷰 신고시 신뢰도 -1
	@Transactional
	public void reportReview(Long reviewNo) {
		System.out.println("reviewNo------------"+reviewNo);
		String writer = reviewDao.findWriterByReviewNo(reviewNo);
		if (writer == null) throw new TargetNotfoundException();
		int reliability = memberDao.selectReliability(writer);
		if (reliability > 0) {
			memberDao.updateReliability(writer, -1);
		}
	}
	
	

	// 좋아요에 대한 신뢰도 갱신 (3좋아요 1신뢰도)
	public void LikeReviewRel(Long reviewNo) {
		String writer = reviewDao.findWriterByReviewNo(reviewNo);

		int totalLike = reviewLikeDao.countTotalLikeByWriter(writer);
		int likeReliability = totalLike / 3;
//		System.out.println("총 좋아요 :"+totalLike); 
//		System.out.println("좋아요가 3개일 시, 1신뢰도 값:"+likeReliability);

		int reviewCount = reviewDao.countReviewByWriter(writer);
//		System.out.println("총 리뷰 개수 :"+reviewCount); 

		////////////////////////

		int totalReliability = likeReliability + reviewCount;
		System.out.println("총 신뢰도 :" + totalReliability);

		memberDao.updateReliabilitySet(writer, totalReliability);
	}
	
	//신고 삭제
	@Transactional
	public void DeleteReview(String loginLevel, long reviewReportId) {
		
		//관리자인지 검사
		if(loginLevel.equals("관리자")==false) throw new NeedPermissionException();
		
		//신고된 리뷰인지 확인
		ReviewReportDto reviewReportDto = reviewReportDao.selectOne(reviewReportId);
		if(reviewReportDto == null) throw new TargetNotfoundException();
		
		//리뷰 아이디 찾기
		long reviewId = reviewReportDto.getReviewReportReviewId();
		
		//컨텐츠 아이디 찾기
		ReviewDto findDto = reviewDao.selectOne(reviewId);
		
		//리뷰 삭제
		if(findDto != null) {
	        reviewDao.deleteByPK(reviewId);
	    }
		
		//신고 삭제
		reviewReportDao.delete(reviewReportId);
	}

}