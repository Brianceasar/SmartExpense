package com.example.smartexpense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "expense_data";
    private static final String KEY_HISTORY = "history";

    private TextView totalLabelText;
    private TextView totalSpentText;
    private TextView remainingBudgetText;
    private TextView budgetUsedText;
    private ProgressBar budgetProgress;
    private LinearLayout recentActivityList;
    private BottomNavigationView bottomNavigation;
    private TextView[] chartBars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthManager.isSignedIn(this)) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_dashboard);
        TopMenu.attach(this);

        totalLabelText = (TextView) findViewById(R.id.dashboardTotalLabel);
        totalSpentText = (TextView) findViewById(R.id.totalSpentText);
        remainingBudgetText = (TextView) findViewById(R.id.remainingBudgetText);
        budgetUsedText = (TextView) findViewById(R.id.budgetUsedText);
        budgetProgress = (ProgressBar) findViewById(R.id.budgetProgress);
        recentActivityList = (LinearLayout) findViewById(R.id.recentActivityList);
        bottomNavigation = (BottomNavigationView) findViewById(R.id.bottomNavigation);
        chartBars = new TextView[]{
                (TextView) findViewById(R.id.chartDay0),
                (TextView) findViewById(R.id.chartDay1),
                (TextView) findViewById(R.id.chartDay2),
                (TextView) findViewById(R.id.chartDay3),
                (TextView) findViewById(R.id.chartDay4),
                (TextView) findViewById(R.id.chartDay5),
                (TextView) findViewById(R.id.chartDay6)
        };

        findViewById(R.id.viewAllHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, HistoryActivity.class));
            }
        });

        loadDashboard();
        setupNavigation();
    }

    private void loadDashboard() {
        String history = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_HISTORY, "");
        String[] lines = history.trim().isEmpty() ? new String[0] : history.split("\\n");
        int total = BudgetManager.getCurrentPeriodSpending(this);
        int budget = BudgetManager.getBudgetAmount(this);
        int[] dailyTotals = new int[7];

        recentActivityList.removeAllViews();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            int amount = BudgetManager.parseAmount(line);
            addToDailyTotal(line, amount, dailyTotals);
            if (recentActivityList.getChildCount() < 3) {
                recentActivityList.addView(createRecentRow(line));
            }
        }

        if (recentActivityList.getChildCount() == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No recent Matumizi yet");
            emptyText.setTextColor(0xFF888888);
            emptyText.setTextSize(14);
            emptyText.setPadding(0, 16, 0, 16);
            recentActivityList.addView(emptyText);
        }

        int remaining = Math.max(budget - total, 0);
        int usedPercent = budget <= 0 ? 0 : Math.min(Math.round((total * 100f) / budget), 100);

        totalLabelText.setText(BudgetManager.getPeriodSpendingLabel(this));
        totalSpentText.setText(String.format(Locale.getDefault(), "%,d TZS", total));
        remainingBudgetText.setText(String.format(Locale.getDefault(), "Remaining budget: %,d TZS", remaining));
        budgetUsedText.setText(String.format(Locale.getDefault(), "%d%% USED", usedPercent));
        budgetProgress.setProgress(usedPercent);
        updateSpendingOverview(dailyTotals);
    }

    private void addToDailyTotal(String record, int amount, int[] dailyTotals) {
        java.util.Date recordDate = BudgetManager.parseRecordDate(record);
        if (recordDate == null) {
            return;
        }

        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        start.add(Calendar.DAY_OF_YEAR, -6);

        Calendar recordDay = Calendar.getInstance();
        recordDay.setTime(recordDate);
        recordDay.set(Calendar.HOUR_OF_DAY, 0);
        recordDay.set(Calendar.MINUTE, 0);
        recordDay.set(Calendar.SECOND, 0);
        recordDay.set(Calendar.MILLISECOND, 0);

        long diffMillis = recordDay.getTimeInMillis() - start.getTimeInMillis();
        int index = (int) (diffMillis / (24L * 60L * 60L * 1000L));
        if (index >= 0 && index < dailyTotals.length) {
            dailyTotals[index] += amount;
        }
    }

    private void updateSpendingOverview(int[] dailyTotals) {
        int max = 0;
        for (int amount : dailyTotals) {
            max = Math.max(max, amount);
        }

        Calendar labelDay = Calendar.getInstance();
        labelDay.add(Calendar.DAY_OF_YEAR, -6);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 0; i < chartBars.length; i++) {
            TextView bar = chartBars[i];
            int height = dailyTotals[i] == 0 || max == 0
                    ? dp(28)
                    : dp(28 + Math.round((dailyTotals[i] * 96f) / max));

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bar.getLayoutParams();
            params.height = height;
            bar.setLayoutParams(params);
            bar.setText(dayFormat.format(labelDay.getTime()).toUpperCase(Locale.getDefault()));
            bar.setBackgroundColor(dailyTotals[i] == 0 ? 0xFF2A2A2A : 0xFFD31329);
            bar.setTextColor(0xFFFFFFFF);

            labelDay.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private View createRecentRow(String record) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.dashboard_card_background);
        row.setPadding(14, 12, 14, 12);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, 12);
        row.setLayoutParams(rowParams);

        TextView icon = new TextView(this);
        icon.setGravity(android.view.Gravity.CENTER);
        icon.setText(categoryIcon(record));
        icon.setTextSize(18);
        icon.setBackgroundResource(record.contains("Shopping") ? R.drawable.recent_icon_shopping : R.drawable.recent_icon_food);
        row.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(12, 0, 8, 0);
        row.addView(details, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView title = new TextView(this);
        title.setText(extractTitle(record));
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(14);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        details.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(extractSubtitle(record));
        subtitle.setTextColor(0xFF888888);
        subtitle.setTextSize(11);
        details.addView(subtitle);

        TextView amount = new TextView(this);
        amount.setText(String.format(Locale.getDefault(), "-%,d", BudgetManager.parseAmount(record)));
        amount.setTextColor(0xFFFFFFFF);
        amount.setGravity(android.view.Gravity.RIGHT);
        amount.setTextSize(14);
        amount.setTypeface(amount.getTypeface(), android.graphics.Typeface.BOLD);
        row.addView(amount);

        return row;
    }

    private String categoryIcon(String record) {
        String category = CategoryManager.getRecordCategory(record);
        if (category.equals("Shopping")) {
            return "$";
        }
        if (category.equals("Transport")) {
            return "T";
        }
        if (category.equals("Bills")) {
            return "B";
        }
        if (category.equals("Investments")) {
            return "I";
        }
        if (category.equals("Food")) {
            return "F";
        }
        return category.isEmpty() ? "?" : category.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String extractTitle(String record) {
        String[] parts = record.split("\\|");
        if (parts.length >= 4) {
            String description = parts[3].trim();
            return description.length() > 22 ? description.substring(0, 22) + "..." : description;
        }
        return "Expense";
    }

    private String extractSubtitle(String record) {
        String[] parts = record.split("\\|");
        if (parts.length >= 3) {
            return parts[0].trim() + " - " + parts[2].trim();
        }
        return "Verified by AI";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void setupNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    return true;
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(DashboardActivity.this, HistoryActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_add) {
                    startActivity(new Intent(DashboardActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_insights) {
                    startActivity(new Intent(DashboardActivity.this, InsightsActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
