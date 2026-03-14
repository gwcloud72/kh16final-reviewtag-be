package com.kh.finalproject.aop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfiguration implements WebMvcConfigurer {

    @Autowired
    private TokenRenewalInterceptor tokenRenewalInterceptor;
    @Autowired
    private MemberInterceptor memberInterceptor;
    @Autowired
    private TokenParsingInterceptor tokenParsingInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    	// 회원 인증이 필요한 경로를 가로채는 인터셉터 등록
    	registry.addInterceptor(memberInterceptor)
    	    .addPathPatterns(
    	        "/member/*",                 // 회원 관련 기본 기능 보호
    	        "/member/mypage/**",         // 마이페이지
    	        "/member/password/**",       // 비밀번호 변경/관리
    	        "/member/myreview/**",       // 내가 작성한 리뷰
    	        "/member/mywatch/**",        // 내가 본 콘텐츠/찜 목록
    	        "/member/myaddquiz/**",      // 내가 등록한 퀴즈
    	        "/member/myanswerquiz/**",   // 내가 푼 퀴즈
    	        "/member/myanswerRate/**",   // 내 정답률/풀이 통계
    	        "/point/**",                 // 포인트 관련 기능
    	        "/content/**",               // 콘텐츠 관련 기능
    	        "/quiz/**",                  // 퀴즈 관련 기능
    	        "/admin/**",                 // 관리자 기능
    	        "/heart/**",                 // 좋아요/찜 기능
    	        "/review/",                  // 리뷰 메인 기능
    	        "/review/check",             // 리뷰 작성 가능 여부 체크
    	        "/review/action/**",         // 리뷰 등록/수정/삭제 등 액션 처리
    	        "/review/report/**",         // 리뷰 신고 기능
    	        "/board/**",                 // 게시판 기능
    	        "/reply/*",                  // 댓글/답글 기능
    	        "/board/report/**"           // 게시글 신고 기능
    	    )
    	    .excludePathPatterns(
    	        "/member/login",             // 로그인 페이지는 비회원도 접근 가능
    	        "/member/refresh",           // 토큰 재발급은 인증 예외
    	        "/member/memberId/**",       // 아이디 중복 확인
    	        "/member/memberNickname/**", // 닉네임 중복 확인
    	        "/member/profile/**",        // 프로필 이미지/조회 공개 경로
    	        "/point/main/store",         // 포인트 상점 메인 조회
    	        "/point/main/store/",        // 포인트 상점 메인 조회
    	        "/point/main/store/detail/**", // 포인트 상품 상세 조회
    	        "/ranking/**",               // 랭킹 조회
    	        "/quiz/log/list/ranking/**", // 퀴즈 랭킹 조회
    	        "/board",                    // 게시판 목록 조회
    	        "/board/",                   // 게시판 목록 조회
    	        "/board/page/**",            // 게시판 페이징 목록 조회
    	        "/board/contentsId/**",      // 콘텐츠별 게시글 조회
    	        "/board/detail/*",           // 게시글 상세 조회
    	        "/reply/"                    // 댓글 목록 조회
    	    );

    	// 모든 요청에 대해 토큰 자동 갱신 인터셉터 등록
    	registry.addInterceptor(tokenRenewalInterceptor)
    	    .addPathPatterns("/**") // 전체 경로 대상
    	    .excludePathPatterns(
    	        "/member/join",              // 회원가입은 토큰 갱신 제외
    	        "/member/login",             // 로그인은 토큰 갱신 제외
    	        "/member/logout",            // 로그아웃은 토큰 갱신 제외
    	        "/point/main/store",         // 포인트 상점 메인 조회는 제외
    	        "/point/main/store/",        // 포인트 상점 메인 조회는 제외
    	        "/point/main/store/detail/**", // 포인트 상품 상세 조회는 제외
    	        "/member/refresh"            // 재발급 요청 자체는 중복 처리 방지 위해 제외
    	    )
    	    .order(2); // 인터셉터 실행 순서 지정
    }
}