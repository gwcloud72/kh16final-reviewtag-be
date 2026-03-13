package com.kh.finalproject.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.ReviewReportDto;
import com.kh.finalproject.vo.ReviewReportListVO;

@Repository
public class ReviewReportDao {
	
	@Autowired
	private SqlSession sqlSession;
	
	
	//리뷰 신고 등록
	public void insertReviewReport(ReviewReportDto reviewReportDto) {
		sqlSession.insert("reviewReport.insertReviewReport", reviewReportDto);
	}
	
	//리뷰 신고 조회(마이페이지 and 관리자 페이지)
	public List<ReviewReportListVO> selectList(){
		return sqlSession.selectList("reviewReport.selectList");
	}
	
	// 개별 리뷰신고조회
	public ReviewReportDto selectOne(Long reviewReportId) {
		return sqlSession.selectOne("reviewReport.selectOne", reviewReportId);
	}
	
	
	//리뷰 신고 삭제(관리자 페이지)
	public boolean delete(long reviewReportId) {
	    return sqlSession.delete("reviewReport.delete", reviewReportId) > 0;
	}

	
	//신고 유형 별 횟수 조회
	public List<Map<String, Object>> countByType(long reviewReportReviewId){
		return sqlSession.selectList("reviewReport.countByType", reviewReportReviewId);
	}
}
