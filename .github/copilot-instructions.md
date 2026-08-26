# FastSpire Copilot Instructions

## Project Overview
**FastSpire** is an Android intermittent fasting tracker app built with **Jetpack Compose** and **Kotlin**. The app helps users monitor fasting duration, track weight changes, view statistics, and manage fasting sessions.

### Core Features
- Real-time fasting timer with circular progress indicator
- Flexible start/end controls (users can end fast at any time)
- Weight tracking and progress chart
- Dashboard with fasting activity stats (week/month/year views)
- Fasting history with delete functionality
- User profile setup and settings

---

## Architecture & Code Style

### Project Structure
```
app/src/main/java/com/example/myfast/
├── presentation/
│   ├── screens/
│   │   ├── HomeScreen.kt          (Fasting timer & plan selection)
│   │   ├── StatsScreen.kt         (Dashboard with charts)
│   │   ├── HistoryScreen.kt       (Fasting history)
│   │   ├── SettingsScreen.kt      (User profile)
│   │   ├── SetupScreen.kt         (First-time setup)
│   │   └── PremiumFastingProgressCircle.kt  (Circular timer)
│   └── Navigation.kt
├── data/
│   ├── FastingRepository.kt       (Fasting data persistence)
│   ├── WeightRepository.kt        (Weight logs)
│   └── UserProfileRepository.kt   (User settings)
└── MainActivity.kt
```

### Kotlin & Compose Conventions
- **Use Kotlin idioms**: `let`, `also`, `remember`, `mutableStateOf` for state management
- **Prefer Compose functions** over imperative Android APIs
- **Memoization**: Use `remember()` blocks to prevent unnecessary recomposition
  - Example: Time formatting with `remember(fastStartTime)` to prevent blinking
- **Type safety**: Always use proper types; no raw `Any` or unchecked casts
- **No comments** unless the logic needs significant clarification

### UI/UX Patterns
- **Material3 theme**: All colors use `MaterialTheme.colorScheme`
- **Consistent spacing**: Use standard `dp` values (8.dp, 12.dp, 16.dp, etc.)
- **Card-based layouts**: Wrap grouped content in `Card()` with `RoundedCornerShape(12.dp)`
- **Typography**: Use semantic font sizes (28.sp for titles, 18.sp for sections, 14.sp for body)
- **Dark mode support**: Colors automatically adapt via Material3

---

## Date & Time Handling

### Standards
- **Primary type**: `LocalDateTime` (from `java.time`)
- **Formatting**: Use `DateTimeFormatter.ofPattern()` for all date/time displays
- **Timezone**: Always use `ZoneId.systemDefault()` when converting from epoch milliseconds
- **Memoization**: Wrap time calculations in `remember()` to prevent UI flicker

### Common Patterns
```kotlin
// Epoch to LocalDateTime
val startDateTime = Instant.ofEpochMilli(fastStartTime)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()

// Formatting with relative day labels
val dayLabel = when (date.toLocalDate()) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
}
```

---

## Data Persistence

### SharedPreferences Usage
- Store all app data in SharedPreferences (no database yet)
- Use descriptive keys: `fast_records`, `weight_logs`, `user_profile`
- Serialize/deserialize using pipe-delimited format (see `FastingRepository.kt`)
- Always include `null` checks when reading from prefs

### FastRecord Data Model
```kotlin
data class FastRecord(
    val id: Int,
    val plan: String,           // e.g., "16:8", "18:6"
    val date: LocalDateTime,    // Start time
    val durationSeconds: Int,   // Total fasting duration
    val isOngoing: Boolean = false
)
```

---

## Recent Features & Patterns

### 1. Day Labels on Active Fast (Aug 26, 2026)
When displaying start/end times for an ongoing fast, include relative day labels:
```
"Started at: Today, 06:30"  or  "Yesterday, 20:45"  or  "Aug 25, 2025, 18:00"
"Ends at: Tomorrow, 22:30"
```

### 2. Fasting Activity Charts (Dashboard)
Three chart ranges with smart bucketing:

| Range  | Granularity | Buckets | Label Format |
|--------|-------------|---------|--------------|
| Week   | 1 day       | 7 days  | "Aug 20", "Aug 21" |
| Month  | 1 week      | Last 28 days (or since first fast) | First day of week: "Aug 18" |
| Year   | 1 month     | 12 months back (or since first fast month if same year) | Month abbreviation: "Aug", "Sep" |

- Year view stops at current month (no future months)
- Month view stops at today (no incomplete weeks beyond today)
- All aggregations correctly split durations across midnight boundaries

### 3. Two-Step End Fast Dialog
1. **Confirmation Step**: "End Fast?" with Cancel/End buttons
2. **Save/Delete Step**: Shows duration and offers Save/Delete options
- Prevents accidental data loss
- Users can cancel at any point

---

## Build & Testing

### Build Commands
```bash
# Compile Kotlin sources
./gradlew compileDebugKotlin --no-daemon

# Full debug build
./gradlew assembleDebug --no-daemon

# Run tests
./gradlew test --no-daemon
```

### Target SDK
- Minimum SDK: 26 (Android 8.0)
- Target SDK: 34 (Android 14)
- Compile SDK: 34

### APK Generation
- Debug APKs are generated at `build/outputs/apk/debug/`
- Use `./gradlew assembleDebug` for installable APK

---

## Git Workflow

### Commit Messages
Include a clear, descriptive title and reference the feature/fix:
```
Add fasting activity charts to Dashboard

- Add Week/Month/Year range toggles
- Show daily totals for week, weekly totals for month, monthly totals for year
- Include day labels on active fast screen (Today/Yesterday/Tomorrow)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
```

### Branch Naming
Use descriptive names prefixed with username or feature type:
```
feature/fasting-charts
fix/timer-blinking
refactor/time-formatting
```

---

## Known Warnings & Cleanup

### Unused Variables/Parameters
Some compiler warnings are acceptable for now (marked as ignored):
- Unused route parameters in composables
- Unused color variables from theme

Fix only if refactoring related code.

---

## Testing Strategy

### Unit Tests (Future)
- Test time calculations and date aggregations
- Validate FastRecord serialization/deserialization
- Test chart bucket generation for edge cases (month boundaries, year transitions)

### Manual Testing Checklist
- [ ] Start/end times display without blinking
- [ ] End Fast button works at any elapsed time
- [ ] Save/Delete dialog flow prevents accidental data loss
- [ ] Dashboard charts aggregate correctly across days/weeks/months
- [ ] Settings persist without navigation away
- [ ] First-time users see welcome message

---

## Performance Tips

### Recomposition Prevention
- Always memoize expensive calculations with `remember()`
- Use `rememberInfiniteTransition()` for animations (pulsing timer)
- Limit state updates to only what changed

### Memory Management
- SharedPreferences queries are lightweight but cache the result in `remember { }`
- Avoid recreating formatters; use `DateTimeFormatter.ofPattern()` and cache if used multiple times

---

## Future Enhancements

- Persistent database (Room) instead of SharedPreferences
- Push notifications for milestones
- Multiple active fasts (family tracking)
- Receipt/warranty scanning integration
- Enhanced animations and haptic feedback
- Offline/cloud sync capabilities

---

*Last updated: August 26, 2026*
