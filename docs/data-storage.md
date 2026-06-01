# Data Storage

The app currently uses SharedPreferences for local prototype storage.

## Auth Preferences

Preference file: `auth_data`

Keys:

- `name`
- `email`
- `password`
- `signed_in`

The password is currently stored locally as plain text. This is acceptable only for a prototype. A production app should use a secure authentication provider or encrypted local storage.

## Expense Preferences

Preference file: `expense_data`

Keys:

- `history`: newline-separated expense records
- `budget_amount`: selected budget amount
- `budget_period`: Daily, Weekly, Monthly, or Yearly
- `custom_categories`: pipe-separated custom categories discovered from AI or user selection

## Expense Record Format

```text
date | amount TZS | category | description
```

Example:

```text
01 Jun 2026, 09:15 | 1000000 TZS | Investments | bought shares from dse worth 1million
```

## Limitations

- There is no database layer yet.
- Records are append-only.
- Existing records cannot currently be edited or deleted.
- Category changes do not rewrite old records.

## Recommended Next Step

Move expenses, categories, budgets, and users into Room entities once the data model stabilizes. That would make editing, filtering, migrations, and analytics more reliable.
