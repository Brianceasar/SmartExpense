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

import java.util.ArrayList;
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
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_UNAVAILABLE = 503;

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
        final ArrayList<String> categories = CategoryManager.getCategories(this);
        categories.add(0, "Auto Category");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.spinner_item, categories);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedCategory = categories.get(position);
                } else {
                    selectedCategory = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });

        // Check if the user forgot to type anything before calling the AI.
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputText.getText().toString().trim();
                if (!text.isEmpty()) {
                    analyzeExpense(text);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter your Matumizi description first!", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Settings will be available soon.", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_about) {
            Toast.makeText(this, "Smart Expense AI helps you log Matumizi in TZS.", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void analyzeExpense(final String text) {
        final String categoryOverride = selectedCategory;
        btnSubmit.setEnabled(false);
        Toast.makeText(this, "Logging your Matumizi with AI...", Toast.LENGTH_SHORT).show();

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
                    Log.w(TAG, "Gemini unavailable, using local parser: " + e.getMessage());
                    result = fallbackExpense(text);
                    errorMessage = e.getMessage();
                }

        if (!categoryOverride.isEmpty()) {
                    result.category = CategoryManager.sanitizeCategory(categoryOverride);
                }

                final ExpenseResult finalResult = result;
                final String finalSource = source;
                final String finalErrorMessage = errorMessage;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finalResult.category = CategoryManager.sanitizeCategory(finalResult.category);
                        CategoryManager.saveCategory(MainActivity.this, finalResult.category);
                        saveExpense(text, finalResult.amount, finalResult.category);
                        btnSubmit.setEnabled(true);
                        inputText.setText("");
                        spinnerCategory.setSelection(0);

                        String message = finalSource + " logged: "
                                + finalResult.amount + " TZS, " + finalResult.category;
                        if (!finalSource.equals("AI") && !finalErrorMessage.isEmpty()) {
                            message = message + " (AI unavailable)";
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

        String prompt = "Return only one JSON object: {\"amount\":\"\",\"category\":\"\"}. "
                + "No markdown, no explanation. "
                + "Amount is money spent, not item counts. "
                + "Prefer: " + joinCategories(CategoryManager.getCategories(this)) + ". "
                + "If none fit, create a short Title Case category. "
                + "Examples: shares/DSE/stocks=Investments, loan repayment=Loans. "
                + "Text: " + truncate(text, 180);

        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("maxOutputTokens", 80);
        generationConfig.put("temperature", 0);
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

        modelText = extractJsonObject(modelText);
        JSONObject parsed = new JSONObject(modelText);
        return new ExpenseResult(
                parsed.optString("amount", findAmount(text)).replace(",", ""),
                normalizeCategory(parsed.optString("category", findCategory(text)), text)
        );
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
        Pattern pattern = Pattern.compile("(\\d[\\d,]*(?:\\.\\d+)?)(\\s*)(million|millions|m|thousand|k)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        long amount = -1;

        while (matcher.find()) {
            String numberText = matcher.group(1).replace(",", "");
            String suffix = matcher.group(3) == null ? "" : matcher.group(3).toLowerCase(Locale.ROOT);

            try {
                double parsed = Double.parseDouble(numberText);
                if (suffix.equals("million") || suffix.equals("millions") || suffix.equals("m")) {
                    parsed *= 1000000;
                } else if (suffix.equals("thousand") || suffix.equals("k")) {
                    parsed *= 1000;
                }
                amount = Math.round(parsed);
            } catch (NumberFormatException ignored) {
            }
        }

        if (amount >= 0) {
            return String.valueOf(amount);
        }

        return "0";
    }

    private String joinCategories(ArrayList<String> categories) {
        StringBuilder builder = new StringBuilder();
        int count = Math.min(categories.size(), 12);
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(categories.get(i));
        }
        return builder.toString();
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

        if (lowerText.contains("share") || lowerText.contains("shares")
                || lowerText.contains("stock") || lowerText.contains("stocks")
                || lowerText.contains("dse") || lowerText.contains("bond")
                || lowerText.contains("treasury") || lowerText.contains("security")
                || lowerText.contains("securities") || lowerText.contains("invest")) {
            return "Investments";
        }

        if (lowerText.contains("loan") || lowerText.contains("borrow")
                || lowerText.contains("debt") || lowerText.contains("repayment")) {
            return "Loans";
        }

        if (lowerText.contains("bill") || lowerText.contains("rent")
                || lowerText.contains("water") || lowerText.contains("electric")) {
            return "Bills";
        }

        return "General";
    }

    private String normalizeCategory(String category, String text) {
        String cleaned = CategoryManager.sanitizeCategory(category);
        if (!cleaned.equals("General")) {
            return cleaned;
        }

        return findCategory(text);
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
