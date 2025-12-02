package me.BaddCamden.SessionLibrary.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import me.BaddCamden.SessionLibrary.Session;

public class SessionEndSequenceStartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Session session;

    public SessionEndSequenceStartEvent(Session session) {
        this.session = session;
    }

    public Session getSession() { return session; }

    public HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}

