package io.github.ricky9667.cubing_tw_notifier.service

import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventCrawlerServiceTest {
    @Test
    fun `uses the page ordinal rather than counting active reopenings`() {
        val document =
            Jsoup.parse(
                """
                <h3>報名時間</h3><p>2026/06/22 (一) 20:00:00 ~ 2026/07/06</p>
                <s><p>第一次重新開放報名時間：2026/06/30 (二) 20:00:00 ~ 2026/07/06</p></s>
                <p>第二次重新開放報名時間：2026/07/06 (一) 20:00:00 ~ 2026/07/06</p>
                """,
            )

        assertEquals(
            RegistrationOpening(LocalDateTime.of(2026, 7, 6, 20, 0), 2),
            EventCrawlerService.parseRegistrationFromDocument(document),
        )
        assertEquals(
            RegistrationOpening(LocalDateTime.of(2026, 6, 22, 20, 0), 0),
            EventCrawlerService.parseRegistrationFromDocument(
                Jsoup.parse("<h3>報名時間</h3><p>2026/06/22 (一) 20:00:00 ~ 2026/07/06</p>"),
            ),
        )
        assertNull(
            EventCrawlerService.parseRegistrationFromDocument(
                Jsoup.parse("<p>第0次重新開放報名時間：2026/07/06 (一) 20:00:00</p>"),
            ),
        )
    }
}
