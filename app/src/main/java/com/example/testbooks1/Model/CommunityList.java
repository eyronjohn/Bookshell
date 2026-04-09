package com.example.testbooks1.Model;


import java.util.ArrayList;
import java.util.HashMap;

public class CommunityList {
    public String listId;
    public String title;
    public String description;
    public long timestamp;
    public ArrayList<CommunityBook> books;

    public CommunityList() {}

    public CommunityList(String listId, String title, String description, long timestamp, ArrayList<CommunityBook> books) {
        this.listId = listId;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.books = books;
    }
}