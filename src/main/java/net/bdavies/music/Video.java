package net.bdavies.music;

import lombok.Getter;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public class Video {

    @Getter
    private VideoId id;
    @Getter
    private VideoSnippet snippet;

    public static class VideoId {
        @Getter
        private String videoId;
    }

    public static class VideoSnippet {
        @Getter
        private String title;
    }
}