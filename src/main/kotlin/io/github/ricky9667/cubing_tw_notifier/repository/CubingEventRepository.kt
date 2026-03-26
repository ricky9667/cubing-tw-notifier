package io.github.ricky9667.cubing_tw_notifier.repository

import io.github.ricky9667.cubing_tw_notifier.domain.CubingEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface CubingEventRepository : JpaRepository<CubingEvent, Long> {
    fun existsByUrl(url: String): Boolean

    fun findByUrl(url: String): CubingEvent?

    fun findByRegistrationTimeLessThanEqualAndIsRegistrationNotifiedFalse(registrationTime: LocalDateTime): List<CubingEvent>

    fun findByStartDateAndIsStartNotifiedFalse(startDate: LocalDate): List<CubingEvent>
}
