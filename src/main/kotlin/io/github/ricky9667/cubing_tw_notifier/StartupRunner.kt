package io.github.ricky9667.cubing_tw_notifier

import io.github.ricky9667.cubing_tw_notifier.service.EventCrawlerService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class StartupRunner(
    private val crawlerService: EventCrawlerService
) : CommandLineRunner {
    override fun run(vararg args: String) {
        crawlerService.crawlNewEvents()
    }
}
