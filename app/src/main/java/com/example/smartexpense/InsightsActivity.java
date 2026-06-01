package com.example.smartexpense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class InsightsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "expense_data";
    private static final String KEY_HISTORY = "history";
    private static final String TAG = "SmartExpenseInsights";
    private static final String GEMINI_MODEL = "gemini-2.5-flash";
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_UNAVAILABLE = 503;

    private BottomNavigationView bottomNavigation;
    private LinearLayout insightsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthManager.isSignedIn(this)) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_insights);
        TopMenu.attach(this);
        bottomNavigation = (BottomNavigationView) findViewById(R.id.bottomNavigation);
        insightsList = (LinearLayout) findViewById(R.id.insightsList);
        loadInsights();
        setupNavigation();
    }

    private void loadInsights() {
        renderInsights(localInsights());

        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ArrayList<String> generated = askGeminiForInsights();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderInsights(generated);
                        }
                    });
                } catch (Exception e) {
                    Log.w(TAG, "AI insights unavailable, keeping local insights: " + e.getMessage());
                }
            }
        }).start();
    }

    private ArrayList<String> askGeminiForInsights() throws Exception {
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                + GEMINI_MODEL + ":generateContent");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-goog-api-key", BuildConfig.GEMINI_API_KEY);

        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        JSONObject generationConfig = new JSONObject();

        part.put("text", "Return only one JSON object: {\"insights\":[\"...\",\"...\",\"...\"]}. "
                + "No markdown, no explanation. "
                + "Each item max 18 words, practical, based only on: "
                + buildCompactSummary());
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("maxOutputTokens", 180);
        generationConfig.put("temperature", 0.3);
        requestBody.put("generationConfig", generationConfig);

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(requestBody.toString().getBytes("UTF-8"));
        outputStream.close();

        int responseCode = connection.getResponseCode();
        InputStream inputStream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String response = readStream(inputStream);
        connection.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            if (responseCode == HTTP_TOO_MANY_REQUESTS || responseCode == HTTP_UNAVAILABLE) {
                throw new Exception("AI service busy");
            }
            throw new Exception("AI request failed");
        }

        JSONObject json = new JSONObject(response);
        String modelText = json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim();

        JSONArray insights = new JSONObject(extractJsonObject(modelText)).getJSONArray("insights");
        ArrayList<String> result = new ArrayList<String>();
        for (int i = 0; i < insights.length() && i < 3; i++) {
            String insight = insights.optString(i, "").trim();
            if (!insight.isEmpty()) {
                result.add(insight);
            }
        }
        return result.isEmpty() ? localInsights() : result;
    }

    private String buildCompactSummary() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String history = prefs.getString(KEY_HISTORY, "");
        String[] records = history.trim().isEmpty() ? new String[0] : history.split("\\n");
        LinkedHashMap<String, Integer> totals = new LinkedHashMap<String, Integer>();
        int total = 0;
        int count = 0;

        for (String record : records) {
            if (count >= 20) {
                break;
            }
            String clean = record.trim();
            if (clean.isEmpty()) {
                continue;
            }

            int amount = BudgetManager.parseAmount(clean);
            String category = CategoryManager.getRecordCategory(clean);
            total += amount;
            totals.put(category, totals.containsKey(category) ? totals.get(category) + amount : amount);
            count++;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("budget=").append(BudgetManager.getBudgetSummary(this));
        summary.append("; period_spend=").append(BudgetManager.getCurrentPeriodSpending(this));
        summary.append("; recent_total=").append(total);
        summary.append("; categories=");
        int added = 0;
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            if (added > 0) {
                summary.append(",");
            }
            summary.append(entry.getKey()).append(":").append(entry.getValue());
            added++;
            if (added >= 8) {
                break;
            }
        }
        return summary.toString();
    }

    private ArrayList<String> localInsights() {
        ArrayList<String> insights = new ArrayList<String>();
        int spending = BudgetManager.getCurrentPeriodSpending(this);
        int budget = BudgetManager.getBudgetAmount(this);
        int percent = budget <= 0 ? 0 : Math.round((spending * 100f) / budget);

        if (spending == 0) {
            insights.add("No spending in this budget period yet. Keep logging expenses to generate useful insights.");
        } else {
            insights.add(String.format(Locale.getDefault(), "You have used %d%% of your %s budget.", percent, BudgetManager.getBudgetPeriod(this).toLowerCase(Locale.getDefault())));
        }
        insights.add("Review the largest category this week and set a smaller target before more spending.");
        insights.add("Keep category names specific so future insights can compare patterns more accurately.");
        return insights;
    }

    private void renderInsights(ArrayList<String> insights) {
        insightsList.removeAllViews();
        for (String insight : insights) {
            TextView card = new TextView(this);
            card.setText(insight);
            card.setTextColor(0xFFFFFFFF);
            card.setTextSize(15);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackgroundResource(R.drawable.dashboard_card_background);
            card.setPadding(dp(18), dp(18), dp(18), dp(18));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dp(14));
            insightsList.addView(card, params);
        }
    }

    private String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        reader.close();
        return builder.toString();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String extractJsonObject(String value) throws Exception {
        String cleaned = value == null ? "" : value
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int start = cleaned.indexOf("{");
        int end = cleaned.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        throw new Exception("Gemini returned non-JSON text: " + truncate(cleaned, 80));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void setupNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_insights);
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(InsightsActivity.this, DashboardActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(InsightsActivity.this, HistoryActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_add) {
                    startActivity(new Intent(InsightsActivity.this, MainActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_insights) {
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(InsightsActivity.this, ProfileActivity.class));
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
