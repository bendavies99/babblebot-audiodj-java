package net.bdavies.request;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.bdavies.MusicBotPluginConfig;
import net.bdavies.music.MusicTrack;
import reactor.core.publisher.Mono;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public interface RequestStrategy {
    /**
     * This will be ran when a new request is made.
     *
     * @param request        - This is the request that has been made by the user.
     * @param commandContext - This is the message object that came from the command context.
     * @param musicBot       - The musicBot plugin instance.
     * @return AudioTrack - audiotrack enQueue.
     */
    Mono<MusicTrack> handle(MusicBotPluginConfig config, String request, AudioPlayerManager manager);
}
