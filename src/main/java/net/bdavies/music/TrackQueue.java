package net.bdavies.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
@Slf4j
public class TrackQueue extends AudioEventAdapter {

    @Getter
    private final AudioPlayer player;

    @Getter
    private final BlockingQueue<AudioTrack> underlyingQueue;

    public TrackQueue(AudioPlayer player) {
        this.player = player;
        underlyingQueue = new LinkedBlockingQueue<>();
    }

    /**
     * This will enqueue a track to the queue and be up for consumption by the AudioPlayer
     *
     * @param track - The track that going to be queued
     */
    public void enQueue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            underlyingQueue.offer(track);
        }
    }

    /**
     * This will pop the queue stack and play that track.
     */
    public void pop() {
        player.startTrack(underlyingQueue.poll(), false);
    }

    /**
     * This will empty the queue all tracks will be deleted.
     */
    public void empty() {
        underlyingQueue.clear();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        log.info("Track ended.");
        if (endReason.mayStartNext) {
            pop();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        assert track != null;
        log.error("An error occurred while loading track: " + track.getIdentifier(), exception);
    }
}
