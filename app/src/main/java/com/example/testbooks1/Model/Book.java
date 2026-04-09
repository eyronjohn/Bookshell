package com.example.testbooks1.Model;

public class Book {
    String id;
    String title;
    String imageUrl;
    String author;
    String description;
    String publisher;
    String category;
    public long timestamp;
    public String readerLink;
    public Book() {}

    public Book(String id, String title, String imageUrl, String author, String description, String publisher, String category, String readerLink) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.author = author;
        this.description = description;
        this.publisher = publisher;
        this.category = category;
        this.readerLink = readerLink;
    }

    public Book(String id, String title, String imageUrl, String author, String description, String category) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.author = author;
        this.description = description;
        this.category = category;
    }

    public Book(String title, String imageUrl, String author, String description, String category) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.author = author;
        this.description = description;
        this.category = category;
    }

    public Book(String title, String imageUrl){
        this.title = title;
        this.imageUrl = imageUrl;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getReaderLink() {
        return readerLink;
    }
}