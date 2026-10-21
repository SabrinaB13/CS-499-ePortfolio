package com.example.watchtheweightfalloff;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText editUsername;
    private EditText editPassword;
    private Button buttonLogin;
    private Button buttonCreateAccount;

    private UserDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Shows the login screen layout
        setContentView(R.layout.activity_login);

        // Connects UI elements from activity_login.xml
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonCreateAccount = findViewById(R.id.buttonCreateAccount);

        // Sets up database helper class
        userDao = new UserDao(this);

        // Validates log in against database
        buttonLogin.setOnClickListener(v -> {
            String username = editUsername.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter a username and password.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean valid = userDao.validateUser(username, password);

            if (valid) {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();

                // Move to the grid screen
                Intent intent = new Intent(MainActivity.this, LoggedWeightsActivity.class);
                intent.putExtra("username", username); // optional for later
                startActivity(intent);

            } else {
                Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
            }
        });

        // Creates Account allows user to insert username/password into database
        buttonCreateAccount.setOnClickListener(v -> {
            String username = editUsername.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter a username and password to create an account.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userDao.userExists(username)) {
                Toast.makeText(this, "That username already exists. Try another one.", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean created = userDao.createUser(username, password);

            if (created) {
                Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();

                // Move to the grid screen after account creation (optional but common)
                Intent intent = new Intent(MainActivity.this, LoggedWeightsActivity.class);
                intent.putExtra("username", username);
                startActivity(intent);

            } else {
                Toast.makeText(this, "Account creation failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });

    }
}