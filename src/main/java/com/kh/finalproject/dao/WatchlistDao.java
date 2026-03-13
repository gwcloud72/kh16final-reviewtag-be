package com.kh.finalproject.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.kh.finalproject.dto.WatchlistDto;
import com.kh.finalproject.vo.WatchlistChangeTypeVO;
import com.kh.finalproject.vo.WatchlistCheckVO;

@Repository
public class WatchlistDao {

	@Autowired
	private SqlSession sqlSession;
	
	// 등록
	public void insert(WatchlistDto watchlistDto) {
		sqlSession.insert("watchlist.insert", watchlistDto);
	}
	
	//삭제
	public boolean delete(Long watchlistContent, String watchlistMember) {
		WatchlistDto watchlistDto = new WatchlistDto();
		watchlistDto.setWatchlistContent(watchlistContent);
		watchlistDto.setWatchlistMember(watchlistMember);
		return sqlSession.delete("watchlist.delete", watchlistDto) > 0;
	}
	
	//조회(확인용)
	public WatchlistCheckVO check(WatchlistDto watchlistDto) {
		// memberId가 없으면 검사 x
		int count = sqlSession.selectOne("watchlist.check",watchlistDto);
		return WatchlistCheckVO.builder()
						.hasWatchlist(count>0)
					.build();
	}

//	//수정(타입 변환용)
	public WatchlistChangeTypeVO updateType(WatchlistDto watchlistDto) {
		WatchlistChangeTypeVO changeVO = new WatchlistChangeTypeVO();
		changeVO.setWatchlistMember(watchlistDto.getWatchlistMember());
		changeVO.setWatchlistContent(watchlistDto.getWatchlistContent());
		changeVO.setWatchlistType(watchlistDto.getWatchlistType());
		boolean result = sqlSession.update("watchlist.update",changeVO)>0;
		changeVO.setChangeResult(result);
		return changeVO;
	}
	
}
