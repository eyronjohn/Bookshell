package com.example.testbooks1;

import android.content.Context;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.testbooks1.Model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegistrationActivity extends AppCompatActivity {

    EditText etEmail, etPassword, fNameET, lNameET;
    Button registerBtn;
    TextView tvAlreadyRegistered;
    FirebaseAuth mAuth;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        c = this;
        initialize();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_registration), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int left = v.getPaddingLeft() + systemBars.left;
            int top = v.getPaddingTop() + systemBars.top;
            int right = v.getPaddingRight() + systemBars.right;
            int bottom = v.getPaddingBottom() + systemBars.bottom;
            v.setPadding(left, top, right, bottom);
            return insets;
        });
    }

    public void initialize(){
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        fNameET = findViewById(R.id.fNameET);
        lNameET = findViewById(R.id.lNameET);

        registerBtn = findViewById(R.id.registerBtn);
        tvAlreadyRegistered = findViewById(R.id.tvAlreadyRegistered);
        mAuth = FirebaseAuth.getInstance();

        registerBtn.setOnClickListener(view -> {
            String fName = fNameET.getText().toString().trim();
            String lName = lNameET.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (fName.isEmpty()) {
                Toast.makeText(c, "Please enter your first name.", Toast.LENGTH_SHORT).show();
                return;
            } if (lName.isEmpty()) {
                Toast.makeText(c, "Please enter your last name.", Toast.LENGTH_SHORT).show();
                return;
            } if (email.isEmpty()) {
                Toast.makeText(c, "Please enter your email.", Toast.LENGTH_SHORT).show();
                return;
            } if (password.isEmpty()) {
                Toast.makeText(c, "Please enter your password.", Toast.LENGTH_SHORT).show();
                return;
            } if (password.length() < 6) {
                Toast.makeText(c, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    String uid = user.getUid();
                                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
                                    User newUser = new User(fName, lName, email);
                                    ref.child(uid).setValue(newUser).addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            user.sendEmailVerification();
                                            Toast.makeText(c, "Registration successful. Please verify your email.", Toast.LENGTH_LONG).show();
                                            startActivity(new Intent(c, LoginActivity.class));
                                        } else {
                                            Toast.makeText(c, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } else {
                                try {
                                    throw task.getException();
                                } catch (FirebaseAuthUserCollisionException e) {
                                    Toast.makeText(c, "This email is already registered.", Toast.LENGTH_SHORT).show();
                                } catch (FirebaseAuthWeakPasswordException e) {
                                    Toast.makeText(c, "Password is too weak.", Toast.LENGTH_SHORT).show();
                                } catch (FirebaseAuthInvalidCredentialsException e) {
                                    Toast.makeText(c, "Invalid email format.", Toast.LENGTH_SHORT).show();
                                } catch (FirebaseAuthException e) {
                                    Toast.makeText(c, e.getMessage(), Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(c, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        tvAlreadyRegistered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(c, LoginActivity.class); //loginActivity
                startActivity(intent);
            }
        });
    }
}