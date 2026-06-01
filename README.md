# SmartExpense

SmartExpense is an Android expense tracking app for logging Matumizi in plain language. It uses Gemini to extract the amount and category from a short expense description, then falls back to local parsing when AI is unavailable.

## Features

- Local signup and login with stored credentials
- Plain-language expense logging
- Gemini-powered amount and category extraction
- Local fallback parser for offline or overloaded AI requests
- Dynamic categories created from AI results
- Budget setup from the profile screen
- Daily, weekly, monthly, and yearly budget periods
- Dashboard budget tracking and last-7-days spending chart
- History search, category filtering, transaction rows, and monthly summaries
- AI-generated insights from compact spending summaries
- Bottom navigation across dashboard, add, history, insights, and profile

## Tech Stack

- Java
- Android SDK
- AppCompat
- Material Components
- Gradle Kotlin DSL
- SharedPreferences for local persistence
- Gemini REST API via `HttpURLConnection`

## Project Structure

```text
app/src/main/java/com/example/smartexpense/
  AuthManager.java        Local account/session storage
  BudgetManager.java      Budget settings and period spending calculations
  CategoryManager.java    Default, custom, and discovered categories
  MainActivity.java       Expense entry and AI/local parsing
  DashboardActivity.java  Budget dashboard, recent activity, chart
  HistoryActivity.java    Search, filters, transactions, monthly summaries
  InsightsActivity.java   AI/local spending insights
  ProfileActivity.java    User profile, budget editor, logout
```

## Configuration

Set the Gemini API key in `local.properties`:

```properties
GEMINI_API_KEY=your_api_key_here
```

The app still works without this key. Expense logging and insights will use local fallback behavior.

## Build

```bash
./gradlew :app:assembleDebug
```

## Runtime Notes

- Expense records are saved locally in SharedPreferences under `expense_data/history`.
- Auth data is saved locally in SharedPreferences under `auth_data`.
- Current storage is device-local and intended for the current prototype.
- Existing saved records keep their original category unless a future edit/reclassify flow is added.

## Documentation

More details are available in:

- [docs/features.md](docs/features.md)
- [docs/code-explanation.md](docs/code-explanation.md)
- [docs/ai-and-api-notes.md](docs/ai-and-api-notes.md)
- [docs/data-storage.md](docs/data-storage.md)
- [docs/testing-and-build.md](docs/testing-and-build.md)
