package com.racephotos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.racephotos.auth.config.CognitoProperties;

@SpringBootApplication
@EnableConfigurationProperties(CognitoProperties.class)
public class RacePhotosApplication {
    public static void main(String[] args) {
        SpringApplication.run(RacePhotosApplication.class, args);
    }
}
