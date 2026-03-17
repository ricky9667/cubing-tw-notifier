package io.github.ricky9667.cubing_tw_notifier

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CubingTwNotifierApplication

fun main(args: Array<String>) {
	runApplication<CubingTwNotifierApplication>(*args)
}
