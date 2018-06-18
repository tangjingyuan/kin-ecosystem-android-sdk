
package com.kin.ecosystem.bi.events;

// Augmented by script

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.kin.ecosystem.bi.Event;
import com.kin.ecosystem.bi.EventLoggerImpl;
import com.kin.ecosystem.bi.EventsStore;


/**
 * User taps on balance and goes to the order history page
 * 
 */
public class BalanceTapped implements Event {
    public static final String EVENT_NAME = "balance_tapped";
    public static final String EVENT_TYPE = "analytics";

    // Augmented by script
    public static BalanceTapped create() {
        return new BalanceTapped(
            (Common) EventsStore.common(),
            (User) EventsStore.user(),
            (Client) EventsStore.client());
    }

    // Augmented by script
    public static void fire() {
        final BalanceTapped event = new BalanceTapped(
            (Common) EventsStore.common(),
            (User) EventsStore.user(),
            (Client) EventsStore.client());

        EventLoggerImpl.Send(event);
    }

    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("event_name")
    @Expose
    private String eventName = EVENT_NAME;
    /**
     * 
     * (Required)
     * 
     */
    @SerializedName("event_type")
    @Expose
    private String eventType = EVENT_TYPE;
    /**
     * common properties for all events
     * (Required)
     * 
     */
    @SerializedName("common")
    @Expose
    private Common common;
    /**
     * common user properties
     * (Required)
     * 
     */
    @SerializedName("user")
    @Expose
    private User user;
    /**
     * common properties for all client events
     * (Required)
     * 
     */
    @SerializedName("client")
    @Expose
    private Client client;

    /**
     * No args constructor for use in serialization
     * 
     */
    public BalanceTapped() {
    }

    /**
     * 
     * @param common

     * @param client

     * @param user
     */
    public BalanceTapped(Common common, User user, Client client) {
        super();
        this.common = common;
        this.user = user;
        this.client = client;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * common properties for all events
     * (Required)
     * 
     */
    public Common getCommon() {
        return common;
    }

    /**
     * common properties for all events
     * (Required)
     * 
     */
    public void setCommon(Common common) {
        this.common = common;
    }

    /**
     * common user properties
     * (Required)
     * 
     */
    public User getUser() {
        return user;
    }

    /**
     * common user properties
     * (Required)
     * 
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * common properties for all client events
     * (Required)
     * 
     */
    public Client getClient() {
        return client;
    }

    /**
     * common properties for all client events
     * (Required)
     * 
     */
    public void setClient(Client client) {
        this.client = client;
    }

}
