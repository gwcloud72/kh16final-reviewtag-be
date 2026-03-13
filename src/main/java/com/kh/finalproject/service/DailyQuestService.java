package com.kh.finalproject.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.configuration.DailyQuestProperties;
import com.kh.finalproject.dao.DailyQuestDao; // [NEW] DAO 추가
import com.kh.finalproject.dao.PointGetQuestDao;
import com.kh.finalproject.vo.DailyQuestVO;
import com.kh.finalproject.vo.DailyQuizVO;

@Service
public class DailyQuestService {

    @Autowired private DailyQuestProperties questProps; 
    @Autowired private PointGetQuestDao questDao;       
    
    @Lazy

    @Autowired private PointService pointService;       // 포인트 지급 및 이력 관리
    @Autowired private DailyQuestDao quizDao;            // 퀴즈 DB 접근 (SqlSession 사용)



    private String getTodayStr() {
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    // 1. 퀘스트 목록 조회
    public List<DailyQuestVO> getQuestList(String memberId) {
        String today = getTodayStr();
        List<Map<String, Object>> logs = questDao.selectTodayLogs(memberId, today);
        
        Map<String, Map<String, Object>> logMap = logs.stream()
            .collect(Collectors.toMap(m -> (String) m.get("type"), m -> m));

        List<DailyQuestVO> result = new ArrayList<>();

        for (DailyQuestProperties.QuestDetail q : questProps.getList()) {
            Map<String, Object> log = logMap.get(q.getType());
            int current = (log != null) ? Integer.parseInt(String.valueOf(log.get("count"))) : 0;
            boolean claimed = (log != null) && "Y".equals(log.get("rewardYn"));
            boolean done = current >= q.getTarget();

            result.add(DailyQuestVO.builder()
                    .type(q.getType()).title(q.getTitle()).current(current).target(q.getTarget())
                    .reward(q.getReward()).done(done).claimed(claimed)
                    .desc(getDescByType(q.getType())).icon(getIconByType(q.getType())).action(getActionByType(q.getType()))
                    .build());
        }
        return result;
    }

    // 2. 랜덤 퀴즈 추출
    public DailyQuizVO getRandomQuiz(String memberId) {
        List<Map<String, Object>> logs = questDao.selectTodayLogs(memberId, getTodayStr());
        boolean alreadySolved = logs.stream().anyMatch(m -> "QUIZ".equals(m.get("type")));

        if (alreadySolved) return null; 
        return quizDao.getRandomQuiz();
    }

    // 3. 정답 검증
    @Transactional
    public boolean checkQuizAndProgress(String memberId, int quizNo, String userAnswer) {
        if (userAnswer == null) return false;
        String correctAnswer = quizDao.getAnswer(quizNo); 
        if (correctAnswer == null) return false;

        String cleanUser = userAnswer.replace(" ", "").toLowerCase();
        String cleanCorrect = correctAnswer.replace(" ", "").toLowerCase();

        if (cleanUser.contains(cleanCorrect)) {
            this.questProgress(memberId, "QUIZ");
            return true;
        }
        return false;
    }

    // 4. 퀘스트 진행도 상승
    @Transactional
    public void questProgress(String memberId, String type) {
        boolean isValid = questProps.getList().stream().anyMatch(q -> q.getType().equals(type));
        if(isValid) {
            questDao.upsertQuestLog(memberId, type, getTodayStr());
        }
    }

    //  5. 보상 수령
    @Transactional
    public int claimReward(String memberId, String type) {
        DailyQuestProperties.QuestDetail targetQuest = questProps.getList().stream()
                .filter(q -> q.getType().equals(type)).findFirst()
                .orElseThrow(() -> new RuntimeException("존재하지 않는 퀘스트입니다."));

        List<Map<String, Object>> logs = questDao.selectTodayLogs(memberId, getTodayStr());
        Map<String, Object> myLog = logs.stream().filter(m -> m.get("type").equals(type)).findFirst().orElse(null);

        if (myLog == null) throw new RuntimeException("기록 없음");
        int current = Integer.parseInt(String.valueOf(myLog.get("count")));
        if (current < targetQuest.getTarget()) throw new RuntimeException("목표 미달성");
        if ("Y".equals(myLog.get("rewardYn"))) throw new RuntimeException("이미 수령");

        if (questDao.updateRewardStatus(memberId, type, getTodayStr()) > 0) {
            // 사유(Reason) 추가: 예) "일일 퀘스트 보상: 오늘의 영화 퀴즈"
            pointService.addPoint(
                memberId, 
                targetQuest.getReward(), 
                "GET",
                "일일 퀘스트 보상: " + targetQuest.getTitle() 
            );
            return targetQuest.getReward();
        }
        return 0;
    }

    private String getIconByType(String type) {
        switch(type) {
            case "REVIEW": return "✍️"; case "QUIZ": return "🧠";
            case "LIKE": return "❤️"; case "ROULETTE": return "🎰"; default: return "❓";
        }
    }
    private String getDescByType(String type) {
        switch(type) {
            case "REVIEW": return "한줄평 남기기"; case "QUIZ": return "오늘의 영화 퀴즈";
            case "LIKE": return "좋아요 누르기"; case "ROULETTE": return "룰렛 돌리기"; default: return "일일 퀘스트";
        }
    }
    private String getActionByType(String type) {
        switch(type) {
            case "REVIEW": return "link"; case "QUIZ": return "quiz";
            case "LIKE": return "link"; case "ROULETTE": return "roulette"; default: return "none";
        }
    }
}