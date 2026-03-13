package com.kh.finalproject.restcontroller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.vo.PointRankingVO;

@RestController
@RequestMapping("/point/ranking")
@CrossOrigin
public class PointRankingRestController {
    
    @Autowired private SqlSession sqlSession;

    @GetMapping("/total")
    public Map<String, Object> getTotalRanking(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        // 1. 전체 개수 구하기
        Map<String, Object> param = new HashMap<>();
        param.put("keyword", keyword);
        int totalCount = sqlSession.selectOne("member.countTotalRanking", param);
        
        // 2. 범위 계산 (1페이지: 1~10, 2	페이지: 11~20)
        int start = (page - 1) * size + 1;
        int end = page * size;
        
        param.put("start", start);
        param.put("end", end);
        
        // 3. 리스트 조회
        List<PointRankingVO> list = sqlSession.selectList("member.totalPointRanking", param);
        
        // 4. 결과 묶어서 반환
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("count", totalCount);
        result.put("totalPage", (totalCount + size - 1) / size); // 전체 페이지 수 계산
        
        return result;
    }
}		