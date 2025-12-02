package me.BaddCamden.SessionLibrary.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import me.BaddCamden.SessionLibrary.Session;

public class SessionTickEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Session session;
    private final int secondsLeft;

    public SessionTickEvent(Session session, int secondsLeft) {
        this.session = session;
        this.secondsLeft = secondsLeft;
    }

    public Session getSession() { return session; }
    public int getSecondsLeft() { return secondsLeft; }

    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
