package com.example.testbooks1.Model;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthManager {

    private static FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public static FirebaseUser getUser() {
        return mAuth.getCurrentUser();
    }

    public static String getUid() {
        FirebaseUser user = getUser();
        return (user != null) ? user.getUid() : null;
    }

    public static boolean isLoggedIn() {
        return getUser() != null;
    }

    public static void logout() {
        mAuth.signOut();
    }
}