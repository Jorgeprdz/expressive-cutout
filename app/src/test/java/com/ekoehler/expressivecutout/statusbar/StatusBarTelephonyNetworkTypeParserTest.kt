package com.ekoehler.expressivecutout.statusbar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatusBarTelephonyNetworkTypeParserTest {

    @Test
    fun `5g display override wins over LTE base network`() {
        val raw = """
            last known state:
              Phone Id=0
                mTelephonyDisplayInfo=TelephonyDisplayInfo {network=LTE, overrideNetwork=NR_NSA, isRoaming=false}
                mIsDataEnabled=true
              mDefaultPhoneId=0
        """.trimIndent()

        assertEquals(StatusBarNetworkType.FIVE_G, StatusBarTelephonyNetworkTypeParser.parse(raw))
    }

    @Test
    fun `LTE carrier aggregation and advanced pro map to 4g plus`() {
        assertEquals(
            StatusBarNetworkType.FOUR_G_PLUS,
            StatusBarTelephonyNetworkTypeParser.parse(
                "Phone Id=0\n mTelephonyDisplayInfo=TelephonyDisplayInfo {network=LTE, overrideNetwork=LTE_CA}\n mDefaultPhoneId=0",
            ),
        )
        assertEquals(
            StatusBarNetworkType.FOUR_G_PLUS,
            StatusBarTelephonyNetworkTypeParser.parse(
                "Phone Id=0\n mTelephonyDisplayInfo=TelephonyDisplayInfo {network=LTE, overrideNetwork=LTE_ADV_PRO}\n mDefaultPhoneId=0",
            ),
        )
    }

    @Test
    fun `native NR maps to 5g and LTE maps to LTE`() {
        assertEquals(
            StatusBarNetworkType.FIVE_G,
            StatusBarTelephonyNetworkTypeParser.parse(
                "Phone Id=0\n mTelephonyDisplayInfo=TelephonyDisplayInfo {network=NR, overrideNetwork=NONE}\n mDefaultPhoneId=0",
            ),
        )
        assertEquals(
            StatusBarNetworkType.LTE,
            StatusBarTelephonyNetworkTypeParser.parse(
                "Phone Id=0\n mTelephonyDisplayInfo=TelephonyDisplayInfo {network=LTE, overrideNetwork=NONE}\n mDefaultPhoneId=0",
            ),
        )
    }

    @Test
    fun `hspa family maps to four g while legacy edge stays absent`() {
        assertEquals(
            StatusBarNetworkType.FOUR_G,
            StatusBarTelephonyNetworkTypeParser.parse(
                "Phone Id=0\n mTelephonyDisplayInfo=TelephonyDisplayInfo {network=HSPAP, overrideNetwork=NONE}\n mDefaultPhoneId=0",
            ),
        )
        assertNull(
            StatusBarTelephonyNetworkTypeParser.parse(
                "Phone Id=0\n mTelephonyDisplayInfo=TelephonyDisplayInfo {network=EDGE, overrideNetwork=NONE}\n mDefaultPhoneId=0",
            ),
        )
    }

    @Test
    fun `default phone wins when multiple modem blocks are present`() {
        val raw = """
            last known state:
              Phone Id=0
                mTelephonyDisplayInfo=TelephonyDisplayInfo {network=LTE, overrideNetwork=NONE}
                mIsDataEnabled=true
              Phone Id=1
                mTelephonyDisplayInfo=TelephonyDisplayInfo {network=NR, overrideNetwork=NONE}
                mIsDataEnabled=true
              mDefaultPhoneId=1
        """.trimIndent()

        assertEquals(StatusBarNetworkType.FIVE_G, StatusBarTelephonyNetworkTypeParser.parse(raw))
    }

    @Test
    fun `data enabled modem is fallback when default phone is unavailable`() {
        val raw = """
            Phone Id=0
              mTelephonyDisplayInfo=TelephonyDisplayInfo {network=UNKNOWN, overrideNetwork=NONE}
              mIsDataEnabled=false
            Phone Id=1
              mTelephonyDisplayInfo=TelephonyDisplayInfo {network=LTE, overrideNetwork=NONE}
              mIsDataEnabled=true
            mDefaultPhoneId=-1
        """.trimIndent()

        assertEquals(StatusBarNetworkType.LTE, StatusBarTelephonyNetworkTypeParser.parse(raw))
    }

    @Test
    fun `malformed or missing display info degrades to null`() {
        assertNull(StatusBarTelephonyNetworkTypeParser.parse(""))
        assertNull(StatusBarTelephonyNetworkTypeParser.parse("mTelephonyDisplayInfo=null"))
    }
}
