package io.github.itshedimisawi.vitro.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { ShowcaseApp() }
    }
}

val Ink = Color(0xFF111111)

// Page, cards, fills and secondary text in the iOS system colours. Primary marks a selection and
// secondary is its lighter edge, both from the showcase palette; tertiary is the rating star.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF6F9DB8),
    secondary = Color(0xFF86B4CB),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3A3A3C),
    onSurfaceVariant = Color(0xFF8E8E93),
    tertiary = Color(0xFF86B4CB),
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF2F4D65),
    secondary = Color(0xFF6F9DB8),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFF2F2F7),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFD1D1D6),
    onSurfaceVariant = Color(0xFF8A8A8E),
    tertiary = Color(0xFF2F4D65),
)

enum class Screen { Movie, Seats, Ticket }

@Composable
fun ShowcaseApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.Movie) }
    var day by rememberSaveable { mutableIntStateOf(0) }
    var time by rememberSaveable { mutableIntStateOf(2) }
    var seats by rememberSaveable { mutableStateOf(listOf<Int>()) }

    BackHandler(enabled = screen != Screen.Movie) { screen = Screen.entries[screen.ordinal - 1] }

    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors) {
        Surface(color = colors.background, contentColor = colors.onBackground) {
            ScreenStack(screen) { shown ->
                when (shown) {
                    Screen.Movie -> MovieScreen(onSchedule = { screen = Screen.Seats })
                    Screen.Seats -> SeatsScreen(
                        day = day,
                        onDay = { day = it },
                        time = time,
                        onTime = { time = it },
                        seats = seats,
                        onSeat = { id -> seats = if (id in seats) seats - id else seats + id },
                        onBack = { screen = Screen.Movie },
                        onBuy = { screen = Screen.Ticket },
                    )
                    Screen.Ticket -> TicketScreen(
                        date = ticketDate(day),
                        time = ShowTimes[time],
                        seats = describeSeats(seats),
                        onBack = { screen = Screen.Seats },
                    )
                }
            }
        }
    }
}
