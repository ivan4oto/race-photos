package com.racephotos.domain.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Captures how participants gain access to an event's gallery.
 */
@Embeddable
public class EventAccessPolicy {

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibility_mode", nullable = false, length = 40)
    private EligibilityMode mode = EligibilityMode.NONE;

    @Column(name = "eligibility_provider", length = 120)
    private String provider;

    @Column(name = "eligibility_config", columnDefinition = "TEXT")
    private String configuration;

    public EventAccessPolicy() {
    }

    public EligibilityMode getMode() {
        return mode;
    }

    public void setMode(EligibilityMode mode) {
        this.mode = mode;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }
}
