package com.kh.finalproject.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.kh.finalproject.dao.contents.GenreDao;
import com.kh.finalproject.dto.contents.ContentsGenreDto;
import com.kh.finalproject.dto.contents.TmdbGenreResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {
	
	@Autowired
    private WebClient webClient;
	@Autowired
	private GenreDao genreDao;

    /**
     * TMDB API에서 장르 목록을 가져와 CONTENTS_GENRE 테이블에 저장/갱신합니다.
     * 영화 장르와 TV 장르를 모두 호출하여 통합합니다.
     */
    public void fetchAndSaveAllGenres() {
        List<String> types = Arrays.asList("movie", "tv");
        
        // 영화와 TV 장르를 모두 호출하고 중복을 제거합니다.
        List<ContentsGenreDto> allGenres = types.stream()
            .flatMap(type -> fetchGenres(type).stream())
            .distinct() // 중복 장르 제거
            .collect(Collectors.toList());

        // MyBatis MERGE INTO를 사용하여 DB에 저장
        allGenres.forEach(genreDao::upsertGenre);
        
        System.out.println("장르 마스터 테이블(CONTENTS_GENRE) 업데이트 완료. 총 장르 수: " + allGenres.size());
    }

    private List<ContentsGenreDto> fetchGenres(String type) {
        String uri = String.format("/genre/%s/list?language=ko-KR", type);
        
        TmdbGenreResponseDto response = webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(TmdbGenreResponseDto.class)
            .block(); // 동기적으로 결과를 기다림

        return response != null ? response.getGenres() : List.of();
    }
}