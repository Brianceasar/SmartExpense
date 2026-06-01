# AI and API Notes

## Expense Parsing

The expense parser asks Gemini for a single JSON object:

```json
{"amount":"","category":""}
```

The prompt is intentionally short. It includes:

- The compact expected JSON shape
- A reminder that amount means money spent, not item counts
- A capped list of known categories
- A short instruction to create a category if none fit
- The user text truncated to 180 characters

## Dynamic Category Handling

If Gemini returns a category that does not already exist, the app sanitizes and stores it. This allows categories such as Loans, Education, Health, or Subscriptions without shipping a new app build for every possible category.

## Insights Generation

Insights are generated from a compact summary:

- Budget amount and period
- Current period spending
- Recent total
- Category totals from recent records

The app does not send the full transaction list for insights. This keeps token usage lower and avoids unnecessary data transfer.

## Graceful Failure

The app handles common API problems:

- Missing API key
- Invalid or non-JSON model output
- 429 rate limiting
- 503 high demand or temporary model overload
- Network timeouts

When Gemini fails, expense logging uses the local parser and insights keep local fallback cards. The app should not crash or block normal tracking.

## Token Controls

Expense parsing uses `maxOutputTokens` of 80. Insights use `maxOutputTokens` of 180. Both prompts request JSON only and avoid sending unnecessary raw history.

## Future Improvements

- Add retry with exponential backoff for background insight refreshes
- Cache AI insight responses until new transactions are added
- Add an edit/reclassify flow for old records
- Move API calls behind a repository/service class to reduce duplicated HTTP code
