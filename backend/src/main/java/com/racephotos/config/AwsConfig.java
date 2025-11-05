package com.racephotos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.RekognitionClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.util.Optional;

@Configuration
public class AwsConfig {

    @Bean
    public Region awsRegion(@Value("${aws.region:us-east-1}") String region) {
        return Region.of(region);
    }

    @Bean
    public S3Client s3Client(
            Region region,
            @Value("${aws.s3.endpoint:}") String s3Endpoint,
            @Value("${aws.s3.path-style-enabled:false}") boolean pathStyleEnabled
    ) {
        S3ClientBuilder builder = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleEnabled)
                        .build());

        Optional.ofNullable(s3Endpoint)
                .filter(v -> !v.isBlank())
                .map(URI::create)
                .ifPresent(builder::endpointOverride);

        return builder.build();
    }

    @Bean
    public RekognitionClient rekognitionClient(
            Region region,
            @Value("${aws.rekognition.endpoint:}") String rekEndpoint
    ) {
        RekognitionClientBuilder builder = RekognitionClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create());

        Optional.ofNullable(rekEndpoint)
                .filter(v -> !v.isBlank())
                .map(URI::create)
                .ifPresent(builder::endpointOverride);

        return builder.build();
    }
}
