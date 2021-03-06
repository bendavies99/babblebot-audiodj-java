package net.bdavies.music;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public class YoutubeMusicTrack extends MusicTrack {

    protected YoutubeMusicTrack(AudioTrack audioTrack, AudioPlaylist playlist) {
        super(audioTrack, playlist, new String[0]);
    }

    public YoutubeMusicTrack(AudioTrack track) {
        super(track);
    }

    public YoutubeMusicTrack(AudioPlaylist playlist) {
        super(playlist);
    }

    public YoutubeMusicTrack(String[] options) {
        super(options);
    }

    @Override
    public String toString() {
        return "YoutubeMusicTrack{" +
                "audioTrack=" + audioTrack +
                ", playlist=" + playlist +
                '}';
    }
}
