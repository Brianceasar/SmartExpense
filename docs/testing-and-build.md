# Testing and Build Notes

## Build Command

```bash
./gradlew :app:assembleDebug
```

## Manual Test Checklist

1. Sign up with a new account.
2. Confirm the app returns to Login after signup.
3. Log in with the created credentials.
4. Add a budget from Profile.
5. Log expenses with known categories, such as food and transport.
6. Log dynamic-category expenses, such as loans, school fees, or investments.
7. Confirm Dashboard totals and chart update.
8. Confirm History search, filters, transaction rows, and monthly summary update.
9. Open Insights with and without a Gemini API key.
10. Turn off network or trigger API failure and confirm local fallback behavior.

## Logcat Filters

Use the app package:

```text
package:com.example.smartexpense
```

Useful tags:

```text
SmartExpenseAI
SmartExpenseInsights
```

## Known Warnings

The build currently reports deprecated API warnings from Android framework usage. These warnings do not block the debug build.
