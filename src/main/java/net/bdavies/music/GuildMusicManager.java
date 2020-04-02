package net.bdavies.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.bdavies.MusicBotPlugin;
import uk.co.bjdavies.api.discord.IDiscordFacade;

/**
 * This handles an AudioPlayer per Guild which allows your bot to run in multiple guilds.
 *
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
@Slf4j
public class GuildMusicManager {

    /**
     * This is the player that handles play, pause, skip, etc.
     */
    @Getter
    private final AudioPlayer player;

    /**
     * This will handle the queue of tracks e.g. enqueue, pop, ontrackstart, ontrackend, etc.
     */
    @Getter
    private final TrackQueue queue;

    /**
     * This is reference for the Music bot plugin
     */
    private final MusicBotPlugin pluginReference;

    /**
     * This is the current connection inside the Guild. The bot needs this to disconnect from the voice channel;
     */
    @Getter
    @Setter
    private VoiceConnection voiceConnection;

    @Getter
    @Setter
    private boolean hasBeenSummoned = false;


    private final AudioProvider provider;

    public GuildMusicManager(final AudioPlayerManager manager, final MusicBotPlugin pluginReference, final IDiscordFacade facade) {
        this.player = manager.createPlayer();
        this.pluginReference = pluginReference;
        this.queue = new TrackQueue(player, facade);
        this.provider = new DiscordAudioProvider(player);
        this.player.addListener(queue);
    }

    public AudioProvider getAudioProvider() {
        return provider;
    }
}
