package com.example.smartexpense;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class BudgetManager {

    static final String PERIOD_DAILY = "Daily";
    static final String PERIOD_WEEKLY = "Weekly";
    static final String PERIOD_MONTHLY = "Monthly";
    static final String PERIOD_YEARLY = "Yearly";

    private static final String PREFS_NAME = "expense_data";
    private static final String KEY_BUDGET_AMOUNT = "budget_amount";
    private static final String KEY_BUDGET_PERIOD = "budget_period";
    private static final String KEY_HISTORY = "history";
    private static final int DEFAULT_BUDGET_AMOUNT = 200000;

    private BudgetManager() {
    }

    static int getBudgetAmount(Context context) {
        return getPrefs(context).getInt(KEY_BUDGET_AMOUNT, DEFAULT_BUDGET_AMOUNT);
    }

    static String getBudgetPeriod(Context context) {
        return getPrefs(context).getString(KEY_BUDGET_PERIOD, PERIOD_MONTHLY);
    }

    static void saveBudget(Context context, int amount, String period) {
        getPrefs(context).edit()
                .putInt(KEY_BUDGET_AMOUNT, amount)
                .putString(KEY_BUDGET_PERIOD, period)
                .apply();
    }

    static int getCurrentPeriodSpending(Context context) {
        String history = getPrefs(context).getString(KEY_HISTORY, "");
        String[] lines = history.trim().isEmpty() ? new String[0] : history.split("\\n");
        String period = getBudgetPeriod(context);
        int total = 0;

        for (String line : lines) {
            String record = line.trim();
            if (!record.isEmpty() && isInCurrentPeriod(record, period)) {
                total += parseAmount(record);
            }
        }

        return total;
    }

    static String getBudgetSummary(Context context) {
        return String.format(
                Locale.getDefault(),
                "%,d TZS / %s",
                getBudgetAmount(context),
                getBudgetPeriod(context).toLowerCase(Locale.getDefault())
        );
    }

    static String getPeriodSpendingLabel(Context context) {
        return getBudgetPeriod(context) + " Spending";
    }

    static int parseAmount(String record) {
        String[] parts = record.split("\\|");
        if (parts.length < 2) {
            return 0;
        }

        String amount = parts[1].replace("TZS", "").replace(",", "").trim();
        try {
            return Integer.parseInt(amount);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static Date parseRecordDate(String record) {
        String[] parts = record.split("\\|");
        if (parts.length == 0) {
            return null;
        }

        String value = parts[0].trim();
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        try {
            return format.parse(value);
        } catch (ParseException e) {
            try {
                return new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH).parse(value);
            } catch (ParseException ignored) {
                return null;
            }
        }
    }

    static boolean isInCurrentPeriod(String record, String period) {
        Date recordDate = parseRecordDate(record);
        if (recordDate == null) {
            return false;
        }

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        if (PERIOD_WEEKLY.equals(period)) {
            start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
        } else if (PERIOD_MONTHLY.equals(period)) {
            start.set(Calendar.DAY_OF_MONTH, 1);
        } else if (PERIOD_YEARLY.equals(period)) {
            start.set(Calendar.DAY_OF_YEAR, 1);
        }

        Calendar end = (Calendar) start.clone();
        if (PERIOD_DAILY.equals(period)) {
            end.add(Calendar.DAY_OF_YEAR, 1);
        } else if (PERIOD_WEEKLY.equals(period)) {
            end.add(Calendar.WEEK_OF_YEAR, 1);
        } else if (PERIOD_MONTHLY.equals(period)) {
            end.add(Calendar.MONTH, 1);
        } else {
            end.add(Calendar.YEAR, 1);
        }

        long time = recordDate.getTime();
        return time >= start.getTimeInMillis() && time < end.getTimeInMillis();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
