package org.openelisglobal.common.event.bus;

/**
 * Interface for subscribers that want to handle events
 * 
 * @param <T> The type of event this subscriber handles
 */
public interface EventSubscriber<T> {
    
    /**
     * Handle an event
     * @param event The event to handle
     */
    void onEvent(T event);
}
