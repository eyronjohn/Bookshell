package com.example.testbooks1.Model;
public class BookCount {
    public String bookId;
    public String title;
    public String imageUrl;
    public String author;
    public String description;
    public String publisher;
    public String category;
    public String readerLink;
    public int count;

    public BookCount() {}

    public BookCount(String bookId, String title, String imageUrl, String author,
                     String description, String publisher, String category, String readerLink, int count) {
        this.bookId = bookId;
        this.title = title;
        this.imageUrl = imageUrl;
        this.author = author;
        this.description = description;
        this.publisher = publisher;
        this.category = category;
        this.readerLink = readerLink;
        this.count = count;
    }
}