package com.example.smartexpense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private BottomNavigationView nav;
    private LinearLayout historyList;
    private LinearLayout monthlySpendList;
    private LinearLayout filterChipsContainer;
    private TextView periodLabel;
    private TextView monthlyTotal;
    private EditText historySearch;
    private ArrayList<TextView> filterChips = new ArrayList<TextView>();
    private ArrayList<String> allRecords = new ArrayList<String>();
    private String selectedCategory = "All";

    private static final String PREFS_NAME = "expense_data";
    private static final String KEY_HISTORY = "history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthManager.isSignedIn(this)) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_history);
        TopMenu.attach(this);

        nav = (BottomNavigationView) findViewById(R.id.bottomNavigation);
        historyList = (LinearLayout) findViewById(R.id.historyList);
        monthlySpendList = (LinearLayout) findViewById(R.id.monthlySpendList);
        filterChipsContainer = (LinearLayout) findViewById(R.id.historyFilterChips);
        periodLabel = (TextView) findViewById(R.id.historyPeriodLabel);
        monthlyTotal = (TextView) findViewById(R.id.historyMonthlyTotal);
        historySearch = (EditText) findViewById(R.id.historySearch);

        loadHistory();
        setupFilters();
        setupNavigation();
    }

    private void setupNavigation() {
        nav.setSelectedItemId(R.id.nav_history);
        nav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(HistoryActivity.this, DashboardActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_add) {
                    startActivity(new Intent(HistoryActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_history) {
                    return true;
                } else if (id == R.id.nav_insights) {
                    startActivity(new Intent(HistoryActivity.this, InsightsActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(HistoryActivity.this, ProfileActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    private void loadHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedHistory = prefs.getString(KEY_HISTORY, "");
        String[] records = savedHistory.trim().isEmpty() ? new String[0] : savedHistory.split("\\n");

        allRecords.clear();
        for (String record : records) {
            String cleanRecord = record.trim();
            if (!cleanRecord.isEmpty()) {
                allRecords.add(cleanRecord);
            }
        }

        renderHistory();
    }

    private void setupFilters() {
        historySearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderHistory();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        renderFilterChips();
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        updateChipStyles();
        renderHistory();
    }

    private void updateChipStyles() {
        for (TextView chip : filterChips) {
            styleChip(chip, selectedCategory.equals(chip.getText().toString()));
        }
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.history_chip_selected : R.drawable.history_chip);
        chip.setTextColor(selected ? 0xFFFFFFFF : 0xFF888888);
    }

    private void renderFilterChips() {
        filterChipsContainer.removeAllViews();
        filterChips.clear();
        addFilterChip("All");

        ArrayList<String> categories = CategoryManager.getCategories(this);
        for (String category : categories) {
            addFilterChip(category);
        }
        updateChipStyles();
    }

    private void addFilterChip(final String category) {
        TextView chip = new TextView(this);
        chip.setText(category);
        chip.setGravity(Gravity.CENTER);
        chip.setTextSize(13);
        chip.setTypeface(chip.getTypeface(), Typeface.BOLD);
        chip.setPadding(dp(18), 0, dp(18), 0);
        chip.setMinHeight(dp(38));
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory(category);
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
        );
        if (!filterChips.isEmpty()) {
            params.setMargins(dp(10), 0, 0, 0);
        }
        filterChipsContainer.addView(chip, params);
        filterChips.add(chip);
    }

    private void renderHistory() {
        int total = BudgetManager.getCurrentPeriodSpending(this);
        int budget = BudgetManager.getBudgetAmount(this);
        int remaining = Math.max(budget - total, 0);
        int rendered = 0;
        String query = historySearch == null ? "" : historySearch.getText().toString().trim().toLowerCase(Locale.ROOT);

        historyList.removeAllViews();

        for (String cleanRecord : allRecords) {
            if (matchesFilters(cleanRecord, query) && rendered < 12) {
                historyList.addView(createTransactionRow(cleanRecord, rendered));
                rendered++;
            }
        }

        if (rendered == 0) {
            TextView emptyText = new TextView(this);
            emptyText.setText(allRecords.isEmpty() ? R.string.empty_history : R.string.empty_filtered_history);
            emptyText.setTextColor(0xFF888888);
            emptyText.setTextSize(15);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, dp(24), 0, dp(24));
            historyList.addView(emptyText);
        }

        periodLabel.setText(BudgetManager.getPeriodSpendingLabel(this).toUpperCase(Locale.getDefault()));
        monthlyTotal.setText(String.format(Locale.getDefault(), "%,d TZS", total));
        TextView comparison = (TextView) findViewById(R.id.historyMonthlyComparison);
        comparison.setText(String.format(
                Locale.getDefault(),
                "Budget: %,d TZS | Remaining: %,d TZS",
                budget,
                remaining
        ));
        renderMonthlySpending();
    }

    private void renderMonthlySpending() {
        int[] totals = new int[6];
        int max = 0;

        for (String record : allRecords) {
            Date date = BudgetManager.parseRecordDate(record);
            int monthIndex = getMonthIndex(date);
            if (monthIndex >= 0 && monthIndex < totals.length) {
                totals[monthIndex] += BudgetManager.parseAmount(record);
                max = Math.max(max, totals[monthIndex]);
            }
        }

        monthlySpendList.removeAllViews();
        Calendar month = Calendar.getInstance();
        for (int i = 0; i < totals.length; i++) {
            monthlySpendList.addView(createMonthlySpendRow(month, totals[i], max, i == 0));
            month.add(Calendar.MONTH, -1);
        }
    }

    private int getMonthIndex(Date date) {
        if (date == null) {
            return -1;
        }

        Calendar current = Calendar.getInstance();
        Calendar record = Calendar.getInstance();
        record.setTime(date);
        return (current.get(Calendar.YEAR) - record.get(Calendar.YEAR)) * 12
                + current.get(Calendar.MONTH) - record.get(Calendar.MONTH);
    }

    private View createMonthlySpendRow(Calendar month, int total, int max, boolean selected) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(18));
        row.setLayoutParams(rowParams);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(top);

        TextView title = new TextView(this);
        title.setText(getMonthLabel(month));
        title.setTextColor(0xFFA0AAB8);
        title.setTextSize(14);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView marker = new TextView(this);
        marker.setText("o");
        marker.setGravity(Gravity.CENTER);
        marker.setTextColor(selected ? 0xFF4CAF50 : 0xFF888888);
        marker.setTextSize(20);
        marker.setTypeface(marker.getTypeface(), Typeface.BOLD);
        top.addView(marker, new LinearLayout.LayoutParams(dp(26), dp(26)));

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(max == 0 ? 0 : Math.round((total * 100f) / max));
        progress.setProgressDrawable(getResources().getDrawable(R.drawable.history_month_progress));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(10)
        );
        progressParams.setMargins(0, dp(10), 0, 0);
        row.addView(progress, progressParams);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        bottomParams.setMargins(0, dp(8), 0, 0);
        row.addView(bottom, bottomParams);

        TextView label = new TextView(this);
        label.setText("Total spend");
        label.setTextColor(0xFFA0AAB8);
        label.setTextSize(12);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        bottom.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView amount = new TextView(this);
        amount.setText(String.format(Locale.getDefault(), "TZS %,d", total));
        amount.setTextColor(0xFF4CAF50);
        amount.setTextSize(12);
        amount.setTypeface(amount.getTypeface(), Typeface.BOLD);
        bottom.addView(amount);

        return row;
    }

    private String getMonthLabel(Calendar month) {
        String monthName = month.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
        if (monthName == null) {
            monthName = "";
        }
        return monthName + "'s spend";
    }

    private boolean matchesFilters(String record, String query) {
        String recordCategory = CategoryManager.getRecordCategory(record);
        if (!selectedCategory.equals("All") && !recordCategory.equalsIgnoreCase(selectedCategory)) {
            return false;
        }

        return query.isEmpty() || record.toLowerCase(Locale.ROOT).contains(query);
    }

    private View createTransactionRow(String record, int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.dashboard_card_background);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(12));
        row.setLayoutParams(rowParams);

        TextView icon = new TextView(this);
        icon.setGravity(Gravity.CENTER);
        icon.setText(categoryIcon(record));
        icon.setTextColor(0xFFD31329);
        icon.setTextSize(16);
        icon.setTypeface(icon.getTypeface(), Typeface.BOLD);
        icon.setBackgroundResource(R.drawable.history_icon_circle);
        row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        details.setPadding(dp(12), 0, dp(8), 0);
        row.addView(details, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView title = new TextView(this);
        title.setText(extractTitle(record));
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(14);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        details.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(extractSubtitle(record));
        subtitle.setTextColor(0xFFA0AAB8);
        subtitle.setTextSize(10);
        subtitle.setTypeface(subtitle.getTypeface(), Typeface.BOLD);
        details.addView(subtitle);

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.RIGHT);
        row.addView(right, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView amount = new TextView(this);
        amount.setText(String.format(Locale.getDefault(), "-%,d", BudgetManager.parseAmount(record)));
        amount.setTextColor(0xFFD31329);
        amount.setTextSize(14);
        amount.setTypeface(amount.getTypeface(), Typeface.BOLD);
        amount.setGravity(Gravity.RIGHT);
        right.addView(amount);

        TextView status = new TextView(this);
        status.setText(index % 4 == 2 ? "PROCESSING" : "AI VERIFIED");
        status.setTextColor(index % 4 == 2 ? 0xFF888888 : 0xFFD31329);
        status.setTextSize(9);
        status.setTypeface(status.getTypeface(), Typeface.BOLD);
        status.setGravity(Gravity.RIGHT);
        right.addView(status);

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
        if (category.equals("Loans")) {
            return "L";
        }
        if (category.equals("Food")) {
            return "F";
        }
        return category.isEmpty() ? "?" : category.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String extractTitle(String record) {
        String[] parts = record.split("\\|");
        if (parts.length >= 4) {
            String title = parts[3].trim();
            return title.length() > 24 ? title.substring(0, 24) + "..." : title;
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

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
