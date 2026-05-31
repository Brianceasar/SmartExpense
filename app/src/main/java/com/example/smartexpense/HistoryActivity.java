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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private BottomNavigationView nav;
    private LinearLayout historyList;
    private TextView monthlyTotal;
    private EditText historySearch;
    private TextView chipAll, chipFood, chipShopping, chipTransport;
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
        monthlyTotal = (TextView) findViewById(R.id.historyMonthlyTotal);
        historySearch = (EditText) findViewById(R.id.historySearch);
        chipAll = (TextView) findViewById(R.id.chipAll);
        chipFood = (TextView) findViewById(R.id.chipFood);
        chipShopping = (TextView) findViewById(R.id.chipShopping);
        chipTransport = (TextView) findViewById(R.id.chipTransport);

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

        chipAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory("All");
            }
        });

        chipFood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory("Food");
            }
        });

        chipShopping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory("Shopping");
            }
        });

        chipTransport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCategory("Transport");
            }
        });
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        updateChipStyles();
        renderHistory();
    }

    private void updateChipStyles() {
        styleChip(chipAll, selectedCategory.equals("All"));
        styleChip(chipFood, selectedCategory.equals("Food"));
        styleChip(chipShopping, selectedCategory.equals("Shopping"));
        styleChip(chipTransport, selectedCategory.equals("Transport"));
    }

    private void styleChip(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.history_chip_selected : R.drawable.history_chip);
        chip.setTextColor(selected ? 0xFFFFFFFF : 0xFF888888);
    }

    private void renderHistory() {
        int total = 0;
        int rendered = 0;
        String query = historySearch == null ? "" : historySearch.getText().toString().trim().toLowerCase(Locale.ROOT);

        historyList.removeAllViews();

        for (String cleanRecord : allRecords) {
            total += parseAmount(cleanRecord);

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

        monthlyTotal.setText(String.format(Locale.getDefault(), "%,d TZS", total));
    }

    private boolean matchesFilters(String record, String query) {
        if (!selectedCategory.equals("All") && !record.toLowerCase(Locale.ROOT).contains(selectedCategory.toLowerCase(Locale.ROOT))) {
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
        subtitle.setTextColor(0xFF4A3340);
        subtitle.setTextSize(10);
        subtitle.setTypeface(subtitle.getTypeface(), Typeface.BOLD);
        details.addView(subtitle);

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.RIGHT);
        row.addView(right, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView amount = new TextView(this);
        amount.setText(String.format(Locale.getDefault(), "-%,d", parseAmount(record)));
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
        if (record.contains("Shopping")) {
            return "$";
        }
        if (record.contains("Transport")) {
            return "T";
        }
        if (record.contains("Bills")) {
            return "B";
        }
        return "F";
    }

    private int parseAmount(String record) {
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
