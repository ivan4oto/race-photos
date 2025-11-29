package com.racephotos.domain.photo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoAssetRepository extends JpaRepository<PhotoAsset, UUID> {
    Optional<PhotoAsset> findByBucketAndObjectKey(String bucket, String objectKey);
    boolean existsByBucketAndObjectKey(String bucket, String objectKey);
    List<PhotoAsset> findByEventIdAndIndexStatusIsNull(UUID eventId);
}
