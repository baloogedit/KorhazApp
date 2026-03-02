package com.edite.korhazapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    // Firebase példányok
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Mezők és gomb összekötése
        EditText etName = findViewById(R.id.etName);
        EditText etCNP = findViewById(R.id.etCNP);
        EditText etEmail = findViewById(R.id.etRegisterEmail);
        EditText etPassword = findViewById(R.id.etRegisterPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String cnp = etCNP.getText().toString();
            String email = etEmail.getText().toString();
            String password = etPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Töltsd ki az adatokat!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Felhasználó létrehozása a Firebase Auth-ban
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String userId = mAuth.getCurrentUser().getUid();

                            // 2. Extra adatok mentése a Firestore-ba (szerepkör: beteg)
                            Map<String, Object> user = new HashMap<>();
                            user.put("name", name);
                            user.put("cnp", cnp);
                            user.put("email", email);
                            user.put("password", password);
                            user.put("role", "patient"); // Automatikusan beteg

                            db.collection("Users").document(userId).set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Sikeres regisztráció!", Toast.LENGTH_SHORT).show();
                                        finish(); // Vissza a loginra
                                    });
                        } else {
                            Toast.makeText(this, "Hiba: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        });

        tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }
}