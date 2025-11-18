package com.racephotos.domain.photographer;

import com.racephotos.domain.common.PricingProfile;
import com.racephotos.domain.event.Event;

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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "photographers")
public class Photographer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "email", nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "phone_number", length = 40)
    private String phoneNumber;

    @Column(name = "studio_name", length = 160)
    private String studioName;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "default_currency", length = 3)
    private String defaultCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PhotographerStatus status = PhotographerStatus.ONBOARDING;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    @Column(name = "commission_override", precision = 5, scale = 4)
    private BigDecimal commissionOverride;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "pricePerPhoto", column = @Column(name = "default_price_per_photo", precision = 10, scale = 2)),
            @AttributeOverride(name = "bundlePrice", column = @Column(name = "default_bundle_price", precision = 10, scale = 2)),
            @AttributeOverride(name = "bundleSize", column = @Column(name = "default_bundle_size")),
            @AttributeOverride(name = "currencyCode", column = @Column(name = "pricing_currency", length = 3))
    })
    private PricingProfile rateCard = new PricingProfile();

    @Embedded
    private PayoutPreferences payoutPreferences = new PayoutPreferences();

    @Column(name = "payout_threshold", precision = 10, scale = 2)
    private BigDecimal payoutThreshold;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @ManyToMany(mappedBy = "photographers", fetch = FetchType.LAZY)
    private Set<Event> events = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Photographer() {
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

    public void assignEvent(Event event) {
        if (event == null) {
            return;
        }
        this.events.add(event);
        event.getPhotographers().add(this);
    }

    public void removeEvent(Event event) {
        if (event == null) {
            return;
        }
        this.events.remove(event);
        event.getPhotographers().remove(this);
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStudioName() {
        return studioName;
    }

    public void setStudioName(String studioName) {
        this.studioName = studioName;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public PhotographerStatus getStatus() {
        return status;
    }

    public void setStatus(PhotographerStatus status) {
        this.status = status;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public BigDecimal getCommissionOverride() {
        return commissionOverride;
    }

    public void setCommissionOverride(BigDecimal commissionOverride) {
        this.commissionOverride = commissionOverride;
    }

    public PricingProfile getRateCard() {
        return rateCard;
    }

    public void setRateCard(PricingProfile rateCard) {
        this.rateCard = rateCard;
    }

    public PayoutPreferences getPayoutPreferences() {
        return payoutPreferences;
    }

    public void setPayoutPreferences(PayoutPreferences payoutPreferences) {
        this.payoutPreferences = payoutPreferences;
    }

    public BigDecimal getPayoutThreshold() {
        return payoutThreshold;
    }

    public void setPayoutThreshold(BigDecimal payoutThreshold) {
        this.payoutThreshold = payoutThreshold;
    }

    public String getInternalNotes() {
        return internalNotes;
    }

    public void setInternalNotes(String internalNotes) {
        this.internalNotes = internalNotes;
    }

    public Set<Event> getEvents() {
        return events;
    }

    public void setEvents(Set<Event> events) {
        this.events = events;
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
        if (!(o instanceof Photographer that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
