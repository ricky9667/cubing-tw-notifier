package io.github.ricky9667.cubing_tw_notifier.domain

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "cubing_events")
class CubingEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val url: String, // We use the URL as a unique identifier to prevent duplicates

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var eventDate: String, // e.g., "2026-04-15 ~ 2026-04-16"

    @Column(name = "start_date")
    var startDate: LocalDate, // Extracts just "2025-12-19" for logic/sorting

    @Column(name = "registration_time")
    var registrationTime: LocalDateTime? = null,

    @Column(name = "is_created_notified", nullable = false)
    var isCreatedNotified: Boolean = false,

    @Column(name = "is_registration_notified", nullable = false)
    var isRegistrationNotified: Boolean = false
)
