package com.example.smartexpense;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private EditText inputText;
    private Spinner spinnerCategory;
    private ImageButton btnSubmit;
    private TextView btnUseLocation, locationDetails;
    private BottomNavigationView bottomNavigation;
    private String selectedCategory = "";
    private LocationData capturedLocation;

    private static final String PREFS_NAME = "expense_data";
    private static final String KEY_HISTORY = "history";
    private static final String TAG = "SmartExpenseAI";
    private static final int LOCATION_PERMISSION_REQUEST = 41;

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
        btnUseLocation = (TextView) findViewById(R.id.btnUseLocation);
        locationDetails = (TextView) findViewById(R.id.locationDetails);
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

        btnUseLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureCurrentLocation();
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
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (itemId == R.id.action_about) {
            Toast.makeText(this, "Smart Expense AI helps you log Matumizi in TZS.", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void captureCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST
            );
            return;
        }

        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Toast.makeText(this, "Location service unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUseLocation.setEnabled(false);
        btnUseLocation.setText("Capturing location...");

        // LocationManager reads device GPS/network provider data for latitude, longitude, accuracy, and time.
        Location bestLocation = getBestLastKnownLocation(locationManager);
        if (bestLocation != null) {
            handleLocation(bestLocation);
            return;
        }

        try {
            String provider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    ? LocationManager.GPS_PROVIDER
                    : LocationManager.NETWORK_PROVIDER;
            locationManager.requestSingleUpdate(provider, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    handleLocation(location);
                }

                @Override
                public void onProviderDisabled(@NonNull String provider) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resetLocationButton();
                            Toast.makeText(MainActivity.this, "Enable location services to capture GPS", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }, null);
        } catch (Exception e) {
            resetLocationButton();
            Toast.makeText(this, "Could not request location", Toast.LENGTH_SHORT).show();
        }
    }

    private Location getBestLastKnownLocation(LocationManager locationManager) {
        Location bestLocation = null;
        List<String> providers = locationManager.getProviders(true);
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null && (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy())) {
                bestLocation = location;
            }
        }
        return bestLocation;
    }

    private void handleLocation(final Location location) {
        capturedLocation = new LocationData(location);
        showCapturedLocation();

        new Thread(new Runnable() {
            @Override
            public void run() {
                LocationData resolved = new LocationData(location);
                Log.d("SmartExpenseAI", "Maps key empty=" + BuildConfig.MAPS_API_KEY.isEmpty()
                        + ", length=" + BuildConfig.MAPS_API_KEY.length());
                if (BuildConfig.MAPS_API_KEY == null || BuildConfig.MAPS_API_KEY.trim().isEmpty()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Missing Maps API Key", Toast.LENGTH_SHORT).show();
                        }
                    });
                    try {
                        // Geocoder is the Android fallback when Places is unavailable or has no result.
                        resolvePlaceWithGeocoder(resolved);
                    } catch (Exception geocoderError) {
                        Log.w(TAG, "Geocoder lookup failed, saving coordinates only: " + geocoderError.getMessage());
                    }
                } else {
                    try {
                        // Google Places converts raw coordinates into a nearby real-world place name.
                        resolvePlaceWithGooglePlaces(resolved);
                    } catch (Exception placesError) {
                        Log.w(TAG, "Places lookup failed, trying Geocoder: " + placesError.getMessage());
                        try {
                            // Geocoder is the Android fallback when Places is unavailable or has no result.
                            resolvePlaceWithGeocoder(resolved);
                        } catch (Exception geocoderError) {
                            Log.w(TAG, "Geocoder lookup failed, saving coordinates only: " + geocoderError.getMessage());
                        }
                    }
                }

                capturedLocation = resolved;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showCapturedLocation();
                        resetLocationButton();
                    }
                });
            }
        }).start();
    }

    private void resolvePlaceWithGooglePlaces(LocationData data) throws Exception {
        if (BuildConfig.MAPS_API_KEY == null || BuildConfig.MAPS_API_KEY.trim().isEmpty()) {
            throw new Exception("missing Maps API key");
        }

        String urlValue = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + data.latitude + "," + data.longitude
                + "&radius=75"
                + "&key=" + BuildConfig.MAPS_API_KEY;
        HttpURLConnection connection = (HttpURLConnection) new URL(urlValue).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(12000);

        int responseCode = connection.getResponseCode();
        InputStream inputStream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        String response = readStream(inputStream);
        connection.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("Places request failed");
        }

        JSONObject json = new JSONObject(response);
        JSONArray results = json.optJSONArray("results");
        if (results == null || results.length() == 0) {
            throw new Exception("no nearby place");
        }

        JSONObject first = results.getJSONObject(0);
        data.placeName = first.optString("name", "");
        data.address = first.optString("vicinity", "");
    }

    private void resolvePlaceWithGeocoder(LocationData data) throws Exception {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocation(data.latitude, data.longitude, 1);
        if (addresses == null || addresses.isEmpty()) {
            throw new Exception("no geocoder address");
        }

        Address address = addresses.get(0);
        data.placeName = address.getFeatureName() == null ? "" : address.getFeatureName();
        data.address = address.getAddressLine(0) == null ? "" : address.getAddressLine(0);
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

    private void showCapturedLocation() {
        if (capturedLocation == null) {
            locationDetails.setVisibility(View.GONE);
            return;
        }

        locationDetails.setVisibility(View.VISIBLE);
        locationDetails.setText("Place: " + valueOrUnknown(capturedLocation.placeName)
                + "\nAddress: " + valueOrUnknown(capturedLocation.address)
                + "\nLat/Lng: " + capturedLocation.latitude + ", " + capturedLocation.longitude
                + "\nAccuracy: " + String.format(Locale.getDefault(), "%.1fm", capturedLocation.accuracy));
    }

    private String valueOrUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown" : value;
    }

    private void resetLocationButton() {
        btnUseLocation.setEnabled(true);
        btnUseLocation.setText(R.string.add_use_current_location);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean granted = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    granted = true;
                    break;
                }
            }

            if (granted) {
                captureCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
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
                        result = new GeminiExpenseClient().askExpense(
                                BuildConfig.GEMINI_API_KEY,
                                text,
                                CategoryManager.getCategories(MainActivity.this),
                                findAmount(text),
                                findCategory(text)
                        );
                        result.category = normalizeCategory(result.category, text);
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
                        capturedLocation = null;
                        showCapturedLocation();

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

    private ExpenseResult fallbackExpense(String text) {
        return new ExpenseResult(findAmount(text), findCategory(text));
    }

    private void saveExpense(String text, String amount, String category) {
        String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
        LocationData location = capturedLocation;
        String record = date + " | " + amount + " TZS | " + sanitizeField(category) + " | " + sanitizeField(text)
                + " | " + (location == null ? "" : location.latitude)
                + " | " + (location == null ? "" : location.longitude)
                + " | " + (location == null ? "" : location.accuracy)
                + " | " + (location == null ? "" : sanitizeField(location.placeName))
                + " | " + (location == null ? "" : sanitizeField(location.address))
                + " | " + (location == null ? "" : location.timestamp);
        saveRecord(record);
    }

    private String sanitizeField(String value) {
        return value == null ? "" : value.replace("|", " ").replace("\n", " ").trim();
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
        // SharedPreferences stores prototype data locally as newline-separated expense records.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String oldHistory = prefs.getString(KEY_HISTORY, "");
        String newHistory = record + "\n" + oldHistory;
        prefs.edit().putString(KEY_HISTORY, newHistory).apply();
    }

    private static final class LocationData {
        final double latitude;
        final double longitude;
        final float accuracy;
        final long timestamp;
        String placeName = "";
        String address = "";

        LocationData(Location location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            accuracy = location.getAccuracy();
            timestamp = location.getTime();
        }
    }
}
