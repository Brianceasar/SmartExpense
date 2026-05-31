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

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail;
    private EditText inputPassword;
    private CheckBox checkboxRemember;
    private Button btnLogin;
    private Button btnTogglePassword;
    private TextView linkSignup;
    private TextView forgotPassword;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AuthManager.isSignedIn(this)) {
            openMain();
            return;
        }

        setContentView(R.layout.activity_login);

        inputEmail = (EditText) findViewById(R.id.inputEmail);
        inputPassword = (EditText) findViewById(R.id.inputPassword);
        checkboxRemember = (CheckBox) findViewById(R.id.checkboxRemember);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnTogglePassword = (Button) findViewById(R.id.btnTogglePassword);
        linkSignup = (TextView) findViewById(R.id.linkSignup);
        forgotPassword = (TextView) findViewById(R.id.forgotPassword);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        linkSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this, "Please sign up again if you need fresh local access.", Toast.LENGTH_SHORT).show();
            }
        });

        btnTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });
    }

    private void login() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString();

        if (!isValidEmail(email)) {
            inputEmail.setError("Enter a valid email address");
            return;
        }

        if (password.isEmpty()) {
            inputPassword.setError("Enter your password");
            return;
        }

        if (!AuthManager.hasAccount(this)) {
            Toast.makeText(this, "No local account found. Please sign up first.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (AuthManager.login(this, email, password)) {
            if (checkboxRemember.isChecked()) {
                Toast.makeText(this, "Welcome back. Your session is active.", Toast.LENGTH_SHORT).show();
            }
            openMain();
        } else {
            Toast.makeText(this, "Email or password does not match our local records.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
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
