package com.kh.finalproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.vo.MemberReviewListVO;

@Repository
public class MemberReviewDao {

	@Autowired
	private SqlSession sqlSession;
	
	public List<MemberReviewListVO> selectList(String loginId){
	 return sqlSession.selectList("memberReview.selectList",loginId);
	}
	public int countReview(String loginId) {
	    return sqlSession.selectOne("memberReview.countReview", loginId);
	}
}
