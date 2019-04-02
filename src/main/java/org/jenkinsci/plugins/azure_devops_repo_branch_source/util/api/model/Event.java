package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model;

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.UUID;

/**
 * Encapsulates the properties of an event.
 */
public class Event {
    private UUID id;
    private String eventType;
    private String publisherId;
    private EventScope scope;
    private FormattedEventMessage message;
    private FormattedEventMessage detailedMessage;
    private JsonObject resource;
    private String resourceVersion;
    private Map<String, ResourceContainer> resourceContainers;

    public Event() {

    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(final String eventType) {
        this.eventType = eventType;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(final String publisherId) {
        this.publisherId = publisherId;
    }

    public EventScope getScope() {
        return scope;
    }

    public void setScope(final EventScope scope) {
        this.scope = scope;
    }

    public FormattedEventMessage getMessage() {
        return message;
    }

    public void setMessage(final FormattedEventMessage message) {
        this.message = message;
    }

    public FormattedEventMessage getDetailedMessage() {
        return detailedMessage;
    }

    public void setDetailedMessage(final FormattedEventMessage detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

    public JsonObject getResource() {
        return resource;
    }

    public void setResource(final JsonObject resource) {
        this.resource = resource;
    }

    public String getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(final String resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public Map<String, ResourceContainer> getResourceContainers() {
        return resourceContainers;
    }

    public void setResourceContainers(final Map<String, ResourceContainer> resourceContainers) {
        this.resourceContainers = resourceContainers;
    }
}
