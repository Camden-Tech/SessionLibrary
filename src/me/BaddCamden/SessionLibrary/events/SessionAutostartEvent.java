package me.BaddCamden.SessionLibrary.events;


import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import me.BaddCamden.SessionLibrary.Session;

public class SessionAutostartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Session session;

    /**
     * Create an event fired when the plugin auto-starts a session.
     *
     * @param session session that was automatically started.
     */
    public SessionAutostartEvent(Session session) {
        super(false); // not async
        this.session = session;
    }

    /**
     * Access the auto-started session.
     *
     * @return session instance.
     */
    public Session getSession() {
        return session;
    }

    @Override
    /**
     * Required Bukkit handler list accessor.
     *
     * @return handler registry for this event.
     */
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Static accessor used for Bukkit registration.
     *
     * @return handler registry for this event type.
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }
}