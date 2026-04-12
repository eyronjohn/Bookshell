package com.example.testbooks1;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import android.content.Intent;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
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
        View root = findViewById(R.id.activity_registration);
        final int baseLeft = root.getPaddingLeft();
        final int baseTop = root.getPaddingTop();
        final int baseRight = root.getPaddingRight();
        final int baseBottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    baseLeft + bars.left,
                    baseTop + bars.top,
                    baseRight + bars.right,
                    baseBottom + bars.bottom);
            return insets;
        });
        attachKeyboardScrollPadding(root, findViewById(R.id.register_root_content));
        initialize();
    }

    public void initialize(){
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        fNameET = findViewById(R.id.fNameET);
        lNameET = findViewById(R.id.lNameET);

        registerBtn = findViewById(R.id.registerBtn);
        tvAlreadyRegistered = findViewById(R.id.tvAlreadyRegistered);
        mAuth = FirebaseAuth.getInstance();

        View.OnFocusChangeListener scrollFocusedField = (v, hasFocus) -> {
            if (hasFocus) {
                ensureFieldVisibleWithKeyboard(v);
            }
        };
        fNameET.setOnFocusChangeListener(scrollFocusedField);
        lNameET.setOnFocusChangeListener(scrollFocusedField);
        etEmail.setOnFocusChangeListener(scrollFocusedField);
        etPassword.setOnFocusChangeListener(scrollFocusedField);

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
            } if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(c, "Invalid email format.", Toast.LENGTH_SHORT).show();
                return;
            } if (password.isEmpty()) {
                Toast.makeText(c, "Please enter your password.", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean hasUpper = false, hasLower = false, hasDigit = false;
            for (int i = 0; i < password.length(); i++) {
                char ch = password.charAt(i);
                if (Character.isUpperCase(ch)) {
                    hasUpper = true;
                } else if (Character.isLowerCase(ch)) {
                    hasLower = true;
                } else if (Character.isDigit(ch)) {
                    hasDigit = true;
                }
            }
            if (password.length() < 8 || !hasUpper || !hasLower || !hasDigit) {
                Toast.makeText(c, R.string.toast_password_requirements, Toast.LENGTH_SHORT).show();
                return;
            }

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
                                Exception cause = task.getException();
                                if (cause == null) {
                                    throw new Exception("Unknown registration error");
                                }
                                throw cause;
                            } catch (FirebaseAuthUserCollisionException e) {
                                Toast.makeText(c, "This email is already registered.", Toast.LENGTH_SHORT).show();
                            } catch (FirebaseAuthWeakPasswordException e) {
                                Toast.makeText(c, R.string.toast_password_requirements, Toast.LENGTH_SHORT).show();
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                Toast.makeText(c, "Invalid email format.", Toast.LENGTH_SHORT).show();
                            } catch (FirebaseAuthException e) {
                                Toast.makeText(c, e.getMessage(), Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(c, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        tvAlreadyRegistered.setOnClickListener(v -> {
            Intent intent = new Intent(c, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void attachKeyboardScrollPadding(View scroll, View content) {
        final int pl = content.getPaddingLeft();
        final int pt = content.getPaddingTop();
        final int pr = content.getPaddingRight();
        final int pbBase = content.getPaddingBottom();
        final View root = scroll.getRootView();
        final float density = getResources().getDisplayMetrics().density;
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            scroll.getWindowVisibleDisplayFrame(r);
            int rootH = root.getHeight();
            int keypad = Math.max(0, rootH - r.bottom);
            int slack = (int) (24 * density);
            int minKeyboard = Math.max((int) (rootH * 0.13f), (int) (180 * density));
            int extra = keypad > minKeyboard ? keypad + slack : 0;
            int want = pbBase + extra;
            if (content.getPaddingBottom() != want) {
                content.setPadding(pl, pt, pr, want);
            }
        });
    }

    private void ensureFieldVisibleWithKeyboard(View field) {
        ScrollView scroll = findViewById(R.id.activity_registration);
        if (scroll == null) {
            return;
        }
        final int gapPx = (int) (56 * getResources().getDisplayMetrics().density);
        field.post(() -> {
            int[] fLoc = new int[2];
            int[] sLoc = new int[2];
            field.getLocationOnScreen(fLoc);
            scroll.getLocationOnScreen(sLoc);
            int fieldBottom = fLoc[1] + field.getHeight();
            int visibleBottom = sLoc[1] + scroll.getHeight() - scroll.getPaddingBottom();
            int delta = fieldBottom - visibleBottom + gapPx;
            if (delta > 0) {
                scroll.smoothScrollBy(0, delta);
            }
        });
    }
}