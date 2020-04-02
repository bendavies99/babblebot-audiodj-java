package net.bdavies.music;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public abstract class MusicTrack {

    @Getter
    protected final AudioTrack audioTrack;

    @Getter
    protected final AudioPlaylist playlist;

    @Getter
    protected final String[] options;


    protected MusicTrack(AudioTrack audioTrack, AudioPlaylist playlist, String[] options) {
        this.audioTrack = audioTrack;
        this.playlist = playlist;
        this.options = options;
    }

    public MusicTrack(AudioTrack track) {
        this(track, null, new String[0]);
    }

    public MusicTrack(AudioPlaylist playlist) {
        this(null, playlist, new String[0]);
    }

    public MusicTrack(String[] options) {
        this(null, null, options);
    }


    public boolean isPlaylist() {
        return playlist != null;
    }

    public boolean isOptions() {
        return options.length > 0;
    }

    @Override
    public String toString() {
        return "MusicTrack{" +
                "audioTrack=" + audioTrack +
                ", playlist=" + playlist +
                '}';
    }
}
