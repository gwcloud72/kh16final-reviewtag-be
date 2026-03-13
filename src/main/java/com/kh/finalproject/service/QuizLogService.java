package com.kh.finalproject.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.dao.QuizDao;
import com.kh.finalproject.dao.QuizLogDao;
import com.kh.finalproject.dto.QuizLogDto;
import com.kh.finalproject.error.NeedPermissionException;
import com.kh.finalproject.error.TargetNotfoundException;
import com.kh.finalproject.vo.QuizMyStatsVO;
import com.kh.finalproject.vo.RankVO;

@Service
public class QuizLogService {

	@Autowired
	private QuizLogDao quizLogDao;
	
	@Autowired
	private QuizDao quizDao;
	
	@Autowired
	private PointService pointService;
	
	@Autowired
	private HeartService heartService;
	
	
	@Transactional
	//퀴즈 기록 등록
	public int submitQuizSession(List<QuizLogDto> logList, String memberId) {
		
		//로그인 검사
		if(memberId == null) throw new NeedPermissionException("로그인이 필요합니다.");
		
		//하트 차감
		heartService.useHeartForQuiz(memberId);
		
		int correctCount = 0;
		
		for(QuizLogDto log : logList) {
			
			//퀴즈 풀이 이용자 등록
			log.setQuizLogMemberId(memberId);
			
			//퀴즈 기록을 개별로 저장
			quizLogDao.insert(log);
			
			//퀴즈 이용자수 증가
			quizDao.increaseSolveCount(log.getQuizLogQuizId());
			
			//정답 개수 카운트
			if("Y".equals(log.getQuizLogIsCorrect())) {
				correctCount++;
			}
		}
		int getPoint = 20; // 정답별 획득 포인트배율
		int totalEarned = correctCount * getPoint;
		
		//correctCount에 따라 포인트 지급 로직 호출 구현 예정
		if (totalEarned > 0) {
            pointService.addPoint(
                memberId, 
                totalEarned, 
                "GET", 
                "영화 퀴즈 정답 보상 (" + correctCount + "개 정답)"
            );
        }
		
		return correctCount; //정답 개수를 int로 반환
	}
	
	//퀴즈 기록 상세 정보 조회
	//오답노트 느낌?
	public QuizLogDto quizLogDetail(long quizLogId, String requesterId, String requesterLevel) {
		
		//퀴즈 이력 조회
		QuizLogDto log = quizLogDao.selectOne(quizLogId);
		if(log == null) throw new TargetNotfoundException("존재하지 않는 기록입니다.");
		
		//로그인 확인
		if(requesterId == null) throw new NeedPermissionException("로그인이 필요합니다.");
		
		//본인 확인 및 관리자 확인 로직
		boolean isOwner = log.getQuizLogMemberId().equals(requesterId);
	    boolean isAdmin = "관리자".equals(requesterLevel);
		
	    if (!isOwner && !isAdmin) throw new NeedPermissionException();
	    
		return log;
	}
	
	//마이페이지 조회
	public List<QuizLogDto> myQuizLogList(String memberId){
		
		//로그인 검사
		if(memberId == null) throw new NeedPermissionException("로그인이 필요합니다.");
		
		return quizLogDao.selectListByMember(memberId);
	}
	
	//내 랭킹 조회
	//추후 일정 랭킹이면 포인트 지급 구현 예정
	public int getMyScore(String memberId) {
		
		//로그인 검사
		if(memberId == null) throw new NeedPermissionException("로그인이 필요합니다.");
		
		return quizLogDao.countCorrectAnswer(memberId);
	}
	
	//랭킹 통계 조회
	public QuizMyStatsVO getMyStats(int contentsId, String memberId) {
		return quizLogDao.getMyStats(contentsId, memberId);
	}
	
	//영화별 TOP랭킹 20위 가져오기
	public List<RankVO> getRanking(int contentsId) {
		return quizLogDao.getRanking(contentsId);
	}
	
	//어떤 사람이 해당 컨텐츠의 문제를 풀었는지 조회
	public List<QuizLogDto> quizLogList(long quizLogQuizId, String memberLevel){
		// 관리자 권한 체크
				if(!"관리자".equals(memberLevel))
					throw new NeedPermissionException();
		
		return quizLogDao.selectList(quizLogQuizId);
	}
}