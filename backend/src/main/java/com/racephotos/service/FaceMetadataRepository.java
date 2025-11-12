package com.racephotos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.rekognition.model.BoundingBox;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Repository
public class FaceMetadataRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public FaceMetadataRepository(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.table:}") String tableName
    ) {
        this.dynamoDbClient = Objects.requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.tableName = tableName;
    }

    public void saveFaceRecord(
            String collectionId,
            String faceId,
            String eventId,
            String bucket,
            String photoKey,
            String imageId,
            BoundingBox boundingBox,
            Float confidence
    ) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException("DynamoDB table name (aws.dynamodb.table) is not configured");
        }

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("faceId", AttributeValue.builder().s(faceId).build());
        item.put("collectionId", AttributeValue.builder().s(collectionId).build());
        item.put("eventId", AttributeValue.builder().s(eventId).build());
        item.put("bucket", AttributeValue.builder().s(bucket).build());
        item.put("photoKey", AttributeValue.builder().s(photoKey).build());
        item.put("indexedAt", AttributeValue.builder().s(Instant.now().toString()).build());

        if (imageId != null && !imageId.isBlank()) {
            item.put("imageId", AttributeValue.builder().s(imageId).build());
        }

        if (confidence != null) {
            item.put("confidence", AttributeValue.builder().n(formatNumber(confidence)).build());
        }

        Map<String, AttributeValue> bbox = buildBoundingBoxAttribute(boundingBox);
        if (!bbox.isEmpty()) {
            item.put("boundingBox", AttributeValue.builder().m(bbox).build());
        }

        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());
    }

    private static Map<String, AttributeValue> buildBoundingBoxAttribute(BoundingBox boundingBox) {
        Map<String, AttributeValue> bbox = new HashMap<>();
        if (boundingBox == null) {
            return bbox;
        }
        if (boundingBox.width() != null) {
            bbox.put("width", AttributeValue.builder().n(formatNumber(boundingBox.width())).build());
        }
        if (boundingBox.height() != null) {
            bbox.put("height", AttributeValue.builder().n(formatNumber(boundingBox.height())).build());
        }
        if (boundingBox.left() != null) {
            bbox.put("left", AttributeValue.builder().n(formatNumber(boundingBox.left())).build());
        }
        if (boundingBox.top() != null) {
            bbox.put("top", AttributeValue.builder().n(formatNumber(boundingBox.top())).build());
        }
        return bbox;
    }

    private static String formatNumber(Number number) {
        return String.format(Locale.US, "%.6f", number.doubleValue());
    }
}
