package com.kh.finalproject.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.kh.finalproject.dao.contents.ContentsDao;
import com.kh.finalproject.dao.contents.GenreDao;
import com.kh.finalproject.dto.contents.CastMemberDto;
import com.kh.finalproject.dto.contents.ContentsDetailDto;
import com.kh.finalproject.dto.contents.ContentsDto;
import com.kh.finalproject.dto.contents.DetailRuntimeDto;
import com.kh.finalproject.dto.contents.SearchResultDto;
import com.kh.finalproject.dto.contents.TmdbCreditsDto;
import com.kh.finalproject.vo.contents.ContentsGenreMapVO;
import com.kh.finalproject.vo.contents.ContentsVO;
import com.kh.finalproject.vo.contents.TmdbSearchResponseVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbApiService {
	
	@Autowired
	private WebClient webClient;
	@Autowired
	private ContentsDao contentsDao;
	@Autowired
	private GenreDao genreDao;

	
	public List<SearchResultDto> searchContents(String query) {
		   // '/search/multi' 엔드포인트를 사용하여 영화, TV를 모두 검색합니다.
        String uri = String.format("/search/multi?query=%s&include_adult=false&language=ko-KR&page=1", query);

        TmdbSearchResponseVO response = webClient.get()
                .uri(uri)
                .retrieve()
                // Generic Type을 처리하기 위해 ParameterizedTypeReference 사용
                .bodyToMono(TmdbSearchResponseVO.class)
                .block();

        if (response != null && response.getSearchResults() != null) {
            // 'movie' 또는 'tv' 타입만 필터링하여 반환합니다.
            return response.getSearchResults().stream()
                .filter(dto -> "movie".equals(dto.getType()) || "tv".equals(dto.getType()))
                .collect(Collectors.toList());
        }
        return List.of();
		}

    
	
    /**
     * 2. [선택 저장 기능] TMDB ID와 타입을 받아 상세 정보를 가져와 DB에 저장합니다.
     * REST Controller: POST /api/tmdb/save
     * @param contentsId TMDB 고유 ID
     * @param type 컨텐츠 타입 ("movie" 또는 "tv")
     * @return 장르 이름이 포함된 최종 저장/조회 결과 DTO
     */
    @Transactional
    public ContentsDetailDto saveContentByTmdbId(Long contentsId, String type) {
        
        // 1. 상세 조회 API URI 구성 및 호출 (ContentsDto에는 genreIds가 포함되어 있음)
        String uri = String.format("/%s/%d?language=ko-KR", type, contentsId);
        
        ContentsDto detailDto = webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(ContentsDto.class)
            .block();
        
        if (detailDto == null) {
            throw new RuntimeException("TMDB에서 컨텐츠 상세 정보를 찾을 수 없습니다: " + contentsId);
        }

        // 2. 런타임 정보 조회 및 DTO에 설정
        Integer runtime = fetchRuntimeFromDetailApi(contentsId, type);
        detailDto.setContentsRuntime(runtime);
        
        TmdbCreditsDto credits = fetchCredits(contentsId, type);
        if (credits != null) {
            detailDto.setContentsDirector(extractDirector(credits));
            detailDto.setContentsMainCast(extractMainCast(credits));
        }
        
        // 3. DB 저장 로직 실행
        saveContent(detailDto, type);
        
        // 4. DAO를 통해 저장된 컨텐츠 조회 (장르 이름 포함)
        return contentsDao.selectContentDetailWithGenres(contentsId);
    }
    
 // TMDB 크레딧 API 호출 (감독/배우 정보)
    private TmdbCreditsDto fetchCredits(Long contentsId, String type) {
        String uri = String.format("/%s/%d/credits?language=ko-KR", type, contentsId);
        
        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(TmdbCreditsDto.class)
            .block();
    }

    // 크레딧 DTO에서 감독 이름 추출
    private String extractDirector(TmdbCreditsDto credits) {
        if (credits.getCrew() == null) return null;
        
        return credits.getCrew().stream()
            .filter(c -> "Director".equalsIgnoreCase(c.getJob()))
            .map(c -> c.getName())
            .findFirst()
            .orElse(null);
    }
    
    // 크레딧 DTO에서 주연 배우 이름 3명 추출
    private String extractMainCast(TmdbCreditsDto credits) {
        if (credits.getCast() == null) return null;

        List<String> mainCastList = credits.getCast().stream()
            // order 순으로 정렬 (null은 마지막에 위치)
            .sorted(Comparator.comparing(CastMemberDto::getOrder, Comparator.nullsLast(Integer::compareTo)))
            .limit(3) 
            .map(c -> c.getName())
            .collect(Collectors.toList());

        return String.join(", ", mainCastList); // "배우1, 배우2, 배우3" 형태로 반환
    }

    // 개별 컨텐츠 저장 및 매핑 테이블 업데이트 로직
    private void saveContent(ContentsDto dto, String type) {
    	System.out.println("장르 ID 리스트: " + dto.getGenres());
        // 1. 런타임 정보 조회 (N+1 호출)
        Integer runtime = fetchRuntimeFromDetailApi(dto.getContentsId(), type);
        dto.setContentsRuntime(runtime != null ? runtime : 0); // null 방지

        // 2. ContentsDto -> ContentsVO 변환 (DB 저장용)
        ContentsVO contentVo = new ContentsVO();
        contentVo.setContentsId(dto.getContentsId());
        contentVo.setContentsTitle(dto.getContentsTitle());
        contentVo.setContentsType(type); // API 경로 기반 타입 주입
        contentVo.setContentsOverview(dto.getContentsOverview());
        contentVo.setContentsPosterPath(dto.getContentsPosterPath());
        contentVo.setContentsBackdropPath(dto.getContentsBackdropPath());
        contentVo.setContentsVoteAverage(dto.getContentsVoteAverage());
        contentVo.setContentsReleaseDate(dto.getContentsReleaseDate());
        contentVo.setContentsRuntime(dto.getContentsRuntime());
        contentVo.setContentsDirector(dto.getContentsDirector()); 
        contentVo.setContentsMainCast(dto.getContentsMainCast());
        
        // 3. CONTENTS 테이블에 저장/갱신
        contentsDao.upsertContent(contentVo);

        // 4. 장르 매핑 업데이트를 위해 기존 매핑 삭제 후 새로 저장
        genreDao.deleteGenreMapping(dto.getContentsId());
        
        if (dto.getGenres() != null) {
            dto.getGenres().forEach(genreDto -> {
                ContentsGenreMapVO mapVo = new ContentsGenreMapVO();
                mapVo.setContentsId(dto.getContentsId());
                mapVo.setGenreId(genreDto.getGenreId() != null ? genreDto.getGenreId().intValue() : null);
                genreDao.upsertGenreMap(mapVo);
            });
        }
    }
    
    // 개별 컨텐츠 상세 조회 API를 호출하여 런타임 정보만 가져옴
    private Integer fetchRuntimeFromDetailApi(Long contentsId, String type) {
        String uri = String.format("/%s/%d?language=ko-KR", type, contentsId);
        
        DetailRuntimeDto detailResponse = webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(DetailRuntimeDto.class)
            .block();
            
        return detailResponse != null ? detailResponse.getSingleRuntime() : null;
    }
    
    /**
     * DB에 저장된 컨텐츠 상세 정보와 장르 리스트를 조회합니다.
     */
    public ContentsDetailDto getContentDetailWithGenres(Long contentsId) {
        return contentsDao.selectContentDetailWithGenres(contentsId);
    }
	
}
