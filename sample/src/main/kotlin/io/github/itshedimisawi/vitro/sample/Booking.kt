package io.github.itshedimisawi.vitro.sample

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CastMember(val name: String, val role: String)

object Movie {
    const val Title = "Severance"
    const val Details = "55 min • Thriller, Drama"
    const val Rating = "8.7"
    const val Synopsis = "Mark leads a team of office workers whose memories have been surgically divided " +
        "between their work and personal lives. When a mysterious colleague appears outside of work, " +
        "it begins a journey to discover the truth about their jobs."
    val Cast = listOf(
        CastMember("Adam Scott", "Mark S."),
        CastMember("Britt Lower", "Helly R."),
        CastMember("Zach Cherry", "Dylan G."),
        CastMember("John Turturro", "Irving B."),
        CastMember("Patricia Arquette", "Harmony Cobel"),
    )
    const val Place = "Cinema Town"
    const val Order = "467346347"
    const val RatingCount = "12,480 ratings"
    val Reviews = listOf(
        Review("Unsettling in the best way", 5, "elevator_ride", "Every episode peels back another layer. The office has never looked this sinister or this beautiful."),
        Review("Waffle party worthy", 5, "macrodata_fan", "Sharp writing, an uncanny score and a cast that makes the strangest moments feel completely real."),
        Review("Slow, then impossible to stop", 4, "outie_mark", "The first hour asks for patience. By the finale I could not look away from the screen."),
    )
    val Information = listOf(
        "Genre" to "Thriller, Drama",
        "Released" to "2022",
        "Rated" to "TV-MA",
        "Created by" to "Dan Erickson",
        "Directed by" to "Ben Stiller, Aoife McArdle",
        "Studio" to "Fifth Season",
        "Original audio" to "English",
        "Subtitles" to "English, French, German, Spanish",
    )
}

data class Review(val title: String, val stars: Int, val author: String, val text: String)

const val SeatPrice = 10

val ShowTimes = listOf("9:00 PM", "10:35 PM", "11:00 PM", "11:25 PM", "11:50 PM")

/** Seats per row, front to back, and the seats already sold in each, numbered from 1. */
val SeatRows = listOf(9, 8, 9, 10, 11, 10, 12)
private val TakenSeats = listOf(
    emptySet(),
    emptySet(),
    setOf(5, 6, 7),
    setOf(4, 5, 6),
    setOf(7, 8),
    setOf(4, 5, 7, 8),
    setOf(6, 7),
)

/** A seat as one number, so a selection can be saved as a plain list. Rows and seats count from 1. */
fun seatId(row: Int, seat: Int): Int = row * 100 + seat

fun isTaken(row: Int, seat: Int): Boolean = seat in TakenSeats[row - 1]

/** The next [count] days: "Today", "Tomorrow", then the date. */
fun showDays(count: Int = 7): List<String> {
    val format = SimpleDateFormat("d MMM", Locale.getDefault())
    val day = Calendar.getInstance()
    return List(count) { index ->
        val label = when (index) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> format.format(day.time)
        }
        day.add(Calendar.DAY_OF_YEAR, 1)
        label
    }
}

/** The full date of the day [offset] days from today, as printed on the ticket. */
fun ticketDate(offset: Int): String {
    val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(day.time)
}

/** "Row 5, Seats 5, 6", with one line per row when the seats span several. */
fun describeSeats(seats: List<Int>): String =
    seats.sorted().groupBy { it / 100 }.entries.joinToString("\n") { (row, inRow) ->
        val numbers = inRow.joinToString(", ") { (it % 100).toString() }
        "Row $row, ${if (inRow.size == 1) "Seat" else "Seats"} $numbers"
    }

fun ticketCount(count: Int): String = if (count == 1) "for 1 ticket" else "for $count tickets"
