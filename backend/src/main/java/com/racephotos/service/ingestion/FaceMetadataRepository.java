package com.racephotos.service.ingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.rekognition.model.BoundingBox;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    public Optional<FaceMetadataRecord> findByFaceId(String faceId) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException("DynamoDB table name (aws.dynamodb.table) is not configured");
        }
        if (faceId == null || faceId.isBlank()) {
            return Optional.empty();
        }

        Map<String, AttributeValue> key = Map.of(
                "faceId", AttributeValue.builder().s(faceId).build()
        );

        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                        .tableName(tableName)
                        .key(key)
                        .build())
                .item();

        if (item == null || item.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapItem(item));
    }

    private static FaceMetadataRecord mapItem(Map<String, AttributeValue> item) {
        String faceId = getString(item, "faceId");
        String collectionId = getString(item, "collectionId");
        String eventId = getString(item, "eventId");
        String bucket = getString(item, "bucket");
        String photoKey = getString(item, "photoKey");
        String imageId = getString(item, "imageId");
        Float confidence = getNumber(item, "confidence");
        BoundingBox boundingBox = parseBoundingBox(item.get("boundingBox"));

        return new FaceMetadataRecord(faceId, collectionId, eventId, bucket, photoKey, imageId, boundingBox, confidence);
    }

    private static BoundingBox parseBoundingBox(AttributeValue bboxAttr) {
        if (bboxAttr == null || bboxAttr.m() == null || bboxAttr.m().isEmpty()) {
            return null;
        }
        Map<String, AttributeValue> bbox = bboxAttr.m();
        BoundingBox.Builder builder = BoundingBox.builder();
        Float width = getNumber(bbox, "width");
        Float height = getNumber(bbox, "height");
        Float left = getNumber(bbox, "left");
        Float top = getNumber(bbox, "top");

        if (width != null) builder.width(width);
        if (height != null) builder.height(height);
        if (left != null) builder.left(left);
        if (top != null) builder.top(top);

        return builder.build();
    }

    private static String getString(Map<String, AttributeValue> item, String key) {
        AttributeValue attr = item.get(key);
        return attr == null || attr.s() == null ? null : attr.s();
    }

    private static Float getNumber(Map<String, AttributeValue> item, String key) {
        AttributeValue attr = item.get(key);
        if (attr == null || attr.n() == null || attr.n().isBlank()) {
            return null;
        }
        return Float.parseFloat(attr.n());
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

    public record FaceMetadataRecord(
            String faceId,
            String collectionId,
            String eventId,
            String bucket,
            String photoKey,
            String imageId,
            BoundingBox boundingBox,
            Float confidence
    ) {}
}
