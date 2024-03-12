package com.example.emailpasswordauth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        Button signUpButton = findViewById(R.id.signUpButton);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText emailInput = findViewById(R.id.editTextTextEmailAddress);
                String email = emailInput.getText().toString();
                EditText passwordInput = findViewById(R.id.editTextTextPassword);
                String password = passwordInput.getText().toString();
                String confirmPassword = ((EditText) findViewById(R.id.editTextTextConfirmPassword)).getText().toString();

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                    return;
                }

                signUp(email, password);
            }
        });

        auth = FirebaseAuth.getInstance();
    }

    public void signUp(String email, String password) {
        // TODO add here validation on email formatting and password (letters, numbers, symbol, min length 6)
        // NOTE Firebase does some validation on its side, will fail if password < 6 chars, if email not in email format
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = auth.getCurrentUser();
                            Toast.makeText(RegisterActivity.this, "Successfully registered!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                        } else {
                            // If sign up fails, display a message to the user.
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}