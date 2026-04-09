package com.example.testbooks1.Model;

public class UserManager {

    private static User currentUser;

    public static void setUser(User user) {
        currentUser = user;
    }

    public static User getUser() {
        return currentUser;
    }

    public static String getFullName() {
        if (currentUser == null) return "Anonymous";

        String first = currentUser.firstName != null ? currentUser.firstName : "";
        String last = currentUser.lastName != null ? currentUser.lastName : "";

        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? "Anonymous" : fullName;
    }
}