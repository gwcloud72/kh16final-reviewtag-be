package com.kh.finalproject.restcontroller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dao.MemberDao;
import com.kh.finalproject.dao.MemberQuizDao;
import com.kh.finalproject.dao.MemberReviewDao;
import com.kh.finalproject.dao.MemberTokenDao;
import com.kh.finalproject.dao.MemberWatchDao;
import com.kh.finalproject.dto.MemberDto;
import com.kh.finalproject.dto.MemberProfileDto;
import com.kh.finalproject.error.TargetNotfoundException;
import com.kh.finalproject.error.UnauthorizationException;
import com.kh.finalproject.service.PointService;
import com.kh.finalproject.service.TokenService;
import com.kh.finalproject.vo.MemberAddQuizListVO;
import com.kh.finalproject.vo.MemberLoginResponseVO;
import com.kh.finalproject.vo.MemberMypageResponseVO;
import com.kh.finalproject.vo.MemberPointVO;
import com.kh.finalproject.vo.MemberProfileResponseVO;
import com.kh.finalproject.vo.MemberQuizListVO;
import com.kh.finalproject.vo.MemberQuizRateVO;
import com.kh.finalproject.vo.MemberRefreshVO;
import com.kh.finalproject.vo.MemberReviewListVO;
import com.kh.finalproject.vo.MemberWatchListVO;
import com.kh.finalproject.vo.PageResponseVO;
import com.kh.finalproject.vo.PageVO;
import com.kh.finalproject.vo.TokenVO;

@CrossOrigin
@RestController
@RequestMapping("/member")
public class MemberRestController {

	@Autowired
	private MemberDao memberDao;
	@Autowired
	private MemberTokenDao memberTokenDao;
	@Autowired
	private TokenService tokenService;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private MemberReviewDao memberReviewDao;
	@Autowired
	private MemberWatchDao memberWatchDao;
	@Autowired
	private MemberQuizDao memberQuizDao;
	@Autowired
	private PointService pointService;	
	
	/// 회원가입
	@PostMapping("/")
	public void join(@RequestBody MemberDto memberDto) {
		memberDao.insert(memberDto);
	}
	// 회원가입 - 아이디 중복검사
	@GetMapping("/memberId/{memberId}")
	public boolean selectOne(@PathVariable String memberId) {
		MemberDto memberDto = memberDao.selectOne(memberId);
		return memberDto == null;
	}
	// 회원가입 - 닉네임 중복검사
	@GetMapping("/memberNickname/{memberNickname}")
	public boolean checkMemberNickname(@PathVariable String memberNickname) {
		MemberDto memberDto = memberDao.selectOneByMemberNickname(memberNickname);
		return memberDto == null;
	}
	
	//회원 조회
	@GetMapping("/")
	public List<MemberDto> selectList(){
		return memberDao.selectList();
	}
	
	@GetMapping("/profile/{memberId}")
	public MemberProfileResponseVO selectProfile(@PathVariable String memberId) {
		MemberProfileDto memberProfileDto =  memberDao.selectProfile(memberId);
		if(memberProfileDto == null) throw new TargetNotfoundException();
		
		MemberPointVO pointVO = pointService.getMyPointInfo(memberId);
		
		
		return MemberProfileResponseVO.builder()
                .profile(memberProfileDto)
                .point(pointVO)
                .build();
	}
	
	@GetMapping("/mypage/{loginId}")
    public MemberMypageResponseVO getMypageData(@PathVariable String loginId) {
        // 1. 기본 정보 조회
        MemberDto memberDto = memberDao.selectOne(loginId);
        if(memberDto == null) throw new TargetNotfoundException();

        // 2. 포인트 정보 조회
        MemberPointVO pointVO = pointService.getMyPointInfo(loginId);

        // 3. 각 항목별 카운트 조회
        int reviewCount = memberReviewDao.countReview(loginId);
        int wishCount = memberWatchDao.countWatchlist(loginId);
        int quizCount = memberQuizDao.countAnswerQuiz(loginId);

        // 4. 합쳐서 반환
        return MemberMypageResponseVO.builder()
                .member(memberDto)
                .point(pointVO)
                .reviewCount(reviewCount)
                .wishCount(wishCount)
                .quizCount(quizCount)
                .build();
    }
	
	
	
	//회원정보수정 (전체수정)
	@PutMapping("/{memberId}")
	public void edit(
	        @PathVariable String memberId,
	        @RequestBody MemberDto memberDto) {

	    MemberDto originDto = memberDao.selectOne(memberId);
	    if(originDto == null) throw new TargetNotfoundException();

	   
	    if(memberDto.getMemberBirth() != null)
	        originDto.setMemberBirth(memberDto.getMemberBirth());

	    if(memberDto.getMemberContact() != null)
	        originDto.setMemberContact(memberDto.getMemberContact());

	    if(memberDto.getMemberPost() != null)
	        originDto.setMemberPost(memberDto.getMemberPost());

	    if(memberDto.getMemberAddress1() != null)
	        originDto.setMemberAddress1(memberDto.getMemberAddress1());

	    if(memberDto.getMemberAddress2() != null)
	        originDto.setMemberAddress2(memberDto.getMemberAddress2());
	    memberDao.update(originDto);
	}
	
	@PutMapping("/password/{loginId}")
	public void editPassword(
			@PathVariable String loginId,
			@RequestBody MemberDto memberDto) {
		MemberDto originDto = memberDao.selectOne(loginId);
		if(originDto == null) throw new TargetNotfoundException();
		// 각 요소 입력
		originDto.setMemberPw(memberDto.getMemberPw());
		memberDao.updatePassword(originDto);
	}
	
	//신뢰도 갱신
	
	//로그인
	@PostMapping("/login")
	public MemberLoginResponseVO login(@RequestBody MemberDto memberDto) {
		MemberDto findDto = memberDao.selectOne(memberDto.getMemberId());
		if(findDto == null) throw new TargetNotfoundException("로그인 오류 - 존재하지 않는 계정");
		//비밀번호 검사
			boolean valid = passwordEncoder.matches(memberDto.getMemberPw(), findDto.getMemberPw());
			if(valid == false) {
				throw new TargetNotfoundException("비밀번호 불일치");
			}
			
		//로그인 성공
			return MemberLoginResponseVO.builder()
					.loginId(findDto.getMemberId())
					.loginLevel(findDto.getMemberLevel())
					.loginNickname(findDto.getMemberNickname())
					.accessToken(tokenService.generateAccessToken(findDto))
					.refreshToken(tokenService.generateRefreshToken(findDto))
				.build();
	}
	
	//로그아웃
	@DeleteMapping("/logout")
	public void logout(@RequestHeader("Authorization") String bearerToken) {
		TokenVO tokenVO = tokenService.parse(bearerToken);
		memberTokenDao.deleteByTarget(tokenVO.getLoginId());
	}
	
	//회원탈퇴
	@DeleteMapping("/{memberId}")
	public void delete(@PathVariable String memberId,
			@RequestHeader("Authorization") String bearerToken) {
		//계정 삭제
		MemberDto memberDto = memberDao.selectOne(memberId);
		if(memberDto ==null) throw new TargetNotfoundException("존재하지 않는 회원입니다");
		memberDao.delete(memberId);
		//토큰 삭제
		TokenVO tokenVO = tokenService.parse(bearerToken);
		memberTokenDao.deleteByTarget(tokenVO.getLoginId());
	}
	
	/// 토큰 갱신
	@PostMapping("/refresh")
	public MemberLoginResponseVO refresh(@RequestBody MemberRefreshVO memberRefreshVO) {
		String refreshToken = memberRefreshVO.getRefreshToken();
		if(refreshToken==null) throw new UnauthorizationException();
		TokenVO tokenVO = tokenService.parse(refreshToken);
		boolean valid = tokenService.checkRefreshToken(tokenVO,refreshToken);
		if(valid == false) throw new TargetNotfoundException();
		//재생성 후 반환
		return MemberLoginResponseVO.builder()
					.loginId(tokenVO.getLoginId())
					.loginLevel(tokenVO.getLoginLevel())
					.accessToken(tokenService.generateAccessToken(tokenVO))
					.refreshToken(tokenService.generateRefreshToken(tokenVO))
				.build();
	}
	//////////////////////////////////
	/// 마이페이지 조회용
	// 등록한 리뷰
	@GetMapping("/myreview/{loginId}")
	public List<MemberReviewListVO> selectReviewList(@PathVariable String loginId){
		return memberReviewDao.selectList(loginId);
	}
	// 찜한 콘텐츠 목록
	@GetMapping("/mywatch/{loginId}")
	public List<MemberWatchListVO> selectWatchList(@PathVariable String loginId){
		return memberWatchDao.selectList(loginId);
	}
	// 등록한 퀴즈 
//	@GetMapping("/myaddquiz/{loginId}")
//	public List<MemberAddQuizListVO> selectAddQuizList(@PathVariable String loginId){
//		return memberQuizDao.selectAddList(loginId);
//	}
	// 등록한 퀴즈 + 페이지네이션
	@GetMapping("/myaddquiz/{loginId}/{page}")
	public PageResponseVO<MemberAddQuizListVO> selectAddQuizListWithPage(
				@PathVariable String loginId, @PathVariable int page){
		int totalCount = memberQuizDao.countMyQuiz(loginId);
		PageVO pageVO = new PageVO();
		pageVO.setPage(page);
		pageVO.setTotalCount(totalCount);
		List<MemberAddQuizListVO> list =
			     memberQuizDao.selectMyQuizListWithPage(loginId, pageVO);
		return new PageResponseVO<>(list, pageVO);
	}
	// 내가 푼 퀴즈 목록
	@GetMapping("/myanswerquiz/{loginId}/{page}")
	public PageResponseVO<MemberQuizListVO> selectAnwserQuizList(
			@PathVariable String loginId, @PathVariable int page){
		int totalCount = memberQuizDao.countAnswerQuiz(loginId);
		PageVO pageVO = new PageVO();
		pageVO.setPage(page);
		pageVO.setTotalCount(totalCount);
		List<MemberQuizListVO> list =
			     memberQuizDao.selectAnswerListWithPage(loginId, pageVO);
		return new PageResponseVO<>(list, pageVO);
	}
	// 내가 푼 퀴즈 정답률
	@GetMapping("/myanswerRate/{loginId}")
	public List<MemberQuizRateVO> selectAnswerQuizRate(@PathVariable String loginId){
		return memberQuizDao.selectAnswerQuizRate(loginId);
	}


}
