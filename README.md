# SessionLibrary

SessionLibrary is a Bukkit/Spigot plugin that manages timed play sessions, broadcasts lifecycle messages, and exposes hookable events so other plugins can react to or control session flow.\
**Author:** BaddCamden | **Plugin version:** 1.0 (`src/plugin.yml`) | **API version:** 1.21

## Features at a Glance
- Start, end, reset, or stop server sessions with configurable durations.
- Automatic session starts via:
  - **Autostart:** fire a session on plugin enable after a configurable buffer.
  - **Scheduled start:** fire once at a configured date/time in a specific timezone.
  - **Calendar rules:** recurring triggers (specific date, daily, day-of-week/month/year) or a custom day counter with optional duration overrides and auto-end.
- Broadcasts milestone messages (halfway, last 10 minutes, 60/30-second warnings, final countdown) plus optional calendar start/auto-end notices.
- Hookable lifecycle via Bukkit events and a `SessionEndHook` interface so integrations can observe, veto, or replace shutdown logic.

## Installation
1. Build with Maven or download the compiled jar:
   ```bash
   mvn package
   ```
2. Drop the resulting jar into your server’s `plugins/` folder.
3. Start or reload the server; `config.yml`, `sessiondata.yml`, and `daycounter.yml` will be generated in `plugins/SessionLibrary/`.

## Commands & Permissions
`/session` (permission: `sessionlibrary.admin`)\
Usage: `/session <start|end|reset|stop|duration|autostart|autostartbuffer>`

| Subcommand | Purpose |
| --- | --- |
| `start` | Start a session using the current default duration. |
| `end` | Begin the end sequence (fires hooks/events; may run grace countdown). |
| `reset` | Reset the session timer to its original duration. |
| `stop` | Cancel the session and reset the session counter. |
| `duration <seconds>` | Set the default session duration (seconds). |
| `autostart` | Toggle autostart on next enable. |
| `autostartbuffer <seconds>` | Set the autostart delay before starting. |

## Configuration Highlights (`config.yml`)
- `session-duration`: Default session length in seconds (used if no override is provided).
- `autostart` / `autostart-buffer`: Enable autostart and control the delay (seconds) after plugin enable.
- `end-grace-period-seconds`: Grace window broadcast when ending before final shutdown logic.
- `scheduled-start.enabled`, `scheduled-start.datetime`, `scheduled-start.timezone`: One-time scheduled launch in a timezone.
- `calendar-auto-session.*`: Calendar-driven recurring starts with modes:
  - `specific`: Fire at a single `specific-datetime`.
  - `daily`: Fire daily at `daily-time`.
  - `day-of-week`, `day-of-month`, `day-of-year`: Fire on listed days.
  - `custom-counter`: Fire when the internal day counter reaches `custom-counter-target`; auto-resets after firing.
  - Optional `duration-override` and `auto-end` for auto-ending after the duration.
- `messages.*`: Customize broadcast and feedback messages (start/end, milestones, calendar notices, errors).

## Data Files
- `sessiondata.yml`: Persists `session-count`.
- `daycounter.yml`: Tracks the calendar counter, last update date, and last calendar trigger.

## API Hooks & Events
### Bukkit Events
All events are synchronous Bukkit events you can listen to in other plugins:
- `SessionStartEvent` — Fired when a session starts.
- `SessionTickEvent` — Fired every second with `getSecondsLeft()` to observe countdowns.
- `SessionEndSequenceStartEvent` — Fired when the end sequence begins (before grace logic).
- `SessionEndSequenceEndEvent` — Fired after the end sequence finishes (post-grace).
- `SessionEndEvent` — Fired when the session fully ends.
- `SessionAutostartEvent` — Fired when a session starts via autostart or calendar/scheduled triggers.

### End Hook Interface
`SessionEndHook` lets integrations intercept or replace the default end sequence:
- Method: `boolean handleEndSequence(Session session)`
  - Return `true` to signal your hook fully handled shutdown (skip built-in grace countdown).
  - Return `false` to allow other hooks or the default countdown to proceed.
- Static utilities on `Session`:
  - `registerEndHook(SessionEndHook hook)`
  - `unregisterEndHook(SessionEndHook hook)`
  - `clearEndHooks()`

### Programmatic Control (static helpers on `SessionManager`)
- `startNewSession(int durationSeconds, boolean autoStartFlag)` — Start if none active (≤0 uses default duration).
- `endSession()` — Begin the end sequence on the current session.
- `stopSession()` — Halt the session without ending it; resets timer.
- `resetSessionTimer()` — Reset the active session timer to its original duration.
- State accessors: `getCurrentSession()`, `hasActiveSession()`, `getSessionCount()`, `getDefaultDuration()`, `isAutostartEnabled()`, `getAutostartBuffer()`, `isScheduledStartEnabled()`, `getScheduledStartDateTime()`, `getScheduledStartZone()`.
- Configuration mutators: `setSessionCount(int)`, `setDefaultDuration(int)`, `setAutostartEnabled(boolean)`, `setAutostartBuffer(int)`.
- Day counter helpers: `getDayCounterValue()`, `resetDayCounter()` (used by `custom-counter` calendar mode).

### Session Instance API
When you hold a `Session` reference (from events or `SessionManager.getCurrentSession()`), you can query:
- `getTimeLeft()`, `getDuration()`, `isRunning()`, `isEndingSequence()`, `isAutoStartSession()`.
- Control methods: `beginEndSequence()`, `forceEndNow()`, `stopSession()`, `reset()`.

## Quirks & Notes
- Autostart, scheduled start, and calendar triggers all fire a `SessionAutostartEvent` before starting.
- If any registered `SessionEndHook` returns `true`, the default grace-period countdown is skipped.
- Calendar mode `custom-counter` increments daily and resets to zero after firing at the target value.
- The `session` command permission node checked in code is `sessionlibrary.admin`.

## Development
- Java 17, built against Spigot API `1.21.10-R0.1-SNAPSHOT`.
- Source lives under `src/`, with resources packaged from the same tree (Java files excluded in the build).
