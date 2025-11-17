package com.racephotos.domain.event;

import com.racephotos.domain.common.PricingProfile;
import com.racephotos.domain.photographer.Photographer;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "organizer_name", length = 160)
    private String organizerName;

    @Column(name = "registration_provider", length = 160)
    private String registrationProvider;

    @Column(name = "vector_collection_id", length = 160)
    private String vectorCollectionId;

    @Column(name = "upload_prefix", length = 255)
    private String uploadPrefix;

    @Column(name = "timezone", length = 60)
    private String timezone;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "location_name", length = 160)
    private String locationName;

    @Column(name = "location_city", length = 120)
    private String locationCity;

    @Column(name = "location_state", length = 120)
    private String locationState;

    @Column(name = "location_country", length = 120)
    private String locationCountry;

    @Column(name = "cover_image_key", length = 255)
    private String coverImageKey;

    @Column(name = "platform_commission_rate", precision = 5, scale = 4)
    private BigDecimal platformCommissionRate;

    @Column(name = "watermarking_enabled", nullable = false)
    private boolean watermarkingEnabled = true;

    @Column(name = "auto_publish_matches", nullable = false)
    private boolean autoPublishMatches = false;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "pricePerPhoto", column = @Column(name = "event_price_per_photo", precision = 10, scale = 2)),
            @AttributeOverride(name = "bundlePrice", column = @Column(name = "event_bundle_price", precision = 10, scale = 2)),
            @AttributeOverride(name = "bundleSize", column = @Column(name = "event_bundle_size")),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "event_currency_code", length = 3))
    })
    private PricingProfile defaultPricing = new PricingProfile();

    @Embedded
    private EventAccessPolicy accessPolicy = new EventAccessPolicy();

    @Column(name = "participant_message", columnDefinition = "TEXT")
    private String participantMessage;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_photographers",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "photographer_id")
    )
    private Set<Photographer> photographers = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Event() {
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void assignPhotographer(Photographer photographer) {
        if (photographer == null) {
            return;
        }
        this.photographers.add(photographer);
        photographer.getEvents().add(this);
    }

    public void removePhotographer(Photographer photographer) {
        if (photographer == null) {
            return;
        }
        this.photographers.remove(photographer);
        photographer.getEvents().remove(this);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public String getRegistrationProvider() {
        return registrationProvider;
    }

    public void setRegistrationProvider(String registrationProvider) {
        this.registrationProvider = registrationProvider;
    }

    public String getVectorCollectionId() {
        return vectorCollectionId;
    }

    public void setVectorCollectionId(String vectorCollectionId) {
        this.vectorCollectionId = vectorCollectionId;
    }

    public String getUploadPrefix() {
        return uploadPrefix;
    }

    public void setUploadPrefix(String uploadPrefix) {
        this.uploadPrefix = uploadPrefix;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getLocationCity() {
        return locationCity;
    }

    public void setLocationCity(String locationCity) {
        this.locationCity = locationCity;
    }

    public String getLocationState() {
        return locationState;
    }

    public void setLocationState(String locationState) {
        this.locationState = locationState;
    }

    public String getLocationCountry() {
        return locationCountry;
    }

    public void setLocationCountry(String locationCountry) {
        this.locationCountry = locationCountry;
    }

    public String getCoverImageKey() {
        return coverImageKey;
    }

    public void setCoverImageKey(String coverImageKey) {
        this.coverImageKey = coverImageKey;
    }

    public BigDecimal getPlatformCommissionRate() {
        return platformCommissionRate;
    }

    public void setPlatformCommissionRate(BigDecimal platformCommissionRate) {
        this.platformCommissionRate = platformCommissionRate;
    }

    public boolean isWatermarkingEnabled() {
        return watermarkingEnabled;
    }

    public void setWatermarkingEnabled(boolean watermarkingEnabled) {
        this.watermarkingEnabled = watermarkingEnabled;
    }

    public boolean isAutoPublishMatches() {
        return autoPublishMatches;
    }

    public void setAutoPublishMatches(boolean autoPublishMatches) {
        this.autoPublishMatches = autoPublishMatches;
    }

    public PricingProfile getDefaultPricing() {
        return defaultPricing;
    }

    public void setDefaultPricing(PricingProfile defaultPricing) {
        this.defaultPricing = defaultPricing;
    }

    public EventAccessPolicy getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(EventAccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public String getParticipantMessage() {
        return participantMessage;
    }

    public void setParticipantMessage(String participantMessage) {
        this.participantMessage = participantMessage;
    }

    public Set<Photographer> getPhotographers() {
        return photographers;
    }

    public void setPhotographers(Set<Photographer> photographers) {
        this.photographers = photographers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event event)) return false;
        return id != null && Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
