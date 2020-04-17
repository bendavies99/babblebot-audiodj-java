package net.bdavies.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.bdavies.MusicBotPlugin;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.managers.AudioManager;
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
    @Getter
    private final MusicBotPlugin pluginReference;

    @Getter
    private final AudioManager manager;

    @Getter
    @Setter
    private boolean hasBeenSummoned = false;

    @Getter
    private final AudioSendHandler sendHandler;


    public GuildMusicManager(final AudioPlayerManager manager, final MusicBotPlugin pluginReference, final IDiscordFacade facade, final AudioManager guildManager) {
        this.player = manager.createPlayer();
        this.pluginReference = pluginReference;
        this.queue = new TrackQueue(player, facade, pluginReference.getApplication());
        this.manager = guildManager;
        this.player.addListener(queue);
        this.sendHandler = new DiscordAudioProvider(player);
        guildManager.setSendingHandler(sendHandler);
    }

}
