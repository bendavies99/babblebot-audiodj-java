package net.bdavies.request;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.bdavies.music.MusicTrack;
import reactor.core.publisher.Mono;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public class SearchRequestStrategy implements RequestStrategy {
    @Override
    public Mono<MusicTrack> handle(String request, AudioPlayerManager manager) {
        return Mono.empty();
    }
}
