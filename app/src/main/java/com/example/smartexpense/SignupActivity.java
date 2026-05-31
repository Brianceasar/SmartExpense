package com.example.smartexpense;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    private EditText inputName;
    private EditText inputEmail;
    private EditText inputPassword;
    private CheckBox checkboxTerms;
    private Button btnSignup;
    private Button btnTogglePassword;
    private TextView linkLogin;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AuthManager.isSignedIn(this)) {
            openMain();
            return;
        }

        setContentView(R.layout.activity_signup);

        inputName = (EditText) findViewById(R.id.inputName);
        inputEmail = (EditText) findViewById(R.id.inputEmail);
        inputPassword = (EditText) findViewById(R.id.inputPassword);
        checkboxTerms = (CheckBox) findViewById(R.id.checkboxTerms);
        btnSignup = (Button) findViewById(R.id.btnSignup);
        btnTogglePassword = (Button) findViewById(R.id.btnTogglePassword);
        linkLogin = (TextView) findViewById(R.id.linkLogin);

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        linkLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                finish();
            }
        });

        btnTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });
    }

    private void signup() {
        String name = inputName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString();

        if (name.isEmpty()) {
            inputName.setError("Enter your full name");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Enter a valid email address");
            return;
        }

        if (!isStrongPassword(password)) {
            inputPassword.setError("Use at least 6 characters");
            return;
        }

        if (!checkboxTerms.isChecked()) {
            Toast.makeText(this, "Please accept the terms to continue.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthManager.createAccount(this, name, email, password);
        openMain();
    }

    private boolean isStrongPassword(String password) {
        return password.length() >= 6;
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        int selection = inputPassword.getSelectionStart();

        if (passwordVisible) {
            inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setText("Hide");
        } else {
            inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setText("Show");
        }

        inputPassword.setSelection(Math.max(selection, 0));
    }

    private void openMain() {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
