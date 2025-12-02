package me.BaddCamden.SessionLibrary;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.BaddCamden.SessionLibrary.events.SessionEndEvent;
import me.BaddCamden.SessionLibrary.events.SessionEndSequenceEndEvent;
import me.BaddCamden.SessionLibrary.events.SessionEndSequenceStartEvent;
import me.BaddCamden.SessionLibrary.events.SessionStartEvent;
import me.BaddCamden.SessionLibrary.events.SessionTickEvent;
import me.BaddCamden.SessionLibrary.hooks.SessionEndHook;


public class Session {

    // Static registry of end hooks (library-style API)
    private static final List<SessionEndHook> END_HOOKS = new ArrayList<>();

    // Per-session state
    private final Plugin plugin;
    private final int duration; // in seconds
    private int timeLeft;
    private boolean running;
    private boolean endingSequence;
    private final boolean autoStartSession; // reserved metadata if you want it

    private BukkitRunnable task;

    public Session(Plugin plugin, int duration, boolean autoStart) {
        this.plugin = plugin;
        this.duration = duration;
        this.timeLeft = duration;
        this.running = false;
        this.endingSequence = false;
        this.autoStartSession = autoStart;
    }

    // ------------------------------------------------------------------------
    // Static hook API
    // ------------------------------------------------------------------------

    public static void registerEndHook(SessionEndHook hook) {
        if (hook != null && !END_HOOKS.contains(hook)) {
            END_HOOKS.add(hook);
        }
    }

    public static void unregisterEndHook(SessionEndHook hook) {
        END_HOOKS.remove(hook);
    }

    public static void clearEndHooks() {
        END_HOOKS.clear();
    }

    // ------------------------------------------------------------------------
    // Core session logic
    // ------------------------------------------------------------------------

    public void start() {
        if (running) return;
        running = true;
        endingSequence = false;

        Bukkit.getPluginManager().callEvent(new SessionStartEvent(this));

        broadcastMessage(SessionManager.config.getString(
                "messages.session-start", "Session started!"));

        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };
        task.runTaskTimer(plugin, 20L, 20L); // every second
    }

    private void tick() {
        if (!running) return;

        timeLeft--;

        // Halfway message
        if (timeLeft == duration / 2) {
            broadcastMessage(SessionManager.config.getString(
                    "messages.session-halfway", "Session is halfway!"));
        }

        // Last 10 minutes warning
        if (timeLeft == 600) {
            broadcastMessage(SessionManager.config.getString(
                    "messages.session-last-10", "10 minutes left!"));
        }

        // Last minute warnings
        if (timeLeft == 60) {
            broadcastMessage(SessionManager.config.getString(
                    "messages.session-last-60", "1 minute left!"));
        }
        if (timeLeft == 30) {
            broadcastMessage(SessionManager.config.getString(
                    "messages.session-last-30", "30 seconds left!"));
        }

        // Final 10-second countdown
        if (timeLeft <= 10 && timeLeft > 0) {
            String msg = SessionManager.config.getString(
                    "messages.session-countdown",
                    "Countdown: %seconds% seconds");
            broadcastMessage(msg.replace("%seconds%", String.valueOf(timeLeft)));
        }

        // End session when timer hits zero
        if (timeLeft <= 0) {
            beginEndSequence();
        }

        // Tick event for other plugins
        Bukkit.getPluginManager().callEvent(new SessionTickEvent(this, timeLeft));
    }

    /**
     * Public control method: begin the end sequence.
     * Other plugins can call this instead of waiting for the timer.
     */
    public void beginEndSequence() {
        if (endingSequence) return;
        endingSequence = true;

        Bukkit.getPluginManager().callEvent(new SessionEndSequenceStartEvent(this));

        // Let hooks handle custom end behavior first
        for (SessionEndHook hook : new ArrayList<>(END_HOOKS)) {
            try {
                boolean handled = hook.handleEndSequence(this);
                if (handled) {
                    // A hook has fully handled logic (including shutdown?)
                    return;
                }
            } catch (Exception ex) {
                Bukkit.getLogger().warning("[SessionManager] SessionEndHook threw an exception:");
                ex.printStackTrace();
            }
        }

        // Default logic: 1 minute silent countdown then end()
        new BukkitRunnable() {
            int endTimer = 60;

            @Override
            public void run() {
                if (endTimer <= 0) {
                    end();
                    Bukkit.getPluginManager().callEvent(
                            new SessionEndSequenceEndEvent(Session.this));
                    cancel();
                    return;
                }
                endTimer--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Immediately end the session and shut down the server.
     * Hooks / other plugins can call this if they want to skip the minute.
     */
    public void forceEndNow() {
        if (!running) return;
        if (task != null) task.cancel();
        timeLeft = 0;
        endingSequence = false;
        end();
    }

    public void end() {
        running = false;

        Bukkit.getPluginManager().callEvent(new SessionEndEvent(this));

        broadcastMessage(SessionManager.config.getString(
                "messages.session-end", "Session ended!"));

        // Increment consecutive session count
        SessionManager.sessionCount++;

        // Schedule server shutdown (1 second later so messages flush)
        //Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 20L);
    }

    public void stopSession() {
        if (task != null) task.cancel();
        running = false;
        endingSequence = false;
        timeLeft = duration;
    }

    public void reset() {
        timeLeft = duration;
    }

    private void broadcastMessage(String message) {
        if (message == null || message.isEmpty()) return;
        Bukkit.getServer().broadcastMessage(message);
    }

    // ------------------------------------------------------------------------
    // Getters for other plugins
    // ------------------------------------------------------------------------

    public int getTimeLeft() {
        return timeLeft;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isEndingSequence() {
        return endingSequence;
    }

    public boolean isAutoStartSession() {
        return autoStartSession;
    }
}