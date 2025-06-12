package org.openelisglobal.sample.event.bus;

import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;

/**
 * Event bus interface for handling sample-related events.
 * This provides a centralized way to publish and subscribe to sample events.
 */
public interface SampleEventBus {
    
    /**
     * Publish a sample creation event
     * @param event The sample creation event to publish
     */
    void publishSampleCreated(SamplePatientUpdateDataCreatedEvent event);
    
    /**
     * Subscribe to sample creation events
     * @param subscriber The subscriber that will handle the events
     */
    void subscribeToSampleCreated(SampleEventSubscriber subscriber);
} 