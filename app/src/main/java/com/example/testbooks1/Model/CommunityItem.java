package com.example.testbooks1.Model;

import java.util.ArrayList;

public class CommunityItem {
    public String listId;
    public String userId;
    public String fullName;
    public String listTitle;
    public String listDescription;
    public String firstBookImage;
    public String coverImage; // Base64 string
    public ArrayList<CommunityBook> books;
    public int commentCount = 0;

    public CommunityItem() {}
}