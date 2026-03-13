package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.ReviewDto;
import com.kh.finalproject.vo.ReviewVO;

@Repository
public class ReviewDao {
	@Autowired
	private SqlSession sqlSession;
	
	//등록
	public void insert(ReviewDto reviewDto) {
		sqlSession.insert("review.insert", reviewDto);
	}
	
	//전체 조회
	public List<ReviewVO> selectByContents(Long reviewContents) { //전체
		return sqlSession.selectList("review.selectList", reviewContents);
	}
	

	//단일 조회(로직용)
//	public ReviewDto selectOne(Long reviewContents , Long reviewNo) {
//		Map<String, Object> map2 = new HashMap<>();
//		map2.put("reviewContents", reviewContents);
//		map2.put("reviewNo", reviewNo);
//		return sqlSession.selectOne("review.selectOne", map2);
//	}
	//단일 조회(닉네임+신뢰도)
	public ReviewVO selectOne(Long reviewContents , Long reviewNo) {
		Map<String, Object> map2 = new HashMap<>();
		map2.put("reviewContents", reviewContents);
		map2.put("reviewNo", reviewNo);
		return sqlSession.selectOne("review.selectOne", map2);
	}
	
	public ReviewVO selectByUserAndContents(String loginId, Long reviewContents) {
	    Map<String, Object> map = new HashMap<>();
	    map.put("loginId", loginId);
	    map.put("reviewContents", reviewContents);
	    return sqlSession.selectOne("review.selectByUserAndContents", map);
	}
	

	//contentsId로 list 조회
	public List<ReviewDto> selectListByContentsId (Long reviewContents) {
		return sqlSession.selectList("review.selectListByContentsId", reviewContents);
	}
	
	public ReviewDto selectOne(Long reviewNo) {
		return sqlSession.selectOne("review.selectOneByReviewNo", reviewNo);
	}
	
//	public List<ReviewDto> detail(String contentsTitle) { //컨텐츠 제목으로 조회
//		return sqlSession.selectList("review.detail", contentsTitle);
//	}
	
	//부분수정
	public boolean updateUnit(ReviewDto reviewDto) {
		return sqlSession.update("review.updateUnit", reviewDto) > 0;
	}
	
	//삭제
	public boolean delete(Long reviewContents, Long reviewNo) {
		Map<String, Object> map3 = new HashMap<>();
		map3.put("reviewContents", reviewContents);
		map3.put("reviewNo", reviewNo);
		return sqlSession.delete("review.delete", map3) > 0;
	}
	
	//하나만 삭제
	public boolean deleteByPK(long reviewId) {
		return sqlSession.delete("review.deleteByPK", reviewId) > 0;
	}

	//리뷰 쓴사람 조회
	public String findWriterByReviewNo(Long reviewNo) {
		return sqlSession.selectOne("review.findWriterByReviewNo", reviewNo);
	}
	
	//리뷰 개수 조회(좋아요 신뢰도 분리용)
		public int countReviewByWriter(String writer) {
			return sqlSession.selectOne("review.countReviewByWriter", writer);
		}
	
	
	//////////////////////////////////////////
	
	//좋아요 관련
	public void updateReviewLike(Long reviewNo) {
		sqlSession.update("review.updateReviewLike", reviewNo);
	}
	
	public void updateReviewReviewLike(Long reviewNo, Long count) {
		Map<String, Object> params = new HashMap<>();
		params.put("reviewNo", reviewNo);
		params.put("count", count);
		sqlSession.update("review.updateReviewReviewLike", params);
	}

}