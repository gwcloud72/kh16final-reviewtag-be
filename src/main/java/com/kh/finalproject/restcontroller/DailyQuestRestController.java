package com.kh.finalproject.restcontroller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.kh.finalproject.service.DailyQuestService;
import com.kh.finalproject.vo.DailyQuestVO;
import com.kh.finalproject.vo.DailyQuizVO;

@RestController
@RequestMapping("/point/quest")
@CrossOrigin
public class DailyQuestRestController {

    @Autowired private DailyQuestService dailyQuestService;

    // 1. 퀘스트 목록 조회
    @GetMapping("/list")
    public List<DailyQuestVO> list(@RequestAttribute(value="loginId", required=false) String loginId) {
        if(loginId == null) return List.of();
        return dailyQuestService.getQuestList(loginId);
    }
    // 2. 랜덤 퀴즈 문제 요청
    @GetMapping("/quiz/random")
    public DailyQuizVO getRandomQuiz(@RequestAttribute("loginId") String loginId) {
        // 이미 풀었다면 서비스에서 null을 리턴함 -> 프론트에서 null 체크 필요
        return dailyQuestService.getRandomQuiz(loginId);
    }
    //3. 퀴즈 정답 제출
    @PostMapping("/quiz/check")
    public String checkQuiz(@RequestAttribute("loginId") String loginId, @RequestBody Map<String, Object> body) {
        if (body.get("quizNo") == null) {
            System.out.println("오류: 프론트엔드에서 quizNo가 오지 않았습니다.");
            return "fail:quizNo is null";
        }

        try {
            int quizNo = Integer.parseInt(String.valueOf(body.get("quizNo")));
            String userAnswer = (String) body.get("answer");
            
            boolean isCorrect = dailyQuestService.checkQuizAndProgress(loginId, quizNo, userAnswer);
            
            return isCorrect ? "success" : "fail";
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "fail:invalid number format";
        }
    }
    // 4. 보상 받기
    @PostMapping("/claim")
    public String claim(@RequestAttribute("loginId") String loginId, @RequestBody Map<String, String> body) {
        try {
            String type = body.get("type");
            int reward = dailyQuestService.claimReward(loginId, type);
            return "success:" + reward;
        } catch (Exception e) {
            return "fail:" + e.getMessage();
        }
    }
}