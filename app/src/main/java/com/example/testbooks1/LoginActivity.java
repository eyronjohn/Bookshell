package com.example.testbooks1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.testbooks1.Model.AuthManager;
import com.example.testbooks1.Model.User;
import com.example.testbooks1.Model.UserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button loginBtn;
    TextView tvNotRegistered;
    FirebaseAuth mAuth;
    Context c;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        c = this;
        initialize();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
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
        loginBtn = findViewById(R.id.loginBtn);
        tvNotRegistered = findViewById(R.id.tvNotRegistered);
        mAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(c, "Please enter your email.", Toast.LENGTH_SHORT).show();
                return;
            } if (password.isEmpty()) {
                Toast.makeText(c, "Please enter your password.", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String uid = AuthManager.getUid();
                                //Toast.makeText(c, "Login successful.", Toast.LENGTH_SHORT).show();
                                //startActivity(new Intent(c, MainActivity.class));
                                DatabaseReference userRef = FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child(uid);

                                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        User user = snapshot.getValue(User.class);
                                        if (user != null) {
                                            UserManager.setUser(user);
                                        }
                                        Toast.makeText(c, "Login successful.", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(c, MainActivity.class));
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError error) {
                                        Toast.makeText(c, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            } else {
                                try {
                                    throw task.getException();
                                } catch (FirebaseAuthInvalidUserException e) {
                                    Toast.makeText(c, "No account found with this email.", Toast.LENGTH_SHORT).show();
                                } catch (FirebaseAuthInvalidCredentialsException e) {
                                    Toast.makeText(c, "Incorrect credentials.", Toast.LENGTH_SHORT).show();
                                } catch (FirebaseAuthException e) {
                                    Toast.makeText(c, e.getMessage(), Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(c, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

        tvNotRegistered.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(c, RegistrationActivity.class);
                startActivity(intent);
            }
        });
    }
}
