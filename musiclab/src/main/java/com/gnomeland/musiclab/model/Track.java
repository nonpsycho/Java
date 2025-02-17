package com.gnomeland.musiclab.model;

public class Track {
    public static final String GENRE_BLUES = "Blues";
    public static final String GENRE_CLASSICAL = "Classical";
    public static final String GENRE_FOLK = "Folk";
    public static final String GENRE_HEAVYMETAL = "HeavyMetal";
    public static final String GENRE_HIPHOP = "HipHop";
    public static final String GENRE_POP = "Pop";
    public static final String GENRE_RAP = "Rap";
    public static final String GENRE_ROCK = "Rock";
    public static final String GENRE_DANCE = "Dance";
    public static final String GENRE_ELECTRONIC = "Electronic";
    public static final String GENRE_JAZZ = "Jazz";
    private final String singer;
    private final String genre;
    private final String title;

    public Track(final String author, final String style, final String name) {
        this.singer = author;
        this.genre = style;
        this.title = name;
    }

    public final String getSinger() {
        return singer;
    }

    public final String getGenre() {
        return genre;
    }

    public final String getTitle() {
        return title;
    }
}
