package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.kh.finalproject.dto.PointHistoryDto;

@Repository
public class PointHistoryDao {

    @Autowired
    private SqlSession sqlSession;

    /* 1. 등록 C */
    public int insert(PointHistoryDto pointHistoryDto) { 
        return sqlSession.insert("pointhistory.insert", pointHistoryDto);
    }
    

    /* 2. 수정 U */
    public boolean update(PointHistoryDto pointHistoryDto) {
        return sqlSession.update("pointhistory.update", pointHistoryDto) > 0;
    }

    /* 3.(1) 목록 출력 R */
    public List<PointHistoryDto> selectListByMemberId(String memberId) {
        return sqlSession.selectList("pointhistory.selectListByMemberId", memberId);
    }

    /* 3.(2) 번호 기준 조회 R (파라미터명 변경 No -> Id) */
    public PointHistoryDto selectOne(long pointHistoryId) {
        return sqlSession.selectOne("pointhistory.selectOne", pointHistoryId);
    }

    /* 4. 삭제 D */
    public boolean delete(long pointHistoryId) {
        return sqlSession.delete("pointhistory.delete", pointHistoryId) > 0;
    }
    
       /* 5. 전체 개수 조회 (필터 포함) */
    public int countHistory(String memberId, String type) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("type", type);
        
        return sqlSession.selectOne("pointhistory.countHistory", params);
    }

    /* 6. 페이징 목록 조회 (필터 포함) */
    public List<PointHistoryDto> selectListByMemberIdPaging(String memberId, int startRow, int endRow, String type) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("startRow", startRow);
        params.put("endRow", endRow);
        params.put("type", type);

        return sqlSession.selectList("pointhistory.selectListByMemberIdPaging", params);
    }

    public int countTodayPurchase(String memberId, String itemName) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("itemName", itemName);
        return sqlSession.selectOne("pointhistory.countTodayPurchase", params);
    }

}