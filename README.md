# SessionLibrary

SessionLibrary is a Bukkit/Spigot plugin that runs timed server sessions, broadcasts lifecycle messages, and exposes multiple hooks so other plugins can observe or override what happens when a session starts, ticks, or ends.

## Key capabilities
- Manual or automated sessions with configurable durations and broadcast milestones (halfway, 10-minute, 60/30-second, final countdown).
- Automatic starts via autostart on enable, one-time scheduled starts, or recurring calendar rules (specific date, daily/weekly/monthly/yearly windows, or a custom day counter trigger with optional auto-end).
- Graceful end sequence that can be replaced or cancelled by `SessionEndHook` implementations.
- Persisted counters and a day counter that other plugins (such as the included DayUtils example) can read via the public API.

## Quick usage examples
- **Start a default session:** `/session start`
- **Run a 90-minute session:** `/session duration 5400` then `/session start`
- **Enable autostart with a 2-minute buffer:** `/session autostart` then `/session autostartbuffer 120`
- **Schedule a one-off start for a future date/time:** set `scheduled-start.enabled: true` and `scheduled-start.datetime: 2025-01-15T18:00:00` in `config.yml` (timezone respected via `scheduled-start.timezone`).
- **Fire recurring calendar sessions:** configure `calendar-auto-session.mode: day-of-week` with `days-of-week: [MONDAY, WEDNESDAY]` to auto-start when the clock hits the configured `daily-time`.

## Installation
1. Build with Maven or download the compiled jar:
   ```bash
   mvn package
   ```
2. Place the jar in your server's `plugins/` folder.
3. Start or reload the server; `config.yml`, `sessiondata.yml`, and `daycounter.yml` will be created under `plugins/SessionLibrary/`.

## Commands & permissions
`/session` (permission: `sessionlibrary.admin` checked in code)

| Subcommand | Purpose | Example |
| --- | --- | --- |
| `start` | Start a session using the current default duration. | `/session start` |
| `end` | Begin the end sequence (fires hooks/events; may run grace countdown). | `/session end` |
| `reset` | Reset the session timer to its original duration. | `/session reset` |
| `stop` | Cancel the session and reset the session counter. | `/session stop` |
| `duration <seconds>` | Set the default session duration (seconds). | `/session duration 1800` |
| `autostart` | Toggle autostart on next enable. | `/session autostart` |
| `autostartbuffer <seconds>` | Set the autostart delay before starting. | `/session autostartbuffer 30` |

## Configuration highlights
- `session-duration`: Default session length (seconds) used when no override is provided.
- `autostart` / `autostart-buffer`: Toggle and delay automatic startup after plugin enable.
- `end-grace-period-seconds`: Grace window broadcast when ending before shutdown logic/hook overrides.
- `scheduled-start.*`: Enable and configure a one-time scheduled start in a specific timezone.
- `calendar-auto-session.*`: Recurring triggers (specific date, daily, day-of-week/month/year, or custom counter) with optional `duration-override` and `auto-end`.
- `messages.*`: Customize broadcast and feedback messages (start/end, milestones, calendar notices, errors).

## Integration & hook reference (for other plugins)
The following public types, methods, and fields are available to integrations.

### Bukkit events
All events are synchronous; each exposes the active `Session` via `getSession()` unless noted.

| Event | Purpose | Extra data |
| --- | --- | --- |
| `SessionStartEvent` | Fired when a session starts. | — |
| `SessionTickEvent` | Fired each second of a running session. | `getSecondsLeft()` returns remaining seconds. |
| `SessionEndSequenceStartEvent` | Fired when the end sequence begins (before hooks or grace). | — |
| `SessionEndSequenceEndEvent` | Fired after the grace countdown completes. | — |
| `SessionEndEvent` | Fired when the session fully ends. | — |
| `SessionAutostartEvent` | Fired when autostart, scheduled, or calendar logic starts a session. | — |

### End-hook interface
`SessionEndHook` (package `me.BaddCamden.SessionLibrary.hooks`)
- `boolean handleEndSequence(Session session)`: return `true` to signal that your hook fully handled shutdown (skipping default grace countdown); return `false` to let other hooks or the default logic proceed.

### Session static hook registry
- `Session.registerEndHook(SessionEndHook hook)`: register a hook (duplicates ignored).
- `Session.unregisterEndHook(SessionEndHook hook)`: remove a previously registered hook.
- `Session.clearEndHooks()`: clear all registered hooks.

### Session instance API
Methods exposed on a `Session` (obtainable from events or `SessionManager.getCurrentSession()`):
- Lifecycle control: `start()`, `beginEndSequence()`, `forceEndNow()`, `stopSession()`, `reset()`.
- State queries: `getTimeLeft()`, `getDuration()`, `isRunning()`, `isEndingSequence()`, `isAutoStartSession()`.

### SessionManager control helpers
Static helpers on `SessionManager` for programmatic control and configuration:
- `Session getCurrentSession()`, `boolean hasActiveSession()`: inspect current session state.
- `Session startNewSession(int durationSeconds, boolean autoStartFlag)`: start a session (≤0 uses default duration).
- `void endSession()`, `void stopSession()`, `void resetSessionTimer()`: drive the current session.
- Counters and defaults: `int getSessionCount()`, `void setSessionCount(int)`, `int getDefaultDuration()`, `void setDefaultDuration(int)`.
- Autostart: `boolean isAutostartEnabled()`, `void setAutostartEnabled(boolean)`, `int getAutostartBuffer()`, `void setAutostartBuffer(int)`.
- Scheduled start info: `boolean isScheduledStartEnabled()`, `LocalDateTime getScheduledStartDateTime()`, `ZoneId getScheduledStartZone()`.
- Day counter helpers: `int getDayCounterValue()`, `void resetDayCounter()`.

### Public configuration fields
If you prefer to read the loaded YAML configurations directly, these public fields are available on `SessionManager` (updated during enable/disable): `config`, `data`, `dataFile`, `dayCounterData`, `dayCounterFile`, `currentSession`, `sessionCount`, `autostart`, `autostartBuffer`, `defaultDuration`, `scheduledStartEnabled`, `scheduledStartDateTime`, `scheduledStartZone`, `calendarAutoSessionEnabled`, `calendarMode`, `calendarZone`, `calendarSpecificDateTime`, `calendarDailyTime`, `calendarWeekdays`, `calendarMonthDays`, `calendarYearDays`, `calendarCustomCounterTarget`, `calendarDurationOverride`, and `calendarAutoEnd`.

## Notes & quirks
- Autostart, scheduled start, and calendar triggers all fire `SessionAutostartEvent` before starting.
- If any registered `SessionEndHook` returns `true`, the default grace-period countdown is skipped.
- The permission node enforced in code for `/session` is `sessionlibrary.admin` (plugin.yml lists `sessionmanager.admin`).
- Day counter values persist in `daycounter.yml`; use the API helpers to read or reset them.
