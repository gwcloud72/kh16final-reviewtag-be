package com.kh.finalproject.configuration;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data 
@Component
@ConfigurationProperties(prefix = "daily-quest")
public class DailyQuestProperties {
    private List<QuestDetail> list; 
    @Data 
    public static class QuestDetail {
        private String type;
        private String title;
        private int target;
        private int reward;
    }
}