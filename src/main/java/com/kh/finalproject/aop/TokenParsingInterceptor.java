package com.kh.finalproject.aop;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kh.finalproject.configuration.JwtProperties;
import com.kh.finalproject.error.UnauthorizationException;
import com.kh.finalproject.service.TokenService;
import com.kh.finalproject.vo.TokenVO;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class TokenParsingInterceptor implements HandlerInterceptor{

     // 조회수 확인을 위한 TOKEN 파싱해서 LOGINiD만 출력
	@Autowired
	private TokenService tokenService;
	
	public boolean preHandle(HttpServletRequest request,
										HttpServletResponse response, Object handler) throws Exception {

		if(request.getMethod().equalsIgnoreCase("options")) return true;

		String bearerToken = request.getHeader("Authorization");
		if(bearerToken == null) throw new UnauthorizationException();
		
		// 토큰 해석
		TokenVO tokenVO = tokenService.parse(bearerToken);
		request.setAttribute("tokenVO", tokenVO);
		
		// 정보 저장
		request.setAttribute("loginId", tokenVO.getLoginId()); 
		return true;
	}
}
