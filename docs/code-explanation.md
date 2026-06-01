# Code Explanation

## AuthManager

`AuthManager` stores local account data and the signed-in flag in SharedPreferences. `createAccount()` saves credentials without automatically signing in. `login()` validates email and password, then sets `signed_in` to true.

## BudgetManager

`BudgetManager` centralizes budget persistence and spending calculations. It stores `budget_amount` and `budget_period` in `expense_data`. It parses saved records, detects whether a record is in the current daily/weekly/monthly/yearly period, and returns current-period spending.

## CategoryManager

`CategoryManager` owns default and custom categories. It sanitizes AI category output, stores new categories, reads categories found in history records, and exposes the category list used by the Add screen and History filters.

## MainActivity

`MainActivity` is the expense logging screen. It builds a compact Gemini prompt, requests JSON only, extracts a JSON object from the model response, and saves the expense. If Gemini fails, it uses `fallbackExpense()` with local amount and category detection.

Important behavior:

- Amount parsing chooses the money amount, including shorthand such as `1million` and comma amounts.
- Category parsing supports common local fallbacks such as food, transport, shopping, bills, investments, and loans.
- Gemini failures do not block logging. The app saves using local parsing and shows a short fallback message.

## DashboardActivity

`DashboardActivity` reads saved history and budget settings. It shows spending for the active budget period, recent transactions, and a last-7-days chart. Chart bars are calculated from actual record dates and amounts.

## HistoryActivity

`HistoryActivity` loads all saved expense records, renders dynamic filter chips, filters by category and search text, and displays recent transaction rows. It also builds monthly summaries from saved record dates.

## InsightsActivity

`InsightsActivity` initially renders local insights, then attempts Gemini-generated insights in the background. It sends a compact summary instead of raw history to reduce token usage and protect against unnecessary API cost.

## ProfileActivity

`ProfileActivity` displays user information and app settings. The Budget row opens a dialog with an amount field and period selector, then saves the result through `BudgetManager`.

## Data Format

Expense records are stored as plain text lines:

```text
dd MMM yyyy, HH:mm | amount TZS | category | original text
```

Example:

```text
01 Jun 2026, 09:15 | 1000000 TZS | Investments | bought shares from dse worth 1million
```
