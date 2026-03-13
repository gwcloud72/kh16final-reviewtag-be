package com.kh.finalproject.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TmdbConfiguratoin {
	
	@Autowired
	private TmdbProperties tmdbProperties;
	
	@Bean
	public WebClient webClient() {//WebClient 준비
		return WebClient.builder()
				.baseUrl(tmdbProperties.getBaseUrl()) //기본(시작) 주소 설정
				.defaultHeader("Authorization", "Bearer " + tmdbProperties.getAccessToken()) //인증 토큰
				.defaultHeader("Accept", "application/json") //전송데이터 유형 설정
			.build();
	}
}
