package com.kh.finalproject.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PointGetQuestDao {

    @Autowired
    private SqlSession sqlSession;
    // 예: <mapper namespace="pointGetQuest"> 로 설정했다고 가정
    private static final String NAMESPACE = "dailyquest."; 
    /**
     * 1. 오늘 날짜의 내 퀘스트 기록 조회
     * @param memberId 유저 ID
     * @param date 오늘 날짜 (YYYYMMDD)
     * @return 퀘스트 로그 리스트 (Map 형태)
     */
    public List<Map<String, Object>> selectTodayLogs(String memberId, String date) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("date", date);
        return sqlSession.selectList(NAMESPACE + "selectTodayLogs", params);
    }

    /**
     * 2. 퀘스트 진행도 업데이트 (없으면 생성, 있으면 +1)
     * @param memberId 유저 ID
     * @param type 퀘스트 타입 (Enum 이름)
     * @param date 오늘 날짜
     */
    public int upsertQuestLog(String memberId, String type, String date) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("type", type);
        params.put("date", date);
        return sqlSession.update(NAMESPACE + "upsertQuestLog", params);
    }
    /**
     * 3. 보상 수령 상태 변경 (N -> Y)
     * @param memberId 유저 ID
     * @param type 퀘스트 타입
     * @param date 오늘 날짜
     */
    public int updateRewardStatus(String memberId, String type, String date) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("type", type);
        params.put("date", date);
        return sqlSession.update(NAMESPACE + "updateRewardStatus", params);
    }
}