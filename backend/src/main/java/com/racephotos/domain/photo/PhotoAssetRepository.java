package com.racephotos.domain.photo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface PhotoAssetRepository extends JpaRepository<PhotoAsset, UUID> {
    Optional<PhotoAsset> findByBucketAndObjectKey(String bucket, String objectKey);
    boolean existsByBucketAndObjectKey(String bucket, String objectKey);
    List<PhotoAsset> findByEventIdAndIndexStatusIsNull(UUID eventId);
    long countByEventIdAndIndexStatusIsNull(UUID eventId);
    long countByEventIdAndIndexStatusIsNotNull(UUID eventId);

    @Transactional(readOnly = true)
    @Query("select p.objectKey from PhotoAsset p where p.event.id = :eventId")
    Stream<String> streamObjectKeysByEventId(@Param("eventId") UUID eventId);
}
