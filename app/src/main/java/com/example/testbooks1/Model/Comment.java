package com.example.testbooks1.Model;

public class Comment {
    public String commentId;
    public String userId;
    public String fullName;
    public String text;
    public long timestamp;

    public Comment() {}

    public Comment(String commentId, String userId, String fullName, String text, long timestamp) {
        this.commentId = commentId;
        this.userId = userId;
        this.fullName = fullName;
        this.text = text;
        this.timestamp = timestamp;
    }
}