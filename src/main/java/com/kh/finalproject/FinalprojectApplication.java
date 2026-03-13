package com.kh.finalproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

//Spring Security를 추가하면서 생기는 자동 설정이 이루어지지 않도록 제거 처리
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class FinalprojectApplication {
 
	public static void main(String[] args) {
		SpringApplication.run(FinalprojectApplication.class, args);
	}

}
