package com.gnomeland.musiclab.controllers;

import com.gnomeland.musiclab.model.Track;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@RequestMapping("/music")
public class MusicController {
    private final List<Track> tracks = List.of(
            new Track("Jovin Webb", Track.GENRE_BLUES, "Save Me"),
            new Track("Jaskon Browne", Track.GENRE_FOLK, "Take it easy"),
            new Track("Schubert", Track.GENRE_CLASSICAL, "Ave Maria"),
            new Track("JADE", Track.GENRE_DANCE, "IT girl"),
            new Track("Rodney Stepp", Track.GENRE_ELECTRONIC, "Breakout"),
            new Track("Architects", Track.GENRE_HEAVYMETAL, "Animals"),
            new Track("Eminem", Track.GENRE_HIPHOP, "My name is"),
            new Track("Eminem", Track.GENRE_RAP, "Without Me"),
            new Track("Peggy Lee", Track.GENRE_JAZZ, "Alone Together"),
            new Track("Doja Cat", Track.GENRE_POP, "Paint tne town red"),
            new Track("Radiohead", Track.GENRE_ROCK, "Creep"),
            new Track("The Kooks", Track.GENRE_ROCK, "Naive"),
            new Track("Doja Cat", Track.GENRE_POP, "Woman"),
            new Track("Stepzz", Track.GENRE_HIPHOP, "Rock")
    );

    @GetMapping("/tracks")
    public List<Track> getTracks(
            @RequestParam(value = "genre",
                    required = false) final String genre,
            @RequestParam(value = "singer",
                    required = false) final String singer
    ) {
        return tracks.stream()
                .filter(track -> (genre == null
                        || track.getGenre().equalsIgnoreCase(genre))
                        && (singer == null || track.getSinger().equals(singer)))
                .toList();
    }

    @GetMapping("/track")
    public Track getTrack(
            @RequestParam String title, @RequestParam String artist) {
        return tracks.stream()
                .filter(track -> track.getTitle().equalsIgnoreCase(title)
                        && track.getSinger().equalsIgnoreCase(artist))
                .findFirst().orElse(null);
    }
}
