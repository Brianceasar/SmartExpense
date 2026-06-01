# Feature Notes

## Authentication

Users create a local account with name, email, and password. After signup, the user is returned to the login screen and must sign in with the created credentials. Login creates an active local session, and logout clears only the signed-in flag.

## Expense Logging

The Add screen accepts natural language such as `spent 15000 on lunch` or `bought shares from DSE worth 1 million`. When Gemini is available, the app asks for a compact JSON response with `amount` and `category`. If Gemini is unavailable, overloaded, or returns unusable output, the local parser extracts the amount and category.

## Dynamic Categories

The app starts with default categories such as Food, Transport, Shopping, Bills, Investments, Loans, and General. Gemini can return a short new category when none of the defaults fit. New categories are stored and reused in the category override spinner and history filter chips.

## Budgeting

Users can add or edit a budget from the Profile screen. A budget has an amount and a period: Daily, Weekly, Monthly, or Yearly. Dashboard and History use the selected period to calculate current spending, remaining budget, and percent used.

## Dashboard

The Dashboard shows spending for the active budget period, remaining budget, percent used, recent activity, and a last-7-days chart. The chart reads actual saved expense records rather than static placeholder values.

## History

History includes search, dynamic category filters, a current budget-period summary, recent transactions, and a monthly spending breakdown. The monthly breakdown groups saved records by calendar month so users can review older spending even when the active budget period is different.

## Insights

Insights are generated from a compact summary of spending data, not full raw transaction history. The app sends budget information, current-period spending, recent total, and category totals to Gemini. If AI is unavailable, it shows local fallback insights.

## Profile

Profile shows user information, budget settings, bank/security/notification rows, and logout. The budget row opens an edit dialog for amount and period.
