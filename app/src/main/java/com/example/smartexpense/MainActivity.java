package com.example.smartexpense;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private EditText inputText;
    private Spinner spinnerCategory;
    private ImageButton btnSubmit;
    private BottomNavigationView bottomNavigation;
    private String selectedCategory = "";

    private static final String PREFS_NAME = "expense_data";
    private static final String KEY_HISTORY = "history";
    private static final String TAG = "SmartExpenseAI";
    private static final String GEMINI_MODEL = "gemini-2.5-flash";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthManager.isSignedIn(this)) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        TopMenu.attach(this);

        // View Mapping
        inputText = (EditText) findViewById(R.id.inputText);
        spinnerCategory = (Spinner) findViewById(R.id.spinnerCategory);
        btnSubmit = (ImageButton) findViewById(R.id.btnSubmit);
        bottomNavigation = (BottomNavigationView) findViewById(R.id.bottomNavigation);

        // Spinner Setup
        final String[] categories = {"Auto Category", "Food", "Transport", "Shopping", "Bills"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedCategory = categories[position];
                } else {
                    selectedCategory = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });

        // Button Click Listener
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputText.getText().toString().trim();
                if (!text.isEmpty()) {
                    analyzeExpense(text);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter an expense", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Bottom Navigation Listener
        bottomNavigation.setSelectedItemId(R.id.nav_add);
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Intent dashboardIntent = new Intent(MainActivity.this, DashboardActivity.class);
                    startActivity(dashboardIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_add) {
                    return true;
                } else if (itemId == R.id.nav_history) {
                    Intent historyIntent = new Intent(MainActivity.this, HistoryActivity.class);
                    startActivity(historyIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_insights) {
                    Intent insightsIntent = new Intent(MainActivity.this, InsightsActivity.class);
                    startActivity(insightsIntent);
                    finish();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(profileIntent);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_about) {
            Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void analyzeExpense(final String text) {
        final String categoryOverride = selectedCategory;
        btnSubmit.setEnabled(false);
        Toast.makeText(this, "AI analyzing expense...", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                ExpenseResult result;
                String source = "Local";
                String errorMessage = "";

                try {
                    if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
                        result = fallbackExpense(text);
                        errorMessage = "missing API key";
                    } else {
                        result = askGemini(text);
                        source = "AI";
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Gemini failed, using local parser", e);
                    result = fallbackExpense(text);
                    errorMessage = e.getMessage();
                }

                if (!categoryOverride.isEmpty()) {
                    result.category = categoryOverride;
                }

                final ExpenseResult finalResult = result;
                final String finalSource = source;
                final String finalErrorMessage = errorMessage;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        saveExpense(text, finalResult.amount, finalResult.category);
                        btnSubmit.setEnabled(true);
                        inputText.setText("");
                        spinnerCategory.setSelection(0);

                        String message = finalSource + " saved: "
                                + finalResult.amount + " TZS, " + finalResult.category;
                        if (!finalSource.equals("AI") && !finalErrorMessage.isEmpty()) {
                            message = message + " (" + finalErrorMessage + ")";
                        }

                        Toast.makeText(
                                MainActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }
        }).start();
    }

    private ExpenseResult askGemini(String text) throws Exception {
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                + GEMINI_MODEL + ":generateContent");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-goog-api-key", BuildConfig.GEMINI_API_KEY);

        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        JSONObject generationConfig = new JSONObject();

        String prompt = "Extract this expense into JSON only. "
                + "Use keys amount and category. "
                + "Category must be Food, Transport, Shopping, Bills, or General. "
                + "Use Food for meals, drinks, beer, restaurants, snacks, groceries, or eating out. "
                + "Return example: {\"amount\":\"15000\",\"category\":\"Food\"}. "
                + "Expense: " + text;

        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);
        generationConfig.put("responseMimeType", "application/json");
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
            throw new Exception("Gemini HTTP " + responseCode + " for " + GEMINI_MODEL
                    + ": " + truncate(response, 160));
        }

        JSONObject json = new JSONObject(response);
        String modelText = json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim();

        modelText = modelText.replace("```json", "").replace("```", "").trim();
        JSONObject parsed = new JSONObject(modelText);
        return new ExpenseResult(
                parsed.optString("amount", findAmount(text)).replace(",", ""),
                parsed.optString("category", findCategory(text))
        );
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

    private ExpenseResult fallbackExpense(String text) {
        return new ExpenseResult(findAmount(text), findCategory(text));
    }

    private void saveExpense(String text, String amount, String category) {
        String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
        String record = date + " | " + amount + " TZS | " + category + " | " + text;
        saveRecord(record);
    }

    private String findAmount(String text) {
        Pattern pattern = Pattern.compile("(\\d[\\d,]*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replace(",", "");
        }
        return "0";
    }

    private String findCategory(String text) {
        String lowerText = text.toLowerCase(Locale.ROOT);

        if (lowerText.contains("pizza") || lowerText.contains("food")
                || lowerText.contains("lunch") || lowerText.contains("dinner")
                || lowerText.contains("restaurant") || lowerText.contains("beer")
                || lowerText.contains("drink") || lowerText.contains("grocery")
                || lowerText.contains("snack")) {
            return "Food";
        }

        if (lowerText.contains("bus") || lowerText.contains("taxi")
                || lowerText.contains("uber") || lowerText.contains("fuel")
                || lowerText.contains("transport")) {
            return "Transport";
        }

        if (lowerText.contains("shop") || lowerText.contains("clothes")
                || lowerText.contains("market") || lowerText.contains("shoes")) {
            return "Shopping";
        }

        if (lowerText.contains("bill") || lowerText.contains("rent")
                || lowerText.contains("water") || lowerText.contains("electric")) {
            return "Bills";
        }

        return "General";
    }

    private void saveRecord(String record) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String oldHistory = prefs.getString(KEY_HISTORY, "");
        String newHistory = record + "\n" + oldHistory;
        prefs.edit().putString(KEY_HISTORY, newHistory).apply();
    }

    private static class ExpenseResult {
        String amount;
        String category;

        ExpenseResult(String amount, String category) {
            this.amount = amount;
            this.category = category;
        }
    }
}
