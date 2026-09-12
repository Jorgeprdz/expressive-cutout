from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected 1 match, found {count}: {old[:80]!r}")
    p.write_text(text.replace(old, new, 1))
    print(f"patched {path}")


# 1) Carry Android 16 Live Update identity through the signal -> render model.
replace_once(
    "app/src/main/java/com/ekoehler/expressivecutout/core/CutoutSignal.kt",
    "        val progressData: ProgressData? = null,\n        val isSilent: Boolean = false,\n",
    "        val progressData: ProgressData? = null,\n        val isSilent: Boolean = false,\n        /** True for an Android 16 promoted ongoing / Live Update notification. */\n        val isLiveUpdate: Boolean = false,\n",
)

replace_once(
    "app/src/main/java/com/ekoehler/expressivecutout/overlay/IslandEvent.kt",
    "    val progressData: ProgressData? = null,\n    /** The package name of the app that posted this event, if any. */\n",
    "    val progressData: ProgressData? = null,\n    /** Pins this notification while Android still reports its promoted ongoing activity alive. */\n    val isLiveUpdate: Boolean = false,\n    /** The package name of the app that posted this event, if any. */\n",
)

replace_once(
    "app/src/main/java/com/ekoehler/expressivecutout/overlay/IconResolver.kt",
    "            progressData = signal.progressData,\n            packageName = packageName,\n",
    "            progressData = signal.progressData,\n            isLiveUpdate = signal.isLiveUpdate,\n            packageName = packageName,\n",
)

# 2) Notification listener: accept promoted ongoing notifications, seed already-running Live Updates,
# keep them out of hold/alert logic, and end their pinned state when the source notification vanishes.
listener = "app/src/main/java/com/ekoehler/expressivecutout/service/CutoutNotificationListenerService.kt"
replace_once(
    listener,
    "import com.ekoehler.expressivecutout.core.IslandEventBus\n",
    "import com.ekoehler.expressivecutout.core.IslandEventBus\nimport com.ekoehler.expressivecutout.core.LiveUpdateBus\nimport com.ekoehler.expressivecutout.core.PromotedLiveUpdateClassifier\n",
)

replace_once(
    listener,
    "        observeBehaviour()\n        seedMediaArt()\n",
    "        observeBehaviour()\n        seedMediaArt()\n        seedPromotedLiveUpdates()\n",
)

replace_once(
    listener,
    "    private fun seedMediaArt() {\n        val active = runCatching { activeNotifications }.getOrNull() ?: return\n        active.sortedBy { it.postTime }.forEach { it.publishMediaArt() }\n    }\n",
    "    private fun seedMediaArt() {\n        val active = runCatching { activeNotifications }.getOrNull() ?: return\n        active.sortedBy { it.postTime }.forEach { it.publishMediaArt() }\n    }\n\n    /** Recover promoted ongoing notifications that were already active before this listener bound. */\n    private fun seedPromotedLiveUpdates() {\n        val active = runCatching { activeNotifications }.getOrNull() ?: return\n        val promoted = active.filter { sbn ->\n            sbn.isPromotedLiveUpdate() &&\n                !CallNotificationParser.isCall(sbn) &&\n                !TimerNotificationParser.isTimer(sbn)\n        }\n        LiveUpdateBus.replace(promoted.mapTo(linkedSetOf()) { it.key })\n        promoted.sortedBy { it.postTime }.forEach { onNotificationPosted(it, null) }\n    }\n",
)

replace_once(
    listener,
    "        if (TimerNotificationParser.isTimer(notification)) {\n            handleTimer(notification)\n            return\n        }\n\n        // Unlike the two branches above, this one doesn't return: album art has to reach the music\n",
    "        if (TimerNotificationParser.isTimer(notification)) {\n            handleTimer(notification)\n            return\n        }\n\n        val isLiveUpdate = notification.isPromotedLiveUpdate()\n        if (isLiveUpdate) LiveUpdateBus.posted(notification.key)\n\n        // Unlike the two branches above, this one doesn't return: album art has to reach the music\n",
)

replace_once(
    listener,
    "        if (progress == null && suppressed.isSuppressed(fingerprint)) return\n",
    "        if (progress == null && !isLiveUpdate && suppressed.isSuppressed(fingerprint)) return\n",
)

replace_once(
    listener,
    "            progressData = progress,\n            isSilent = isSilent,\n",
    "            progressData = progress,\n            isSilent = isSilent,\n            isLiveUpdate = isLiveUpdate,\n",
)

replace_once(
    listener,
    "        if (alertOnNotification && progress == null) {\n",
    "        if (alertOnNotification && progress == null && !isLiveUpdate) {\n",
)

replace_once(
    listener,
    "        if (dismissNotifications && progress == null && isUserPresent()) {\n",
    "        if (dismissNotifications && progress == null && !isLiveUpdate && isUserPresent()) {\n",
)

replace_once(
    listener,
    "    override fun onNotificationRemoved(sbn: StatusBarNotification?) {\n        // Clears call cutout when call ends\n",
    "    override fun onNotificationRemoved(sbn: StatusBarNotification?) {\n        sbn?.key?.let { LiveUpdateBus.removed(it) }\n        // Clears call cutout when call ends\n",
)

replace_once(
    listener,
    "    /**\n     * Whether a notification should reach the island at all: drops the app's own notifications,\n     * group summaries, and the ongoing-but-unclearable ones that are plumbing rather than news.\n     */\n    private fun StatusBarNotification.shouldSurface(): Boolean {\n",
    "    /** Android 16 Live Update detection without requiring compileSdk 36. */\n    private fun StatusBarNotification.isPromotedLiveUpdate(): Boolean {\n        val requested = notification.extras\n            ?.getBoolean(PromotedLiveUpdateClassifier.EXTRA_REQUEST_PROMOTED_ONGOING_COMPAT, false) == true\n        return PromotedLiveUpdateClassifier.isLiveUpdate(notification.flags, requested)\n    }\n\n    /**\n     * Whether a notification should reach the island at all: drops the app's own notifications,\n     * group summaries, and the ongoing-but-unclearable ones that are plumbing rather than news.\n     */\n    private fun StatusBarNotification.shouldSurface(): Boolean {\n",
)

replace_once(
    listener,
    "        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false\n        // A transfer in flight (a Chrome download, an upload) is ongoing and unclearable by design,\n",
    "        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false\n        // Android 16 Live Updates are intentionally ongoing/unclearable. The old noise filter below\n        // must never discard them merely because they have no classic progress extras.\n        if (isPromotedLiveUpdate()) return true\n        // A transfer in flight (a Chrome download, an upload) is ongoing and unclearable by design,\n",
)

# 3) Overlay: promoted ongoing notifications are real live tiles. Keep them pinned, update them in
# place, return to them after transient notifications, and remove them only when the framework says
# the source notification ended.
overlay = "app/src/main/java/com/ekoehler/expressivecutout/overlay/IslandOverlayController.kt"
replace_once(
    overlay,
    "import com.ekoehler.expressivecutout.core.IslandEventBus\n",
    "import com.ekoehler.expressivecutout.core.IslandEventBus\nimport com.ekoehler.expressivecutout.core.LiveUpdateBus\n",
)

replace_once(
    overlay,
    "    private var assistantActive = false\n    private var lastAssistantEvent: IslandEvent? = null\n",
    "    private var assistantActive = false\n    private var lastAssistantEvent: IslandEvent? = null\n    /** Last promoted ongoing notification, kept so it can return after a transient interruption. */\n    private var lastLiveUpdateEvent: IslandEvent? = null\n",
)

replace_once(
    overlay,
    "        observeOnCall()\n        observeRunningTimer()\n        observePreviewPin()\n",
    "        observeOnCall()\n        observeRunningTimer()\n        observeLiveUpdates()\n        observePreviewPin()\n",
)

replace_once(
    overlay,
    "            callActive && lastCallEvent != null -> {\n                dismissJob?.cancel()\n                expanded = false\n                currentEvent.value = lastCallEvent\n            }\n            musicPlaying && lastMusicEvent != null && !playerAppHidden -> {\n",
    "            callActive && lastCallEvent != null -> {\n                dismissJob?.cancel()\n                expanded = false\n                currentEvent.value = lastCallEvent\n            }\n            lastLiveUpdateEvent?.let { event ->\n                event.notificationKey?.let { it in LiveUpdateBus.activeKeys.value } == true\n            } == true -> {\n                dismissJob?.cancel()\n                expanded = false\n                currentEvent.value = lastLiveUpdateEvent\n            }\n            musicPlaying && lastMusicEvent != null && !playerAppHidden -> {\n",
)

replace_once(
    overlay,
    "    /** The assistant cutout should stay pinned up (no auto-dismiss) while assistant is active. */\n    private fun isPinnedAssistant(): Boolean = assistantActive && currentEvent.value?.assistant != null\n\n    /** The lock cutout should stay pinned up (no auto-dismiss) while the device is locked. */\n",
    "    /** The assistant cutout should stay pinned up (no auto-dismiss) while assistant is active. */\n    private fun isPinnedAssistant(): Boolean = assistantActive && currentEvent.value?.assistant != null\n\n    /** A promoted ongoing notification stays pinned exactly while Android reports its key active. */\n    private fun isPinnedLiveUpdate(): Boolean {\n        val event = currentEvent.value ?: return false\n        if (!event.isLiveUpdate) return false\n        val key = event.notificationKey ?: return false\n        return key in LiveUpdateBus.activeKeys.value\n    }\n\n    /** Remove a promoted pill/bubble immediately when its source Live Update ends. */\n    private fun observeLiveUpdates() = scope.launch {\n        LiveUpdateBus.activeKeys.collect { active ->\n            val last = lastLiveUpdateEvent\n            if (last != null) {\n                val key = last.notificationKey\n                if (key == null || key !in active) {\n                    lastLiveUpdateEvent = null\n                    if (satelliteEvent.value?.notificationKey == key) clearSatellite()\n                    if (currentEvent.value?.isLiveUpdate == true &&\n                        currentEvent.value?.notificationKey == key\n                    ) {\n                        dismissIsland()\n                    }\n                }\n            }\n            pruneSatellite()\n        }\n    }\n\n    /** The lock cutout should stay pinned up (no auto-dismiss) while the device is locked. */\n",
)

replace_once(
    overlay,
    "    private fun isPinnedLiveTile(): Boolean = isPinnedMusic() || isPinnedCall() || isPinnedTimer() || isPinnedAssistant() || isPinnedLock()\n",
    "    private fun isPinnedLiveTile(): Boolean =\n        isPinnedMusic() || isPinnedCall() || isPinnedTimer() || isPinnedAssistant() ||\n            isPinnedLiveUpdate() || isPinnedLock()\n",
)

replace_once(
    overlay,
    "            bubble.media != null -> !musicPlaying\n            bubble.call != null -> !callActive\n",
    "            bubble.isLiveUpdate -> bubble.notificationKey?.let { it !in LiveUpdateBus.activeKeys.value } ?: true\n            bubble.media != null -> !musicPlaying\n            bubble.call != null -> !callActive\n",
)

replace_once(
    overlay,
    "                        is CutoutSignal.Timer -> {\n                            timerActive = true\n                            lastTimerEvent = resolvedEvent\n                        }\n                        is CutoutSignal.System -> {\n",
    "                        is CutoutSignal.Timer -> {\n                            timerActive = true\n                            lastTimerEvent = resolvedEvent\n                        }\n                        is CutoutSignal.Notification -> {\n                            if (signal.isLiveUpdate) lastLiveUpdateEvent = resolvedEvent\n                        }\n                        is CutoutSignal.System -> {\n",
)

replace_once(
    overlay,
    "            if (signal is CutoutSignal.Notification && signal.key != null &&\n                existing != null && existing.notificationKey == signal.key\n            ) {\n                currentEvent.value = resolvedEvent.copy(id = existing.id)\n                syncWindowSize()\n                scheduleDismiss()\n                return@collect\n            }\n",
    "            if (signal is CutoutSignal.Notification && signal.key != null &&\n                existing != null && existing.notificationKey == signal.key\n            ) {\n                currentEvent.value = resolvedEvent.copy(id = existing.id)\n                if (signal.isLiveUpdate) {\n                    lastLiveUpdateEvent = currentEvent.value\n                    dismissJob?.cancel()\n                    currentDeadlineMs = null\n                } else {\n                    scheduleDismiss()\n                }\n                syncWindowSize()\n                return@collect\n            }\n",
)

replace_once(
    overlay,
    "                is CutoutSignal.System -> {\n                    if (signal.type == SystemEventType.DEVICE_LOCKED) {\n                        isDeviceLocked = true\n                        lastLockEvent = resolvedEvent\n                        dismissJob?.cancel()\n                    } else if (signal.type == SystemEventType.DEVICE_UNLOCKED) {\n                        isDeviceLocked = false\n                        lastLockEvent = null\n                        scheduleDismiss()\n                    } else {\n                        scheduleDismiss()\n                    }\n                }\n\n                else -> scheduleDismiss()\n",
    "                is CutoutSignal.System -> {\n                    if (signal.type == SystemEventType.DEVICE_LOCKED) {\n                        isDeviceLocked = true\n                        lastLockEvent = resolvedEvent\n                        dismissJob?.cancel()\n                    } else if (signal.type == SystemEventType.DEVICE_UNLOCKED) {\n                        isDeviceLocked = false\n                        lastLockEvent = null\n                        scheduleDismiss()\n                    } else {\n                        scheduleDismiss()\n                    }\n                }\n\n                is CutoutSignal.Notification -> {\n                    if (signal.isLiveUpdate) {\n                        lastLiveUpdateEvent = resolvedEvent\n                        dismissJob?.cancel()\n                        currentDeadlineMs = null\n                    } else {\n                        scheduleDismiss()\n                    }\n                }\n",
)

replace_once(
    overlay,
    "    private fun livePillToReturnTo(): IslandEvent? =\n        callPillToReturnTo() ?: musicPillToReturnTo() ?: timerPillToReturnTo() ?: lockPillToReturnTo()\n",
    "    private fun livePillToReturnTo(): IslandEvent? =\n        callPillToReturnTo() ?: liveUpdatePillToReturnTo() ?: musicPillToReturnTo() ?:\n            timerPillToReturnTo() ?: lockPillToReturnTo()\n",
)

replace_once(
    overlay,
    "    private fun isLiveTileEvent(event: IslandEvent?): Boolean = event?.let {\n        it.media != null || it.call != null || it.timer != null || (isDeviceLocked && it.id == lastLockEvent?.id)\n    } == true\n",
    "    private fun isLiveTileEvent(event: IslandEvent?): Boolean = event?.let {\n        it.media != null || it.call != null || it.timer != null ||\n            (it.isLiveUpdate && it.notificationKey?.let { key -> key in LiveUpdateBus.activeKeys.value } == true) ||\n            (isDeviceLocked && it.id == lastLockEvent?.id)\n    } == true\n",
)

replace_once(
    overlay,
    "    /**\n     * The music pill to fall back to once a transient pill is done, or null when music shouldn't be\n     * showing.\n     */\n    private fun musicPillToReturnTo(): IslandEvent? {\n",
    "    /** The promoted ongoing pill to restore after a transient interruption. */\n    private fun liveUpdatePillToReturnTo(): IslandEvent? {\n        if (showingLiveTile()) return null\n        val event = lastLiveUpdateEvent ?: return null\n        val key = event.notificationKey ?: return null\n        if (key !in LiveUpdateBus.activeKeys.value) return null\n        if (event.packageName in disabledApps) return null\n        return event.copy(initiallyExpanded = false)\n    }\n\n    /**\n     * The music pill to fall back to once a transient pill is done, or null when music shouldn't be\n     * showing.\n     */\n    private fun musicPillToReturnTo(): IslandEvent? {\n",
)

print("Live Update P2 patch applied successfully")
