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

    /**
     * Create a new session that can be started, monitored, and ended.
     *
     * @param plugin     plugin context used to schedule tasks and fire events.
     * @param duration   total session length in seconds.
     * @param autoStart  whether the session was initiated automatically (metadata only).
     */
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

    /**
     * Register a callback that can override or extend the default end sequence.
     * Duplicate hooks are ignored.
     *
     * @param hook consumer invoked when the end sequence begins.
     */
    public static void registerEndHook(SessionEndHook hook) {
        if (hook != null && !END_HOOKS.contains(hook)) {
            END_HOOKS.add(hook);
        }
    }

    /**
     * Remove a previously registered end hook.
     *
     * @param hook hook instance to remove.
     */
    public static void unregisterEndHook(SessionEndHook hook) {
        END_HOOKS.remove(hook);
    }

    /**
     * Clear all registered end hooks, restoring default end behavior only.
     */
    public static void clearEndHooks() {
        END_HOOKS.clear();
    }

    // ------------------------------------------------------------------------
    // Core session logic
    // ------------------------------------------------------------------------

    /**
     * Start the session timer and broadcast a start notification.
     * Any existing task is cancelled before a new one is scheduled.
     */
    public void start() {
        if (running) return;
        if (task != null) {
            task.cancel();
        }
        timeLeft = duration;
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

    /**
     * Advance the timer by one second, emitting warnings and triggering the end sequence when needed.
     */
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
     * Begin the configured end sequence, notifying listeners and allowing hooks to override behavior.
     * Other plugins can invoke this to bypass waiting for the timer to expire.
     */
    public void beginEndSequence() {
        if (endingSequence) return;
        endingSequence = true;

        int graceSeconds = Math.max(1, SessionManager.config.getInt("end-grace-period-seconds", 60));
        broadcastMessage(SessionManager.config.getString(
                "messages.session-ending",
                "Session has ended. Please log off within %seconds% seconds.")
                .replace("%seconds%", String.valueOf(graceSeconds)));

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

        // Default logic: countdown grace period then end()
        new BukkitRunnable() {
            int endTimer = graceSeconds;

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
     * Immediately terminate the session, bypassing the grace period countdown.
     */
    public void forceEndNow() {
        if (!running) return;
        if (task != null) task.cancel();
        timeLeft = 0;
        endingSequence = false;
        end();
    }

    /**
     * Finalize the session by stopping timers, emitting the end event, and incrementing the counter.
     */
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

    /**
     * Cancel any running task and restore the timer to its initial state without firing events.
     */
    public void stopSession() {
        if (task != null) task.cancel();
        running = false;
        endingSequence = false;
        timeLeft = duration;
    }

    /**
     * Reset the timer to the full duration while leaving the running state unchanged.
     */
    public void reset() {
        timeLeft = duration;
    }

    /**
     * Broadcast a message to all players if the text is present.
     *
     * @param message text to broadcast; ignored if null or empty.
     */
    private void broadcastMessage(String message) {
        if (message == null || message.isEmpty()) return;
        Bukkit.getServer().broadcastMessage(message);
    }

    // ------------------------------------------------------------------------
    // Getters for other plugins
    // ------------------------------------------------------------------------

    /**
     * Retrieve the remaining seconds before the end sequence is triggered.
     *
     * @return seconds left in the session countdown.
     */
    public int getTimeLeft() {
        return timeLeft;
    }

    /**
     * Get the configured full duration of this session.
     *
     * @return session duration in seconds.
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Determine whether the session is currently active.
     *
     * @return true when the timer is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Check if the end sequence countdown is underway.
     *
     * @return true when the session is in its shutdown grace period.
     */
    public boolean isEndingSequence() {
        return endingSequence;
    }

    /**
     * Identify whether this session originated from an auto-start trigger.
     *
     * @return true if auto-started; false for manual starts.
     */
    public boolean isAutoStartSession() {
        return autoStartSession;
    }
}