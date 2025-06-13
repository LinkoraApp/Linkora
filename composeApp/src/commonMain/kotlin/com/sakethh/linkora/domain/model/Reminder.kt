package com.sakethh.linkora.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DoNotDisturbOnTotalSilence
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable


@Entity(tableName = "reminder")
@Serializable
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val linkId: Long,
    val title: String,
    val description: String,
    val reminderType: Type,
    val reminderMode: Mode,
    val date: Date?,             // only used for ONCE type reminders
    val daysOfWeek: List<String>?, // only used for PERIODIC weekly reminders
    val datesOfMonth: List<Int>?,  // only used for PERIODIC monthly reminders
    val time: Time?,
    val linkView: String         // base64‑encoded "view"
) {
    @Serializable
    data class Date(
        val year: String, val month: String, val dayOfMonth: String
    ) {
        constructor(
            year: Int, month: Int, dayOfMonth: Int
        ) : this(
            year = year.toString(), month = month.toString(), dayOfMonth = dayOfMonth.toString()
        )
    }

    @Serializable
    data class Time(val hour: String, val minute: String, val second: String = "0") {
        constructor(hour: Int, minute: Int, second: Int = 0) : this(
            hour = hour.toString(), minute = minute.toString(), second = second.toString()
        )
    }

    @Serializable
    sealed interface Type {

        @Serializable
        data object ONCE : Type {
            override val imgVector = Icons.Default.LooksOne
        }

        @Serializable
        sealed interface PERIODIC : Type {
            override val imgVector: ImageVector
                get() = Icons.Default.Repeat

            @Serializable
            data object WEEKLY : PERIODIC

            @Serializable
            data object MONTHLY : PERIODIC

            companion object : Type {
                override val imgVector = Icons.Default.Repeat
                override fun toString(): String = "PERIODIC"
            }

        }

        @Serializable
        data object STICKY : Type {
            override val imgVector = Icons.Default.Pin
        }

        val imgVector: ImageVector
    }

    @Serializable
    enum class Mode {
        SILENT {
            override val imgVector: ImageVector = Icons.Default.DoNotDisturbOnTotalSilence
        },
        VIBRATE {
            override val imgVector: ImageVector = Icons.Default.Vibration
        },
        CRUCIAL {
            override val imgVector: ImageVector = Icons.AutoMirrored.Filled.VolumeUp
        }, ;

        abstract val imgVector: ImageVector
    }
}