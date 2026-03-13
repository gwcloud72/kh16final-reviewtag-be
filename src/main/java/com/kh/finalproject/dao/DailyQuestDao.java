package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.vo.DailyQuizVO;

@Repository
public class DailyQuestDao {

    @Autowired
    private SqlSession sqlSession;

    // 1. 랜덤 퀴즈 가져오기
    public DailyQuizVO getRandomQuiz() {
        // namespace.id 형식으로 호출
        return sqlSession.selectOne("dailyquiz.getRandomQuiz");
    }

    // 2. 정답 가져오기
    public String getAnswer(int quizNo) {
        return sqlSession.selectOne("dailyquiz.getAnswer", quizNo);
    }
 // [관리자] 목록
    public List<DailyQuizVO> selectList(int startRow, int endRow, String type, String keyword) {
        Map<String, Object> param = new HashMap<>();
        param.put("startRow", startRow);
        param.put("endRow", endRow);
        param.put("type", type);       // 검색 타입
        param.put("keyword", keyword); // 검색어
        return sqlSession.selectList("dailyquiz.selectList", param);
    }
    public int count(String type, String keyword) {
        Map<String, Object> param = new HashMap<>();
        param.put("type", type);
        param.put("keyword", keyword);
        return sqlSession.selectOne("dailyquiz.count", param);
    }
    public void insert(DailyQuizVO vo) {
        sqlSession.insert("dailyquiz.insert", vo);
    }
    public boolean update(DailyQuizVO vo) {
        return sqlSession.update("dailyquiz.update", vo) > 0;
    }
    public boolean delete(int quizNo) {
        return sqlSession.delete("dailyquiz.delete", quizNo) > 0;
    }
}