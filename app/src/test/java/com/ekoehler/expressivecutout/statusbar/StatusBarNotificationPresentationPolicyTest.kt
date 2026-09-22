package com.ekoehler.expressivecutout.statusbar

import com.ekoehler.expressivecutout.data.StatusBarNotificationDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StatusBarNotificationPresentationPolicyTest {
    private fun entry(
        key: String,
        packageName: String = "app.$key",
        postTime: Long = 1L,
        rank: Int? = null,
        group: Boolean = false,
    ) = StatusBarNotificationEntry(
        key = key,
        packageName = packageName,
        postTime = postTime,
        rank = rank,
        isGroupSummary = group,
    )

    @Test
    fun `dot mode shows one dot for any nonzero eligible count`() {
        val one = resolve(listOf(entry("a")), StatusBarNotificationDisplayMode.DOT)
        val many = resolve(listOf(entry("a"), entry("b"), entry("c")), StatusBarNotificationDisplayMode.DOT)
        assertTrue(one.showDot)
        assertTrue(many.showDot)
        assertTrue(one.entries.isEmpty())
        assertTrue(many.entries.isEmpty())
    }

    @Test
    fun `dot hides when even its own width cannot fit`() {
        val result = resolve(
            listOf(entry("a")),
            StatusBarNotificationDisplayMode.DOT,
            width = 2,
        )
        assertFalse(result.showDot)
    }

    @Test
    fun `hidden mode renders nothing`() {
        val result = resolve(listOf(entry("a")), StatusBarNotificationDisplayMode.HIDDEN)
        assertFalse(result.showDot)
        assertFalse(result.showOverflow)
        assertTrue(result.entries.isEmpty())
    }

    @Test
    fun `filters own summaries and represented live activity keys`() {
        val result = resolve(
            listOf(
                entry("own", packageName = "com.test"),
                entry("summary", group = true),
                entry("live"),
                entry("visible"),
            ),
            StatusBarNotificationDisplayMode.ICONS,
            represented = setOf("live"),
        )
        assertEquals(listOf("visible"), result.entries.map { it.key })
    }

    @Test
    fun `ranking wins then post time breaks ties`() {
        val result = resolve(
            listOf(
                entry("late", postTime = 30, rank = 2),
                entry("first", postTime = 10, rank = 0),
                entry("newerTie", postTime = 40, rank = 2),
            ),
            StatusBarNotificationDisplayMode.ICONS,
        )
        assertEquals(listOf("first", "newerTie", "late"), result.entries.map { it.key })
    }

    @Test
    fun `icons reserve overflow indicator when width is narrow`() {
        val result = resolve(
            listOf(entry("a"), entry("b"), entry("c"), entry("d")),
            StatusBarNotificationDisplayMode.ICONS,
            width = 58,
        )
        assertEquals(2, result.entries.size)
        assertTrue(result.showOverflow)
    }

    private fun resolve(
        entries: List<StatusBarNotificationEntry>,
        mode: StatusBarNotificationDisplayMode,
        represented: Set<String> = emptySet(),
        width: Int = 400,
    ) = StatusBarNotificationPresentationPolicy.resolve(
        entries = entries,
        mode = mode,
        ownPackageName = "com.test",
        representedNotificationKeys = represented,
        availableWidthPx = width,
        iconWidthPx = 18,
        spacingPx = 4,
        overflowWidthPx = 6,
    )
}

class StatusBarNotificationStateTest {
    @Before
    fun reset() = StatusBarNotificationStore.clear()

    @Test
    fun `upsert replaces same key without duplication`() {
        StatusBarNotificationStore.upsert(StatusBarNotificationEntry("a", "one", 1))
        StatusBarNotificationStore.upsert(StatusBarNotificationEntry("a", "two", 2))
        assertEquals(1, StatusBarNotificationStore.entries.value.size)
        assertEquals("two", StatusBarNotificationStore.entries.value.single().packageName)
    }

    @Test
    fun `replace remove rank and clear are deterministic`() {
        StatusBarNotificationStore.replaceAll(
            listOf(
                StatusBarNotificationEntry("a", "a", 1),
                StatusBarNotificationEntry("b", "b", 2),
            ),
        )
        StatusBarNotificationStore.updateRanks(mapOf("b" to 0, "a" to 1))
        assertEquals(0, StatusBarNotificationStore.entries.value.first { it.key == "b" }.rank)
        StatusBarNotificationStore.remove("a")
        assertEquals(listOf("b"), StatusBarNotificationStore.entries.value.map { it.key })
        StatusBarNotificationStore.clear()
        assertTrue(StatusBarNotificationStore.entries.value.isEmpty())
    }
}
