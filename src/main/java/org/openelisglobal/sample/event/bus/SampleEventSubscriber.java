package org.openelisglobal.sample.event.bus;

import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;

/**
 * Interface for subscribers that want to handle sample events
 */
public interface SampleEventSubscriber {

    /**
     * Handle a sample creation event
     * 
     * @param event The sample creation event to handle
     */
    void onSampleCreated(SamplePatientUpdateDataCreatedEvent event);
}