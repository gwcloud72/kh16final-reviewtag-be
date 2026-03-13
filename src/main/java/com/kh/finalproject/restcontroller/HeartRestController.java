package com.kh.finalproject.restcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dto.HeartDto;
import com.kh.finalproject.service.HeartService;
import com.kh.finalproject.vo.TokenVO;

@CrossOrigin
@RestController
@RequestMapping("/heart")
public class HeartRestController {
	
	@Autowired
	private HeartService heartService;
	
	//내 하트 정보 조회
	@GetMapping("/")
	public HeartDto getMyHeart(@RequestAttribute TokenVO tokenVO) {
		return heartService.getHeartInfo(tokenVO.getLoginId());
	}
	
	//하트 충전
	//추후 포인트 상점에서 구현
	@PostMapping("/charge")
	public void chargeHeart(
			@RequestAttribute TokenVO tokenVO, 
			@RequestParam int amount) {
		heartService.chargeHeart(tokenVO.getLoginId(), amount);
	}
}
