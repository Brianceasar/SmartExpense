package com.example.smartexpense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;

public class GeminiExpenseClientTest {
    @Test
    public void buildExpenseRequestJsonRequestsJsonMode() throws Exception {
        ArrayList<String> categories = new ArrayList<String>();
        categories.add("Food");
        categories.add("Transport");
        categories.add("Investments");

        JSONObject request = new JSONObject(GeminiExpenseClient.buildExpenseRequestJson(
                "spent 15000 on lunch",
                categories
        ));

        JSONObject generationConfig = request.getJSONObject("generationConfig");
        String prompt = request.getJSONArray("contents")
                .getJSONObject(0)
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        assertEquals("application/json", generationConfig.getString("responseMimeType"));
        assertEquals(0, generationConfig.getInt("temperature"));
        assertEquals(256, generationConfig.getInt("maxOutputTokens"));
        assertEquals(0, generationConfig.getJSONObject("thinkingConfig").getInt("thinkingBudget"));
        assertTrue(prompt.contains("{\"amount\":\"\",\"category\":\"\"}"));
        assertTrue(prompt.contains("spent 15000 on lunch"));
    }

    @Test
    public void parseExpenseResponseAcceptsJsonText() throws Exception {
        String response = "{"
                + "\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"amount\\\":\\\"15,000\\\",\\\"category\\\":\\\"Food\\\"}\"}]}}]"
                + "}";

        ExpenseResult result = GeminiExpenseClient.parseExpenseResponse(response, "0", "General");

        assertEquals("15000", result.amount);
        assertEquals("Food", result.category);
    }

    @Test
    public void parseExpenseResponseExtractsJsonFromMarkdown() throws Exception {
        String response = "{"
                + "\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Here is the JSON:\\n```json\\n{\\\"amount\\\":\\\"10000\\\",\\\"category\\\":\\\"Transport\\\"}\\n```\"}]}}]"
                + "}";

        ExpenseResult result = GeminiExpenseClient.parseExpenseResponse(response, "0", "General");

        assertEquals("10000", result.amount);
        assertEquals("Transport", result.category);
    }
}
