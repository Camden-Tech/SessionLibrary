package me.BaddCamden.SessionLibrary;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.BaddCamden.SessionLibrary.commands.SessionCommand;
import me.BaddCamden.SessionLibrary.events.SessionAutostartEvent;

public class SessionManager extends JavaPlugin {

    // Singleton instance for easy access from other classes
    private static SessionManager instance;

    // Static configuration/data (loaded once, reused)
    public static FileConfiguration config;
    public static FileConfiguration data;
    public static File dataFile;
    public static FileConfiguration dayCounterData;
    public static File dayCounterFile;

    // Session variables
    public static Session currentSession;

    // Session data in memory
    public static int sessionCount;
    public static boolean autostart;
    public static int autostartBuffer;
    public static int defaultDuration;
    public static boolean scheduledStartEnabled;
    public static LocalDateTime scheduledStartDateTime;
    public static ZoneId scheduledStartZone;
    public static boolean calendarAutoSessionEnabled;
    public static String calendarMode;
    public static ZoneId calendarZone;
    public static LocalDateTime calendarSpecificDateTime;
    public static LocalTime calendarDailyTime;
    public static Set<DayOfWeek> calendarWeekdays;
    public static Set<Integer> calendarMonthDays;
    public static Set<Integer> calendarYearDays;
    public static int calendarCustomCounterTarget;
    public static int calendarDurationOverride;
    public static boolean calendarAutoEnd;

    private BukkitRunnable scheduledStartMonitor;
    private boolean scheduledStartTriggered;
    private BukkitRunnable calendarMonitor;
    private boolean calendarSpecificTriggered;
    private LocalDate lastCalendarTriggerDate;
    private int lastCounterTriggerValue;
    private LocalDate counterLastUpdatedDate;
    private int dayCounterValue;

    /**
     * Initialize plugin state, load configuration files, and register commands and tasks.
     */
    @Override
    public void onEnable() {
        instance = this;

        // Load default config (copies from jar if not present)
        saveDefaultConfig();
        config = getConfig();

        // Load data file (sessiondata.yml) once
        dataFile = new File(getDataFolder(), "sessiondata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create sessiondata.yml");
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        // Load day counter file (daycounter.yml)
        dayCounterFile = new File(getDataFolder(), "daycounter.yml");
        if (!dayCounterFile.exists()) {
            try {
                dayCounterFile.getParentFile().mkdirs();
                dayCounterFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Could not create daycounter.yml");
                e.printStackTrace();
            }
        }
        dayCounterData = YamlConfiguration.loadConfiguration(dayCounterFile);

        // Load static values (in-memory only)
        sessionCount = data.getInt("session-count", 0);
        defaultDuration = config.getInt("session-duration", 3600);
        autostart = config.getBoolean("autostart", false);
        autostartBuffer = config.getInt("autostart-buffer", 60);
        scheduledStartEnabled = config.getBoolean("scheduled-start.enabled", false);
        scheduledStartDateTime = parseScheduledDate(config.getString("scheduled-start.datetime", ""));
        scheduledStartZone = parseZoneId(config.getString("scheduled-start.timezone", ZoneId.systemDefault().getId()));
        loadCalendarConfig();
        loadDayCounter();

        // Register command
        if (getCommand("session") != null) {
            SessionCommand sessionCommand = new SessionCommand(this);
            getCommand("session").setExecutor(sessionCommand);
            getCommand("session").setTabCompleter(sessionCommand);
        } else {
            getLogger().severe("Command 'session' not found in plugin.yml!");
        }

        getLogger().info("SessionManager enabled. Current session count: " + sessionCount);

        // Auto-start session if enabled (with buffer)
        if (autostart) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                currentSession = new Session(this, defaultDuration, true);
                // Fire autostart event so other plugins can react
                Bukkit.getPluginManager().callEvent(new SessionAutostartEvent(currentSession));
                currentSession.start();
            }, autostartBuffer * 20L); // seconds -> ticks
        }

        // Schedule specific start date/time if configured
        if (scheduledStartEnabled && scheduledStartDateTime != null) {
            startScheduledStartMonitor();
        } else if (scheduledStartEnabled) {
            getLogger().warning("Scheduled start is enabled but datetime is invalid. Please check config.");
        }

        startCalendarMonitor();
    }

    /**
     * Persist runtime state back to disk, cancel background monitors, and stop any running session.
     */
    @Override
    public void onDisable() {
        // Save in-memory session count to data file
        data.set("session-count", sessionCount);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save config values back to config.yml
        config.set("session-duration", defaultDuration);
        config.set("autostart", autostart);
        config.set("autostart-buffer", autostartBuffer);
        config.set("scheduled-start.enabled", scheduledStartEnabled);
        config.set("scheduled-start.datetime", scheduledStartDateTime != null
                ? scheduledStartDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : "");
        config.set("scheduled-start.timezone", scheduledStartZone != null ? scheduledStartZone.getId()
                : ZoneId.systemDefault().getId());
        config.set("calendar-auto-session.enabled", calendarAutoSessionEnabled);
        config.set("calendar-auto-session.mode", calendarMode);
        config.set("calendar-auto-session.timezone", calendarZone != null ? calendarZone.getId()
                : ZoneId.systemDefault().getId());
        config.set("calendar-auto-session.specific-datetime", calendarSpecificDateTime != null
                ? calendarSpecificDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : "");
        config.set("calendar-auto-session.daily-time", calendarDailyTime != null
                ? calendarDailyTime.toString()
                : "");
        config.set("calendar-auto-session.days-of-week", calendarWeekdays != null ? new ArrayList<>(calendarWeekdays.stream()
                .map(DayOfWeek::name).collect(Collectors.toList())) : null);
        config.set("calendar-auto-session.days-of-month", calendarMonthDays != null ? new ArrayList<>(calendarMonthDays)
                : null);
        config.set("calendar-auto-session.days-of-year", calendarYearDays != null ? new ArrayList<>(calendarYearDays)
                : null);
        config.set("calendar-auto-session.custom-counter-target", calendarCustomCounterTarget);
        config.set("calendar-auto-session.duration-override", calendarDurationOverride);
        config.set("calendar-auto-session.auto-end", calendarAutoEnd);
        saveConfig();

        saveDayCounter();

        // Stop session cleanly if running
        if (currentSession != null && currentSession.isRunning()) {
            currentSession.stopSession();
        }

        if (scheduledStartMonitor != null) {
            scheduledStartMonitor.cancel();
            scheduledStartMonitor = null;
        }

        if (calendarMonitor != null) {
            calendarMonitor.cancel();
            calendarMonitor = null;
        }

        getLogger().info("SessionManager disabled.");
        instance = null;
    }

    // ------------------------------------------------------------------------
    // API / control methods for other plugins
    // ------------------------------------------------------------------------

    /**
     * Access the singleton plugin instance set during {@link #onEnable()}.
     *
     * @return loaded SessionManager instance or null if not yet enabled.
     */
    public static SessionManager getInstance() {
        return instance;
    }

    /**
     * Get the currently active Session object, if any.
     *
     * @return current session or null when none exists.
     */
    public static Session getCurrentSession() {
        return currentSession;
    }

    /**
     * Determine whether a session is currently running.
     *
     * @return true when a session exists and is active.
     */
    public static boolean hasActiveSession() {
        return currentSession != null && currentSession.isRunning();
    }

    /**
     * Report the number of sessions completed since the last reset.
     *
     * @return in-memory session counter.
     */
    public static int getSessionCount() {
        return sessionCount;
    }

    /**
     * Override the session completion counter (persisted on disable).
     *
     * @param count new counter value.
     */
    public static void setSessionCount(int count) {
        sessionCount = count;
    }

    /**
     * Read the default session duration configured for new sessions.
     *
     * @return number of seconds a new session should last.
     */
    public static int getDefaultDuration() {
        return defaultDuration;
    }

    /**
     * Update the default session length used when no override is provided.
     *
     * @param seconds duration in seconds; ignored if non-positive.
     */
    public static void setDefaultDuration(int seconds) {
        if (seconds > 0) {
            defaultDuration = seconds;
        }
    }

    /**
     * Check if sessions should start automatically on server boot.
     *
     * @return true when autostart is enabled.
     */
    public static boolean isAutostartEnabled() {
        return autostart;
    }

    /**
     * Enable or disable automatic session startup after boot.
     *
     * @param enabled desired autostart flag.
     */
    public static void setAutostartEnabled(boolean enabled) {
        autostart = enabled;
    }

    /**
     * Retrieve the configured buffer (in seconds) before autostart triggers.
     *
     * @return autostart delay in seconds.
     */
    public static int getAutostartBuffer() {
        return autostartBuffer;
    }

    /**
     * Update the autostart buffer while enforcing non-negative values.
     *
     * @param bufferSeconds seconds to wait before auto-starting a session.
     */
    public static void setAutostartBuffer(int bufferSeconds) {
        if (bufferSeconds >= 0) {
            autostartBuffer = bufferSeconds;
        }
    }

    /**
     * Check if a scheduled start time should be monitored.
     *
     * @return true when scheduled start monitoring is enabled.
     */
    public static boolean isScheduledStartEnabled() {
        return scheduledStartEnabled;
    }

    /**
     * Access the configured scheduled start datetime.
     *
     * @return local datetime or null when not configured.
     */
    public static LocalDateTime getScheduledStartDateTime() {
        return scheduledStartDateTime;
    }

    /**
     * Access the time zone used for scheduled start calculations.
     *
     * @return zone id for scheduled start.
     */
    public static ZoneId getScheduledStartZone() {
        return scheduledStartZone;
    }

    /**
     * Start a new session from another plugin.
     *
     * @param durationSeconds duration in seconds, or <= 0 to use defaultDuration.
     * @param autoStartFlag whether this was auto-started (for your own semantics).
     * @return the started Session instance.
     */
    /**
     * Start a new session or return the existing one if already running.
     *
     * @param durationSeconds desired duration, or <=0 to fall back to {@link #defaultDuration}.
     * @param autoStartFlag   metadata flag indicating whether the start was automatic.
     * @return active session instance.
     */
    public static Session startNewSession(int durationSeconds, boolean autoStartFlag) {
        if (instance == null) {
            throw new IllegalStateException("SessionManager not loaded yet");
        }

        if (currentSession == null || !currentSession.isRunning()) {
            int dur = (durationSeconds > 0) ? durationSeconds : defaultDuration;
            currentSession = new Session(instance, dur, autoStartFlag);
            currentSession.start();
        }
        return currentSession;
    }

    /**
     * Begin the end sequence on the current session if one exists.
     */
    public static void endSession() {
        if (currentSession != null) {
            currentSession.beginEndSequence();
        }
    }

    /**
     * Stop and reset the current session without firing end events.
     */
    public static void stopSession() {
        if (currentSession != null) {
            currentSession.stopSession();
        }
    }

    /**
     * Reset the timer on the active session back to its original duration.
     */
    public static void resetSessionTimer() {
        if (currentSession != null) {
            currentSession.reset();
        }
    }

    /**
     * Parse an ISO_LOCAL_DATE_TIME string into a {@link LocalDateTime}.
     *
     * @param dateString raw value from configuration.
     * @return parsed datetime or null when invalid.
     */
    private LocalDateTime parseScheduledDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ex) {
            getLogger().warning("Unable to parse scheduled-start datetime. Expected ISO_LOCAL_DATE_TIME format.");
            return null;
        }
    }

    /**
     * Parse a timezone identifier while falling back to the system default.
     *
     * @param zoneString string ID from configuration.
     * @return a valid {@link ZoneId}.
     */
    private ZoneId parseZoneId(String zoneString) {
        try {
            return ZoneId.of(zoneString);
        } catch (Exception ex) {
            getLogger().warning("Invalid timezone provided for scheduled-start. Using system default.");
            return ZoneId.systemDefault();
        }
    }

    /**
     * Parse a daily time (HH:mm) from configuration.
     *
     * @param timeString raw time string.
     * @return {@link LocalTime} or null when invalid.
     */
    private LocalTime parseLocalTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return null;
        }

        try {
            return LocalTime.parse(timeString);
        } catch (Exception ex) {
            getLogger().warning("Unable to parse calendar daily-time. Expected HH:mm format.");
            return null;
        }
    }

    /**
     * Parse a calendar date using ISO-8601 formatting.
     *
     * @param dateString raw date string.
     * @return {@link LocalDate} value or null.
     */
    private LocalDate parseLocalDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateString);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Load calendar-based scheduling rules from configuration.
     */
    private void loadCalendarConfig() {
        calendarAutoSessionEnabled = config.getBoolean("calendar-auto-session.enabled", false);
        calendarMode = config.getString("calendar-auto-session.mode", "specific").toLowerCase();
        calendarZone = parseZoneId(config.getString("calendar-auto-session.timezone", ZoneId.systemDefault().getId()));
        calendarSpecificDateTime = parseScheduledDate(config.getString("calendar-auto-session.specific-datetime", ""));
        calendarDailyTime = parseLocalTime(config.getString("calendar-auto-session.daily-time", "00:00"));

        calendarWeekdays = new HashSet<>();
        for (String entry : config.getStringList("calendar-auto-session.days-of-week")) {
            try {
                calendarWeekdays.add(DayOfWeek.valueOf(entry.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                getLogger().warning("Invalid day-of-week in config: " + entry);
            }
        }

        calendarMonthDays = new HashSet<>(config.getIntegerList("calendar-auto-session.days-of-month"));
        calendarYearDays = new HashSet<>(config.getIntegerList("calendar-auto-session.days-of-year"));

        calendarCustomCounterTarget = config.getInt("calendar-auto-session.custom-counter-target", 0);
        calendarDurationOverride = config.getInt("calendar-auto-session.duration-override", 0);
        calendarAutoEnd = config.getBoolean("calendar-auto-session.auto-end", true);
        calendarSpecificTriggered = false;
    }

    /**
     * Load the persisted day counter values from disk.
     */
    private void loadDayCounter() {
        dayCounterValue = dayCounterData.getInt("day-count", 0);
        counterLastUpdatedDate = parseLocalDate(dayCounterData.getString("last-updated-date", ""));
        lastCalendarTriggerDate = parseLocalDate(dayCounterData.getString("last-calendar-trigger-date", ""));
        lastCounterTriggerValue = dayCounterData.getInt("last-counter-trigger", 0);
    }

    /**
     * Persist the in-memory day counter values to the YAML file.
     */
    private void saveDayCounter() {
        dayCounterData.set("day-count", dayCounterValue);
        dayCounterData.set("last-updated-date", counterLastUpdatedDate != null ? counterLastUpdatedDate.toString() : "");
        dayCounterData.set("last-calendar-trigger-date", lastCalendarTriggerDate != null ? lastCalendarTriggerDate.toString() : "");
        dayCounterData.set("last-counter-trigger", lastCounterTriggerValue);
        try {
            dayCounterData.save(dayCounterFile);
        } catch (IOException e) {
            getLogger().warning("Could not save daycounter.yml");
        }
    }

    /**
     * Increment the day counter when dates change to support calendar triggers.
     *
     * @param now current timestamp in the configured zone.
     */
    private void updateDayCounterIfNeeded(ZonedDateTime now) {
        LocalDate today = now.toLocalDate();
        if (counterLastUpdatedDate == null) {
            counterLastUpdatedDate = today;
            saveDayCounter();
            return;
        }

        if (counterLastUpdatedDate.isBefore(today)) {
            long daysBetween = ChronoUnit.DAYS.between(counterLastUpdatedDate, today);
            dayCounterValue += (int) daysBetween;
            counterLastUpdatedDate = today;
            saveDayCounter();
        }
    }

    /**
     * Get the current value of the persisted day counter.
     *
     * @return day counter or 0 if the plugin is not yet initialized.
     */
    public static int getDayCounterValue() {
        if (instance == null) {
            return 0;
        }
        return instance.dayCounterValue;
    }

    /**
     * Reset the day counter metrics and immediately persist them.
     */
    public static void resetDayCounter() {
        if (instance != null) {
            instance.dayCounterValue = 0;
            instance.lastCounterTriggerValue = 0;
            instance.counterLastUpdatedDate = ZonedDateTime.now(instance.calendarZone != null ? instance.calendarZone : ZoneId.systemDefault())
                    .toLocalDate();
            instance.saveDayCounter();
        }
    }

    /**
     * Start (or restart) the repeating task that checks calendar-based triggers.
     */
    private void startCalendarMonitor() {
        if (calendarMonitor != null) {
            calendarMonitor.cancel();
        }

        calendarMonitor = new BukkitRunnable() {
            @Override
            public void run() {
                runCalendarCheck();
            }
        };

        calendarMonitor.runTaskTimer(this, 0L, 800L);

        if (calendarAutoSessionEnabled) {
            ZonedDateTime now = ZonedDateTime.now(calendarZone != null ? calendarZone : ZoneId.systemDefault());
            ZonedDateTime next = computeNextEligibleTime(now);
            if (next != null) {
                getLogger().info("Next calendar auto-session eligibility at " + next);
            }
        }
    }

    /**
     * Evaluate configured calendar rules and start sessions when eligibility is met.
     */
    private void runCalendarCheck() {
        ZoneId zone = calendarZone != null ? calendarZone : ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalTime targetTime = calendarDailyTime != null ? calendarDailyTime : LocalTime.MIDNIGHT;

        updateDayCounterIfNeeded(now);

        if (!calendarAutoSessionEnabled) {
            return;
        }

        if (currentSession != null && currentSession.isRunning()) {
            return;
        }

        switch (calendarMode) {
            case "specific":
                if (calendarSpecificTriggered || calendarSpecificDateTime == null) {
                    return;
                }
                ZonedDateTime target = ZonedDateTime.of(calendarSpecificDateTime, zone);
                if (!now.isBefore(target)) {
                    calendarSpecificTriggered = true;
                    triggerCalendarSession(now);
                }
                break;
            case "daily":
                if (lastCalendarTriggerDate != null && lastCalendarTriggerDate.equals(now.toLocalDate())) return;
                if (!now.toLocalTime().isBefore(targetTime)) {
                    triggerCalendarSession(now);
                }
                break;
            case "day-of-week":
                if (calendarWeekdays.contains(now.getDayOfWeek())
                        && (lastCalendarTriggerDate == null || !lastCalendarTriggerDate.equals(now.toLocalDate()))
                        && !now.toLocalTime().isBefore(targetTime)) {
                    triggerCalendarSession(now);
                }
                break;
            case "day-of-month":
                if (calendarMonthDays.contains(now.getDayOfMonth())
                        && (lastCalendarTriggerDate == null || !lastCalendarTriggerDate.equals(now.toLocalDate()))
                        && !now.toLocalTime().isBefore(targetTime)) {
                    triggerCalendarSession(now);
                }
                break;
            case "day-of-year":
                if (calendarYearDays.contains(now.getDayOfYear())
                        && (lastCalendarTriggerDate == null || !lastCalendarTriggerDate.equals(now.toLocalDate()))
                        && !now.toLocalTime().isBefore(targetTime)) {
                    triggerCalendarSession(now);
                }
                break;
            case "custom-counter":
                if (calendarCustomCounterTarget > 0 && dayCounterValue >= calendarCustomCounterTarget
                        && lastCounterTriggerValue != dayCounterValue) {
                    triggerCalendarSession(now);
                    lastCounterTriggerValue = dayCounterValue;
                    resetDayCounter();
                }
                break;
            default:
                break;
        }
    }

    /**
     * Compute the next datetime when an auto-session could start, used for logging.
     *
     * @param now current reference time.
     * @return next eligible timestamp or null if none can be determined.
     */
    private ZonedDateTime computeNextEligibleTime(ZonedDateTime now) {
        ZoneId zone = calendarZone != null ? calendarZone : ZoneId.systemDefault();
        LocalTime timeForCandidate = calendarDailyTime != null ? calendarDailyTime : LocalTime.MIDNIGHT;
        switch (calendarMode) {
            case "specific":
                return calendarSpecificDateTime != null ? ZonedDateTime.of(calendarSpecificDateTime, zone) : null;
            case "daily":
                LocalDate nextDay = now.toLocalTime().isBefore(timeForCandidate) ? now.toLocalDate()
                        : now.toLocalDate().plusDays(1);
                return ZonedDateTime.of(nextDay, timeForCandidate, zone);
            case "day-of-week":
                for (int i = 0; i < 7; i++) {
                    ZonedDateTime candidate = now.plusDays(i);
                    if (calendarWeekdays.contains(candidate.getDayOfWeek())) {
                        return ZonedDateTime.of(candidate.toLocalDate(), timeForCandidate, zone);
                    }
                }
                break;
            case "day-of-month":
                for (int i = 0; i < 60; i++) {
                    ZonedDateTime candidate = now.plusDays(i);
                    if (calendarMonthDays.contains(candidate.getDayOfMonth())) {
                        return ZonedDateTime.of(candidate.toLocalDate(), timeForCandidate, zone);
                    }
                }
                break;
            case "day-of-year":
                for (int i = 0; i < 370; i++) {
                    ZonedDateTime candidate = now.plusDays(i);
                    if (calendarYearDays.contains(candidate.getDayOfYear())) {
                        return ZonedDateTime.of(candidate.toLocalDate(), timeForCandidate, zone);
                    }
                }
                break;
            case "custom-counter":
            default:
                break;
        }
        return null;
    }

    /**
     * Record the date when an auto-session was triggered to avoid duplicate firings.
     *
     * @param date date of the trigger.
     */
    private void markTriggerConsumed(LocalDate date) {
        lastCalendarTriggerDate = date;
        saveDayCounter();
    }

    /**
     * Create and start a new session based on calendar eligibility and broadcast notifications.
     *
     * @param now timestamp of the trigger.
     */
    private void triggerCalendarSession(ZonedDateTime now) {
        if (currentSession != null && currentSession.isRunning()) {
            return;
        }

        int duration = calendarDurationOverride > 0 ? calendarDurationOverride : defaultDuration;
        currentSession = new Session(this, duration, true);
        Bukkit.getPluginManager().callEvent(new SessionAutostartEvent(currentSession));
        String startMessage = config.getString("messages.calendar-session-start", "A calendar session has started.");
        if (startMessage != null && !startMessage.isEmpty()) {
            Bukkit.getServer().broadcastMessage(startMessage.replace("%mode%", calendarMode));
        }
        currentSession.start();

        if (calendarAutoEnd) {
            String autoEndMessage = config.getString("messages.calendar-session-auto-end", "Session will auto-end soon.");
            if (autoEndMessage != null && !autoEndMessage.isEmpty()) {
                Bukkit.getServer().broadcastMessage(autoEndMessage.replace("%seconds%", String.valueOf(duration)));
            }
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (currentSession != null && currentSession.isRunning()) {
                    currentSession.beginEndSequence();
                }
            }, duration * 20L);
        }

        markTriggerConsumed(now.toLocalDate());
    }

    /**
     * Begin monitoring for a scheduled start datetime and start a session when reached.
     */
    private void startScheduledStartMonitor() {
        if (scheduledStartMonitor != null) {
            scheduledStartMonitor.cancel();
        }

        scheduledStartTriggered = false;
        scheduledStartMonitor = new BukkitRunnable() {
            @Override
            public void run() {
                if (scheduledStartTriggered) {
                    cancel();
                    return;
                }

                if (currentSession != null && currentSession.isRunning()) {
                    return;
                }

                LocalDateTime now = LocalDateTime.now(scheduledStartZone);
                if (!now.isBefore(scheduledStartDateTime)) {
                    scheduledStartTriggered = true;
                    currentSession = new Session(SessionManager.this, defaultDuration, true);
                    Bukkit.getPluginManager().callEvent(new SessionAutostartEvent(currentSession));
                    currentSession.start();
                    cancel();
                }
            }
        };

        // check every 30 seconds
        scheduledStartMonitor.runTaskTimer(this, 0L, 600L);
    }
}