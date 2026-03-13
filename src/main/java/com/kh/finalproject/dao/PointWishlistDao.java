package com.kh.finalproject.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.PointWishlistDto;
import com.kh.finalproject.vo.PointItemWishVO;

@Repository
public class PointWishlistDao {

    @Autowired
    private SqlSession sqlSession;

    // 찜 추가
    public void insert(PointItemWishVO vo) {
        sqlSession.insert("pointWishlist.insert", vo);
    }

    // 찜 삭제
    public void delete(PointItemWishVO vo) {
        sqlSession.delete("pointWishlist.delete", vo);
    }

    // 찜 여부 확인 (1이면 찜한 상태, 0이면 아님)
    public int checkWish(PointItemWishVO vo) {
        return sqlSession.selectOne("pointWishlist.checkWish", vo);
    }

    // 내가 찜한 아이템 번호들만 조회 (프론트엔드 하트 표시용)
    public List<Long> selectMyWishItemNos(String memberId) {
        return sqlSession.selectList("pointWishlist.selectMyWishItemNos", memberId);
    }

    // 내 찜 목록 전체 조회 (상세 정보 포함)
    public List<PointWishlistDto> selectMyWishlist(String memberId) {
        return sqlSession.selectList("pointWishlist.selectMyWishlist", memberId);
    }
}