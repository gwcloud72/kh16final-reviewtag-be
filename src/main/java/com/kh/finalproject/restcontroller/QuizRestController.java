package com.kh.finalproject.restcontroller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dto.QuizDto;
import com.kh.finalproject.service.QuizService;
import com.kh.finalproject.vo.TokenVO;

@CrossOrigin
@RestController
@RequestMapping("/quiz")
public class QuizRestController {
	
	@Autowired
	private QuizService quizService;
	
	//퀴즈 등록
	//TokenVO로 사용자 조회 및 quiz_creator_id로 등록
	@PostMapping("/")
	public QuizDto insert(
			@RequestBody QuizDto quizDto,
			@RequestAttribute TokenVO tokenVO
			) {
		//토큰에서 memberId 추출 한 후 작성자 설정
		String loginId = tokenVO.getLoginId();
		quizDto.setQuizCreatorId(loginId);
		
		return quizService.registQuiz(quizDto);
	}
	
	//퀴즈 푸는 페이지
	//TokenVO로 사용자 조회
	@GetMapping("/game/{contentsId}")
	public List<QuizDto> game(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable long contentsId
			){
		String loginId = tokenVO.getLoginId();
		
		return quizService.getQuizGame(contentsId, loginId);
	}
	
	//해당 영화의 퀴즈 목록 조회 구문
	//관리자 페이지로 뺄 가능성 높음
	@GetMapping("/list/{contentsId}")
	public List<QuizDto> getList(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable long contentsId
			) {
		
		String loginLevel = tokenVO.getLoginLevel();
		
		return quizService.getQuizList(contentsId, loginLevel);
	}
	
	//퀴즈 상세정보 조회
	@GetMapping("/{quizId}")
	public QuizDto detail(@PathVariable long quizId,
			@RequestAttribute TokenVO tokenVO
			) {
		String loginId = tokenVO.getLoginId();
		String loginLevel = tokenVO.getLoginLevel();
		System.out.println("실행중"); 
		return quizService.getQuizDetail(quizId, loginId, loginLevel);
	}
	
	//퀴즈 수정
	//TokenVO로 사용자 조회
	@PatchMapping("/")
	public boolean update(
			@RequestAttribute TokenVO tokenVO,
			@RequestBody QuizDto quizDto
			) {
		String loginId = tokenVO.getLoginId();
		
		return quizService.editQuiz(quizDto, loginId);
	}
	
	//퀴즈 상태 변경(BLIND)
	@PatchMapping("/status")
	public boolean changeStatus(
			@RequestAttribute TokenVO tokenVO,
			@RequestBody QuizDto quizDto
			) {
		
		String loginId = tokenVO.getLoginId();
		String loginLevel = tokenVO.getLoginLevel();
		
		
		return quizService.changeQuizStatus(quizDto, loginId, loginLevel);
	}

	
	//퀴즈 삭제
	@DeleteMapping("/{quizId}")
	public boolean delete(
			@RequestAttribute TokenVO tokenVO,
			@PathVariable long quizId
			) {
		
		//TokenVO에서 loginId와 loginLevel 추출
		String loginId = tokenVO.getLoginId();
		String loginLevel = tokenVO.getLoginLevel();
		
		return quizService.deleteQuiz(quizId, loginId, loginLevel);
	}
	
	
}





