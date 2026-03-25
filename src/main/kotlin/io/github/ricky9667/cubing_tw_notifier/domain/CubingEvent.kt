package io.github.ricky9667.cubing_tw_notifier.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
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
    @Column(nullable = false)
    var startDate: LocalDate, // Extracts just "2025-12-19" for logic/sorting
    var registrationTime: LocalDateTime? = null,
    @Column(nullable = false)
    var isCreatedNotified: Boolean = false,
    @Column(nullable = false)
    var isRegistrationNotified: Boolean = false,
    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    var isStartNotified: Boolean = false,
)
