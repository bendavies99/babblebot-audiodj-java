package net.bdavies.request;

import com.google.inject.Inject;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.extern.slf4j.Slf4j;
import net.bdavies.MusicBotPluginConfig;
import net.bdavies.music.MusicTrack;
import net.bdavies.music.YoutubeMusicTrack;
import reactor.core.publisher.Mono;
import uk.co.bjdavies.api.discord.IDiscordFacade;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
@Slf4j
public class StandardRequestStrategy implements RequestStrategy {


    private final IDiscordFacade facade;

    @Inject
    public StandardRequestStrategy(IDiscordFacade facade) {
        this.facade = facade;
    }


    @Override
    public Mono<MusicTrack> handle(MusicBotPluginConfig config, String request, AudioPlayerManager manager) {

        return Mono.create(sink -> manager.loadItem(request, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                sink.success(new YoutubeMusicTrack(track));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                sink.success(new YoutubeMusicTrack(playlist));
            }

            @Override
            public void noMatches() {
                sink.error(new FriendlyException("No Matches found", FriendlyException.Severity.COMMON, null));
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                sink.error(exception);
            }
        }));
    }
}
