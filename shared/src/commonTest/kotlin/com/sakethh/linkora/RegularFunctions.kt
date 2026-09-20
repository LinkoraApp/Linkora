package com.sakethh.linkora

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.sakethh.linkora.utils.epochToReadableDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test

class RegularFunctions {
    // parameterized test will make more sense here,
    // the burst lib is cool
    // but i don't want it to test with every possible combination
    @Test
    fun `epoch converts to correct human readable string`() {
        val longActualMap =
            mapOf(
                1698393200L to "27 October 2023, 07:53:20 AM",
                1698393350L to "27 October 2023, 07:55:50 AM",
                1698393415L to "27 October 2023, 07:56:55 AM",
                1698393480L to "27 October 2023, 07:58:00 AM",
                1698380800L to "27 October 2023, 04:26:40 AM",
                1767095454L to "30 December 2025, 11:50:54 AM",
            )
        longActualMap.forEach { (epoch, dateTimeString) ->
            println("Testing $epoch")
            val actualValue = epochToReadableDateTime(epoch, TimeZone.UTC)
            assertThat(actualValue).isEqualTo(dateTimeString)
        }
    }
}
