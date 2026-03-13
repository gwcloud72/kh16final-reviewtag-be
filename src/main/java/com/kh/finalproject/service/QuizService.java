package com.kh.finalproject.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kh.finalproject.dao.QuizDao;
import com.kh.finalproject.dto.QuizDto;
import com.kh.finalproject.error.NeedPermissionException;
import com.kh.finalproject.error.TargetNotfoundException;

@Service
public class QuizService {
	
	@Autowired
	private QuizDao quizDao;
	
	@Autowired
	private HeartService heartService;
	
	//퀴즈 등록
	@Transactional
	public QuizDto registQuiz(QuizDto quizDto) {
        return quizDao.insert(quizDto);
    }
	
	//랜덤 5문제 출제
	@Transactional
	public List<QuizDto> getQuizGame(long contentsId, String memberId) {
		
		//로그인 상태 검사
		if(memberId == null) throw new NeedPermissionException("로그인이 필요합니다.");
		
		//하트 보유 여부 확인
		heartService.checkHeart(memberId);
		
		//문제 출제
        return quizDao.selectRandomQuizList(contentsId, memberId);
    }
	
	//퀴즈 삭제 (상태만 변경)
    public boolean deleteQuiz(long quizId, String memberId, String memberLevel) {
    	//퀴즈 조회
    	QuizDto origin = quizDao.selectOne(quizId);
		if(origin == null) throw new TargetNotfoundException();
		
		//권한 체크 (작성자 OR 관리자)
		boolean isOwner = origin.getQuizCreatorId().equals(memberId);
		boolean isAdmin = "관리자".equals(memberLevel);
    	
		if(!isOwner && !isAdmin) throw new NeedPermissionException();
		
    	//DELETED로 상태 변경(이게 삭제 상태)
    	QuizDto quizDto = QuizDto.builder()
    	        	.quizId(quizId)
    	        	.quizStatus("DELETED")
    	        .build();
        
        return quizDao.updateQuizStatus(quizDto);
    }
    
    //퀴즈 수정
    public boolean editQuiz(QuizDto quizDto, String memberId) {
    	
    	//퀴즈 조회
    	QuizDto origin = quizDao.selectOne(quizDto.getQuizId());
		if(origin == null) throw new TargetNotfoundException();
    	
		//퀴즈 작성자 비교
		if(!origin.getQuizCreatorId().equals(memberId)) 
			throw new NeedPermissionException("본인의 퀴즈만 수정할 수 있습니다.");
	
        return quizDao.update(quizDto);
    }
    
    //관리자용 목록 조회
    public List<QuizDto> getQuizList(long contentsId, String memberLevel) {
    	
    	//로그인한 계정의 등급 이 관리자인지 검사
    	if(!"관리자".equals(memberLevel)) throw new NeedPermissionException("관리자만 접근 가능합니다.");
    	
    	return quizDao.selectByContent(contentsId);
    }
    
    //상세 조회
    public QuizDto getQuizDetail(long quizId, String loginId, String loginLevel) {
    	
    	//퀴즈 조회
    	QuizDto findDto = quizDao.selectOne(quizId);
		if(findDto == null) throw new TargetNotfoundException("존재하지 않는 퀴즈입니다.");
    	
		boolean isOwner = findDto.getQuizCreatorId().equals(loginId);
		boolean isAdmin = "관리자".equals(loginLevel);
    	if(!isOwner && !isAdmin) throw new NeedPermissionException();
		
    	return findDto;
    }
    
    //상태 변경 메소드(관리자용)
    public boolean changeQuizStatus(QuizDto quizDto, String memberId, String memberLevel) {
    	
    	//퀴즈 조회
    	QuizDto origin = quizDao.selectOne(quizDto.getQuizId());
		if(origin == null) throw new TargetNotfoundException();
    	
		//권한 검사
		boolean isOwner = origin.getQuizCreatorId().equals(memberId);
		boolean isAdmin = "관리자".equals(memberLevel);
    	if(!isOwner && !isAdmin) throw new NeedPermissionException();
		
        return quizDao.updateQuizStatus(quizDto);
    }
    
    //퀴즈 누적 신고 누적 횟수 변경
    public boolean reportQuiz(long quizId) {
        return quizDao.increaseQuizReportCount(quizId);
    }
    
    
}
