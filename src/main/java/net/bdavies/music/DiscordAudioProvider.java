package net.bdavies.music;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import discord4j.voice.AudioProvider;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public class DiscordAudioProvider extends AudioProvider {
    private final AudioPlayer audioPlayer;
    private final MutableAudioFrame frame = new MutableAudioFrame();

    /**
     * @param audioPlayer Audio player to wrap.
     */
    public DiscordAudioProvider(AudioPlayer audioPlayer) {
        super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
        this.audioPlayer = audioPlayer;
        frame.setBuffer(getBuffer());
    }

    @Override
    public boolean provide() {
        final boolean didProvide = audioPlayer.provide(frame);
        if (didProvide) {
            ((Buffer) getBuffer()).flip();
        }
        return didProvide;
    }
}
