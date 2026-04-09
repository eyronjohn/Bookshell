package com.example.testbooks1.Model;

public class BookReactions {
    public int like;
    public int fire;
    public int heart;
    public int sad;
    public int angry;

    public BookReactions() {}

    public BookReactions(int like, int fire, int heart, int sad, int angry) {
        this.like = like;
        this.fire = fire;
        this.heart = heart;
        this.sad = sad;
        this.angry = angry;
    }
}