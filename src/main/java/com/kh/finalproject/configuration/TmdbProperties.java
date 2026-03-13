package com.kh.finalproject.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "custom.tmdb")
public class TmdbProperties {
	private String key;
	private String accessToken;
	private String baseUrl;
}
