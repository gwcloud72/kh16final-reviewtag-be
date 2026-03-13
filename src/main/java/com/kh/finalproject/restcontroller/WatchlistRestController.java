package com.kh.finalproject.restcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dao.WatchlistDao;
import com.kh.finalproject.dto.WatchlistDto;
import com.kh.finalproject.vo.WatchlistChangeTypeVO;
import com.kh.finalproject.vo.WatchlistCheckVO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/watchlist")
public class WatchlistRestController {

	@Autowired
	private WatchlistDao watchlistDao;
	
	
	// 북마크 등록
	@PostMapping("/")
	public void insert(@RequestBody WatchlistDto watchlistDto) {
		watchlistDao.insert(watchlistDto);
	}
	
	// 북마크 삭제
	@DeleteMapping("/{contentsId}/{loginId}")
	public void delete(@PathVariable long contentsId,
							   @PathVariable String loginId) {
		watchlistDao.delete(contentsId, loginId);
	}
	
	//북마크 조회(확인용)
	@PostMapping("/check")
	public WatchlistCheckVO check(@RequestBody WatchlistDto watchlistDto) {
		return watchlistDao.check(watchlistDto);
	}
	
	//북마크 수정
	@PutMapping("/")
	public WatchlistChangeTypeVO ChangeType(@RequestBody WatchlistDto watchlistDto) {
		return watchlistDao.updateType(watchlistDto);
	}
}
