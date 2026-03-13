package com.kh.finalproject.restcontroller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kh.finalproject.dao.contents.ContentsDao;
import com.kh.finalproject.dao.contents.GenreDao;
import com.kh.finalproject.dto.contents.ContentsDetailDto;
import com.kh.finalproject.dto.contents.GenreDto;
import com.kh.finalproject.dto.contents.SearchResultDto;
import com.kh.finalproject.service.GenreService;
import com.kh.finalproject.service.TmdbApiService;
import com.kh.finalproject.vo.contents.SaveRequestVO;

import lombok.RequiredArgsConstructor;

@CrossOrigin
@RestController
@RequestMapping("/api/tmdb")
@RequiredArgsConstructor
public class TmdbDataRestController {

		@Autowired
		private GenreService genreService;
		@Autowired
		private TmdbApiService tmdbApiService;
		@Autowired
		private GenreDao genreDao;
		@Autowired
		private ContentsDao contentsDao;

		
		//DB에 장르 마스터 데이터를 초기 저장합니다. //1회만 실시
	    @GetMapping("/genre/collect")
	    public String collectGenres() {
	        genreService.fetchAndSaveAllGenres();
	        return "장르 마스터 데이터 수집 및 저장 완료";
	    }
	    
		
		// 검색어로 api 조회 & 검색 결과 list 반환 
		@GetMapping("/search")
		public ResponseEntity<List<SearchResultDto>> searchContents(@RequestParam String query) {
			 // 검색어 유효성 검증
	        if (query == null || query.trim().isEmpty()) {
	            return ResponseEntity.badRequest().build(); // 400 Bad Request
	        }
	        
	        // Service의 검색 로직 호출
	        List<SearchResultDto> results = tmdbApiService.searchContents(query);
	        return ResponseEntity.ok(results); // 200 OK와 함께 JSON 데이터 반환
		}
		
		
		// 선택한 api 데이터를 DB에 저장 후 반환
	    @PostMapping("/save")
	    public ResponseEntity<ContentsDetailDto> saveSelectedContent(@RequestBody SaveRequestVO requestVO) {
	        // 요청 데이터 유효성 검증
	        if (requestVO.getContentsId() == null || requestVO.getType() == null || 
	            (!requestVO.getType().equals("movie") && !requestVO.getType().equals("tv"))) {
	            return ResponseEntity.badRequest().build(); // 400 Bad Request
	        }
	        
	        try {
	            // Service의 저장 로직 호출 (상세 조회 -> DB 저장 -> 최종 DTO 반환)
	            ContentsDetailDto savedContent = tmdbApiService.saveContentByTmdbId(
	                requestVO.getContentsId(),
	                requestVO.getType()
	            );
	            // 저장 성공 시 200 OK와 함께 저장된 객체 (JSON) 반환
	            return ResponseEntity.ok(savedContent);
	            
	        } catch (RuntimeException e) {
	            System.err.println("컨텐츠 저장 실패: " + e.getMessage());
	            // 500 Internal Server Error 반환
	            return ResponseEntity.internalServerError().build();
	        }
	    }
	    
	    
	    //컨텐츠 상세
	    @GetMapping("/contents/detail/{contentsId}")
	    public ContentsDetailDto getContentDetail(@PathVariable Long contentsId) {
	        ContentsDetailDto contentsDetailDto =  tmdbApiService.getContentDetailWithGenres(contentsId);
	        Long contentsLike = contentsDao.selectContentsLike(contentsId);
	        contentsDetailDto.setContentsLike(contentsLike);
	        return contentsDetailDto;
	    }
	    
	    
	    //장르 목록
	    @GetMapping("/genre")
	    public List<GenreDto> genreList() {
	    	return genreDao.selectGenre();
	    }
	    
 	    
	    //장르별 리스트
	    @GetMapping("/contents/list/{genreName}")
	    public List<ContentsDetailDto> selectListByGenre(@PathVariable(required = false) String genreName, 
	            @RequestParam(defaultValue = "1") Integer page) {
	    	int size = 12; // 한 페이지당 보여줄 개수 
	        
	        // Oracle 페이징 계산 (1페이지: 1~12, 2페이지: 13~24 ...)
	        int end = page * size;
	        int start = end - (size - 1);
	        
	        Map<String, Object> params = new HashMap<>();
	        params.put("start", start);
	        params.put("end", end);
	        
	        if(genreName == null || genreName.equals("all") || genreName.equals("전체")) {
		        List<ContentsDetailDto> list = contentsDao.selectContentsList(params);
		        return list;
	        }

	    
	        params.put("genreName", genreName);

	    	List<ContentsDetailDto> list = contentsDao.selectListByGenre(params);
	    	return list;
	    }
	    
	    //타입 별 리스트
	    //tv
	    @GetMapping("/contents/list/tv")
	    public List<ContentsDetailDto> selectTvList() {
	    	
	        Map<String, Object> params = new HashMap<>();
	        params.put("type", "tv");

	        
	        List<ContentsDetailDto> list = contentsDao.selectContentsListByType(params);
	        return list;
	    }
	    //movie
	    @GetMapping("/contents/list/movie")
	    public List<ContentsDetailDto> selectMovieList() {
	    	
	        Map<String, Object> params = new HashMap<>();
	        params.put("type", "movie");

	        
	        List<ContentsDetailDto> list = contentsDao.selectContentsListByType(params);
	        return list;
	    }
	    
	    //랭킹 리스트
	    //별점
	    @GetMapping("/contents/rank/rate")
	    public List<ContentsDetailDto> selectRateRank() {
	    	return contentsDao.selectContentsListByRateRank();
	    }
	    //가격
	    @GetMapping("/contents/rank/price")
	    public List<ContentsDetailDto> selectPriceRank() {
	    	return contentsDao.selectContentsListByPriceRank();
	    }
	    
	    //id로 제목 조회(for board)
	    @GetMapping("/title/{boardContentsId}")
	    public String selectTitleById(@PathVariable Long boardContentsId) {
	    	return contentsDao.selectTitleById(boardContentsId);
	    }
}
