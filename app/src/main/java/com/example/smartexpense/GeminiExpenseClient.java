package com.example.smartexpense;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

class GeminiExpenseClient {
    static final String MODEL = "gemini-2.5-flash";

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_UNAVAILABLE = 503;

    ExpenseResult askExpense(
            String apiKey,
            String text,
            ArrayList<String> categories,
            String fallbackAmount,
            String fallbackCategory
    ) throws Exception {
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/"
                + MODEL + ":generateContent");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-goog-api-key", apiKey);

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(buildExpenseRequestJson(text, categories).getBytes("UTF-8"));
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
            throw new Exception("AI request failed HTTP " + responseCode + ": " + truncate(response, 160));
        }

        return parseExpenseResponse(response, fallbackAmount, fallbackCategory);
    }

    static String buildExpenseRequestJson(String text, ArrayList<String> categories) throws Exception {
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        JSONObject generationConfig = new JSONObject();
        JSONObject thinkingConfig = new JSONObject();

        String prompt = "Return only one JSON object matching this exact shape: "
                + "{\"amount\":\"\",\"category\":\"\"}. "
                + "No markdown, no explanation. "
                + "Amount is money spent, not item counts. "
                + "Prefer: " + joinCategories(categories) + ". "
                + "If none fit, create a short Title Case category. "
                + "Examples: shares/DSE/stocks=Investments, loan repayment=Loans. "
                + "Text: " + truncate(text, 180);

        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("maxOutputTokens", 256);
        generationConfig.put("temperature", 0);
        thinkingConfig.put("thinkingBudget", 0);
        generationConfig.put("thinkingConfig", thinkingConfig);
        requestBody.put("generationConfig", generationConfig);

        return requestBody.toString();
    }

    static ExpenseResult parseExpenseResponse(
            String response,
            String fallbackAmount,
            String fallbackCategory
    ) throws Exception {
        JSONObject json = new JSONObject(response);
        String modelText = json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim();

        JSONObject parsed = new JSONObject(extractJsonObject(modelText));
        return new ExpenseResult(
                parsed.optString("amount", fallbackAmount).replace(",", ""),
                CategoryManager.sanitizeCategory(parsed.optString("category", fallbackCategory))
        );
    }

    static String extractJsonObject(String value) throws Exception {
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

    private static String readStream(InputStream inputStream) throws Exception {
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

    private static String joinCategories(ArrayList<String> categories) {
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

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength) + "...";
    }
}
