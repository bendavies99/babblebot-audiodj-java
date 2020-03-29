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


    protected MusicTrack(AudioTrack audioTrack, AudioPlaylist playlist) {
        this.audioTrack = audioTrack;
        this.playlist = playlist;
    }

    public MusicTrack(AudioTrack track) {
        this(track, null);
    }

    public MusicTrack(AudioPlaylist playlist) {
        this(null, playlist);
    }

    public boolean isPlaylist() {
        return playlist != null;
    }

    @Override
    public String toString() {
        return "MusicTrack{" +
                "audioTrack=" + audioTrack +
                ", playlist=" + playlist +
                '}';
    }
}
