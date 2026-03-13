package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.kh.finalproject.dto.IconDto;

@Repository
public class IconDao {

    @Autowired 
    private SqlSession sqlSession;

    // 아이콘 등록
    public int insert(IconDto iconDto) { 
        return sqlSession.insert("icon.insert", iconDto); 
    }

    // 아이콘 수정
    public boolean update(IconDto iconDto) { 
        return sqlSession.update("icon.update", iconDto) > 0; 
    }

    // 아이콘 삭제
    public boolean delete(int iconId) { 
        return sqlSession.delete("icon.delete", iconId) > 0; 
    }

    // 전체 아이콘 목록 조회
    public List<IconDto> selectList() { 
        return sqlSession.selectList("icon.selectList"); 
    }
    // 전체 아이콘 목록 조회
    public List<IconDto> selectListByContents(Long iconContents) { 
        return sqlSession.selectList("icon.selectListByContents",iconContents); 
    }
    
    public int countIcons(String category, String rarity) {
        Map<String, Object> params = new HashMap<>();
        params.put("category", category);
        params.put("rarity", rarity);
        return sqlSession.selectOne("icon.countIcons", params);
    }

    public List<IconDto> selectListPaging(int startRow, int endRow, String category, String rarity) {
        Map<String, Object> params = new HashMap<>();
        params.put("startRow", startRow);
        params.put("endRow", endRow);
        params.put("category", category);
        params.put("rarity", rarity);
        return sqlSession.selectList("icon.selectListPaging", params);
    }
}