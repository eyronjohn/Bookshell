package com.example.testbooks1.Model;

import android.os.Parcel;
import android.os.Parcelable;

public class CommunityBook implements Parcelable {

    public String bookId;
    public String title;
    public String imageUrl;
    public String author, category, description;
    public String snippet;

    public CommunityBook() {}

    public CommunityBook(String bookId, String title, String author,
                         String imageUrl, String category, String description, String snippet) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.imageUrl = imageUrl;
        this.category = category;
        this.description = description;
        this.snippet = snippet;
    }

    protected CommunityBook(Parcel in) {
        bookId = in.readString();
        title = in.readString();
        author = in.readString();
        imageUrl = in.readString();
        category = in.readString();
        description = in.readString();
        snippet = in.readString();
    }

    public static final Creator<CommunityBook> CREATOR = new Creator<CommunityBook>() {
        @Override
        public CommunityBook createFromParcel(Parcel in) {
            return new CommunityBook(in);
        }

        @Override
        public CommunityBook[] newArray(int size) {
            return new CommunityBook[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(bookId);
        parcel.writeString(title);
        parcel.writeString(author);
        parcel.writeString(imageUrl);
        parcel.writeString(category);
        parcel.writeString(description);
        parcel.writeString(snippet);
    }
}