package io.github.ricky9667.cubing_tw_notifier

import io.github.ricky9667.cubing_tw_notifier.service.EventCrawlerService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(properties = ["spring.datasource.url=jdbc:h2:mem:cubing_tw_notifier_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"])
@ActiveProfiles("test")
class CubingTwNotifierApplicationTests {
    @field:MockitoBean
    lateinit var crawlerService: EventCrawlerService

    @Test
    fun contextLoads() {
    }
}
