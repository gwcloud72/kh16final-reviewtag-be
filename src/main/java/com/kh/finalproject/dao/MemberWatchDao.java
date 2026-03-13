package com.kh.finalproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.vo.MemberWatchListVO;

@Repository
public class MemberWatchDao {

	@Autowired
	private SqlSession sqlSession;
	
	public List<MemberWatchListVO> selectList(String loginId){
	 return sqlSession.selectList("memberWatch.selectList",loginId);
	}
	public int countWatchlist(String loginId) {
	    return sqlSession.selectOne("memberWatch.countWatchlist", loginId);
	}
}
