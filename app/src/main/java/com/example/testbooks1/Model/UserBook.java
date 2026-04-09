package com.example.testbooks1.Model;

public class UserBook {
    public String title;
    public String status;
    public String imageUrl;
    public long timestamp;
    public String author;
    public String description;
    public String category;

    public UserBook() {}

    public UserBook(String title, String status, String imageUrl, long timestamp, String author, String description, String category) {
        this.title = title;
        this.status = status;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.author = author;
        this.description = description;
        this.category = category;
    }

    public UserBook(String title, String imageUrl, long timestamp, String author, String description, String category) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.author = author;
        this.description = description;
        this.category = category;
    }
}