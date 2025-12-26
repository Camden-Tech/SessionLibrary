package me.BaddCamden.SessionLibrary.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import me.BaddCamden.SessionLibrary.Session;

public class SessionTickEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Session session;
    private final int secondsLeft;

    /**
     * Create an event fired each second of a running session.
     *
     * @param session     session being ticked.
     * @param secondsLeft seconds remaining until the end sequence starts.
     */
    public SessionTickEvent(Session session, int secondsLeft) {
        this.session = session;
        this.secondsLeft = secondsLeft;
    }

    /**
     * Access the session linked to this tick.
     *
     * @return active session instance.
     */
    public Session getSession() { return session; }

    /**
     * Retrieve the remaining seconds at the moment of this tick.
     *
     * @return seconds remaining.
     */
    public int getSecondsLeft() { return secondsLeft; }

    /**
     * Required Bukkit handler list accessor.
     *
     * @return handler registry for this event.
     */
    public HandlerList getHandlers() { return handlers; }

    /**
     * Static accessor for Bukkit event registration.
     *
     * @return handler registry for this event type.
     */
    public static HandlerList getHandlerList() { return handlers; }
}
