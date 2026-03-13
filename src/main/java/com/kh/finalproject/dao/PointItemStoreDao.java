package com.kh.finalproject.dao;

import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.kh.finalproject.dto.PointItemStoreDto;
import com.kh.finalproject.vo.AdminPointItemPageVO;

@Repository
public class PointItemStoreDao {
    
    @Autowired
    private SqlSession sqlSession;


    public int sequence() {
        return sqlSession.selectOne("pointitemstore.sequence");
    }

    public int insert(PointItemStoreDto pointItemDto) {
        return sqlSession.insert("pointitemstore.insert", pointItemDto);
    }

    public boolean update(PointItemStoreDto pointItemDto) {
        return sqlSession.update("pointitemstore.update", pointItemDto) > 0;
    }

    public List<PointItemStoreDto> selectList(AdminPointItemPageVO vo) {
        return sqlSession.selectList("pointitemstore.selectList", vo);
    }
  
    public int selectCount(AdminPointItemPageVO vo) {
        return sqlSession.selectOne("pointitemstore.selectCount", vo);
    }
  
    public PointItemStoreDto selectOneNumber(long pointItemNo) {
        return sqlSession.selectOne("pointitemstore.selectOneNumber", pointItemNo);
    }

    public boolean delete(long pointItemNo) { 
        return sqlSession.delete("pointitemstore.delete", pointItemNo) > 0;
    }
 // [추가] 관리자용 목록 조회 (페이징 + 필터링)
    public List<PointItemStoreDto> selectAdminList(AdminPointItemPageVO vo) {
        return sqlSession.selectList("pointitemstore.selectAdminList", vo);
    }

    // [추가] 조건에 맞는 데이터 전체 개수 (페이지네이션 계산용)
    public int selectAdminCount(AdminPointItemPageVO vo) {
        return sqlSession.selectOne("pointitemstore.selectPointCount", vo);
    }
    }