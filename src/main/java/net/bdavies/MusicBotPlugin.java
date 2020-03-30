package net.bdavies;

import com.google.inject.Inject;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.bdavies.music.GuildMusicManager;
import net.bdavies.request.RequestStrategy;
import net.bdavies.request.RequestStrategyFactory;
import reactor.core.publisher.Mono;
import reactor.util.Loggers;
import uk.co.bjdavies.api.IApplication;
import uk.co.bjdavies.api.command.Command;
import uk.co.bjdavies.api.command.CommandExample;
import uk.co.bjdavies.api.command.CommandParam;
import uk.co.bjdavies.api.command.ICommandContext;
import uk.co.bjdavies.api.discord.IDiscordFacade;
import uk.co.bjdavies.api.plugins.IPluginEvents;
import uk.co.bjdavies.api.plugins.IPluginSettings;
import uk.co.bjdavies.api.plugins.Plugin;
import uk.co.bjdavies.api.plugins.PluginConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
@Slf4j
@Plugin(value = "musicbot", author = "Ben <ben.davies99@outlook.com>", minServerVersion = "2.0.0", namespace = "")
public class MusicBotPlugin implements IPluginEvents {

    /**
     * This is the player manager allows encoding tracks and getting data from Youtube, Soundcloud, etc.
     */
    @Getter
    private final AudioPlayerManager playerManager;

    /**
     * Application
     */
    private final IApplication application;

    /**
     * Discord Facade
     */
    private final IDiscordFacade discordFacade;

    /**
     * This will hold all the managers in a guild.
     */
    private final Map<Long, GuildMusicManager> musicManagers;

    @PluginConfig
    private MusicBotPluginConfig config;

    @Inject
    public MusicBotPlugin(IApplication application, IDiscordFacade discordFacade) {
        this.application = application;
        this.discordFacade = discordFacade;
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);

        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        musicManagers = new HashMap<>();
    }

    private Mono<GuildMusicManager> addToMusicManagersIfDoesntExist(Long guildId) {
        if (musicManagers.containsKey(guildId)) {
            return Mono.just(musicManagers.get(guildId));
        }
        log.info("MusicManager not found creating one...");
        GuildMusicManager manager = new GuildMusicManager(playerManager, this);
        musicManagers.put(guildId, manager);
        return Mono.just(manager).log("addToMusicManagersIfDoesntExist");
    }

    private Mono<GuildMusicManager> getAudioPlayer(Mono<Guild> guild) {
        return guild.map(g -> g.getId().asLong()).flatMap(this::addToMusicManagersIfDoesntExist).log("getAudioPlayer");
    }

    @Command(description = "describe the music bot")
    public String about(ICommandContext commandContext) {
        return "Music Bot, Ben Davies";
    }

    @Command(aliases = {"play", "req", "request"},
            description = "This will play a song.",
            requiresValue = true,
            exampleValue = "https://www.youtube.com/watch?v=we9jeU76Y9E")
    @CommandParam(value = "url",
            canBeEmpty = false,
            exampleValue = "https://www.youtube.com/watch?v=we9jeU76Y9E")
    public String play(ICommandContext commandContext) {
        String value = commandContext.hasParameter("url") ?
                commandContext.getParameter("url") :
                commandContext.getValue();

        return loadAndPlay(commandContext, value);
    }

    @Command(aliases = "summon", description = "Summon the bot the voice channel you are in.")
    public Mono<String> summon(ICommandContext commandContext) {
        return commandContext.getMessage().getAuthorAsMember()
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .flatMap(c -> getAudioPlayer(c.getGuild()).flatMap(gmm -> c.join(spec -> spec.setProvider(gmm.getAudioProvider()))
                        .flatMap(vc -> {
                            gmm.setVoiceConnection(vc);
                            gmm.setHasBeenSummoned(true);
                            return Mono.just("I Have been summoned to " + c.getName()).log("Return Message");
                        }))).log(Loggers.getLogger(MusicBotPlugin.class))
                .map(m -> m != null ? m : "You are not in a voice channel!");
    }

    @Command(aliases = {"exile", "part"},
            description = "Exile the bot from the voice channel, only if your in the channel")
    public String exile(ICommandContext context) {
        return runCommand(context, gmm -> {
            String toReturn = discordFacade
                    .getOurUser()
                    .flatMap(u -> context.getMessage().getGuild()
                            .flatMap(g -> u.asMember(g.getId())))
                    .flatMap(self -> context.getMessage().getAuthorAsMember()
                            .flatMap(member -> member.getVoiceState()
                                    .flatMap(memberVoiceState -> self.getVoiceState()
                                            .flatMap(selfVoiceState -> selfVoiceState.getChannel()
                                                    .flatMap(selfVoiceChannel -> memberVoiceState.getChannel()
                                                            .filterWhen(memberVoiceChannel ->
                                                                    Mono.just(
                                                                            selfVoiceChannel.getId().asLong() ==
                                                                                    memberVoiceChannel.getId().asLong()))
                                                            .map(memberVoiceChannel -> {
                                                                gmm.getPlayer().destroy();
                                                                gmm.getQueue().empty();
                                                                gmm.setHasBeenSummoned(false);
                                                                gmm.getVoiceConnection().disconnect();
                                                                return "Left Channel: " + memberVoiceChannel.getName();
                                                            })))))).map(m -> m).block();

            return toReturn != null ? toReturn :
                    "Unable to leave channel, please make sure the bot is in the same voice channel you are in.";
        });
    }

    @Command(description = "Skip a track use the value of all to clear the queue (useful for someone who has requested a playlist)", exampleValue = "all")
    @CommandExample("!skip all")
    public String skip(ICommandContext context) {
        return runCommand(context, gmm -> {
            if (context.getValue().toLowerCase().contains("all")) {
                gmm.getQueue().empty();
                gmm.getQueue().pop();
                return "Cleared Queue";
            }

            gmm.getQueue().pop();

            return "Track skipped.";
        });
    }

    @Command(description = "Pause the current playing track.")
    public String pause(ICommandContext context) {
        return runCommand(context, gmm -> {
            if (gmm.getPlayer().isPaused()) {
                return "Bot already paused use !music-resume";
            }

            gmm.getPlayer().setPaused(true);
            return "Track paused.";
        });
    }

    @Command(description = "Pause the current playing track.")
    public String resume(ICommandContext context) {
        return runCommand(context, gmm -> {
            if (!gmm.getPlayer().isPaused()) {
                return "Bot already playing use !music-pause";
            }

            gmm.getPlayer().setPaused(false);
            return "Track resumed.";
        });
    }

    @Command(description = "skip to a point in a track")
    @CommandParam(value = "mins", canBeEmpty = false, exampleValue = "1")
    @CommandParam(value = "seconds", canBeEmpty = false, exampleValue = "1")
    @CommandParam(value = "hours", canBeEmpty = false, exampleValue = "1")
    public String skipTo(ICommandContext context) {
        return runCommand(context, gmm -> {
            if (gmm.getPlayer().getPlayingTrack() == null) {
                return "I am not a playing a track";
            }

            if (context.hasParameter("mins")) {
                int v = Integer.parseInt(context.getParameter("mins"));
                v = v * 1000 * 60;
                long value = Long.parseLong(Integer.toString(v));
                gmm.getPlayer().getPlayingTrack().setPosition(value);
                return "Track skipped to " + context.getParameter("mins") + " mins.";
            }

            if (context.hasParameter("seconds")) {
                int v = Integer.parseInt(context.getParameter("seconds"));
                v = v * 1000;
                long value = Long.parseLong(Integer.toString(v));
                gmm.getPlayer().getPlayingTrack().setPosition(value);
                return "Track skipped to " + context.getParameter("seconds") + " seconds.";
            }

            if (context.hasParameter("hours")) {
                int v = Integer.parseInt(context.getParameter("hours"));
                v = v * 1000;
                long value = Long.parseLong(Integer.toString(v));
                gmm.getPlayer().getPlayingTrack().setPosition(value);
                return "Track skipped to " + context.getParameter("hours") + " hours.";
            }

            long value = Long.parseLong(context.getValue());

            gmm.getPlayer().getPlayingTrack().setPosition(value);

            return "Track skipped to part.";
        });
    }

    private String runCommand(ICommandContext context, Function<GuildMusicManager, String> consumer) {
        return this.getAudioPlayer(context.getMessage().getGuild()).map(gmm -> {
            if (!gmm.isHasBeenSummoned()) {
                return "You have not summoned me, Command will not work unless you use !music-summon";
            }

            return consumer.apply(gmm);
        }).block();
    }

    /**
     * This will return an embed object to display on discord.
     *
     * @param track     - The track that is being displayed.
     * @param desc      - The description of the embed object.
     * @param isRequest - this determines id the display is a request display.
     * @param guild     - The guild to get the information for the times.
     * @return EmbedObject
     */
    public Consumer<EmbedCreateSpec> getEmbedObject(AudioTrack track, String desc, boolean isRequest, Mono<Guild> guild) {
        return embed -> embed.setTitle(track.getInfo().title)
                .setFooter("Provided by BabbleBot 2020. Authored by Ben Davies", "")
                //.setThumbnail(JSONUtils.getVideoThumbnail(track.getInfo().identifier))
                .setColor(Objects.requireNonNull(guild
                        .flatMap(g -> discordFacade.getOurUser()
                                .flatMap(u -> u.asMember(g.getId()))
                                .flatMap(Member::getColor)).map(c -> c).block()))
                .setDescription(desc)
                .addField("Song url", "http://y2u.be/" + track.getInfo().identifier, false)
                .addField("Song length", getSongLengthFormatted(track), true)
                .addField(isRequest ? "Wait time" : "Remaining Time",
                        isRequest ? getQueueTimeFormatted(guild, track) : getSongLengthRemainingFormatted(track),
                        true);
    }

    /**
     * This will return the current length of the queue.
     *
     * @param guild - The guild that we want get the queue for.
     * @return long
     */
    public long getLengthOfQueue(Mono<Guild> guild, AudioTrack requestedTrack) {
        AtomicLong time = new AtomicLong();
        getAudioPlayer(guild).doOnNext(gmm -> {
            AudioTrack currentTrack = gmm.getPlayer().getPlayingTrack();
            if (currentTrack != null) {
                if (currentTrack != requestedTrack) {
                    time.set(currentTrack.getDuration() - currentTrack.getPosition());
                }
            }
            for (AudioTrack track : gmm.getQueue().getUnderlyingQueue()) {
                if (track != requestedTrack) {
                    time.addAndGet(track.getDuration() - track.getPosition());
                }
            }
        }).block();
        return time.get();
    }

    /**
     * This will return the queue time in a format of HH:MM:SS
     *
     * @param guild - The guild that we want get the queue for.
     * @return String
     */
    public String getQueueTimeFormatted(Mono<Guild> guild, AudioTrack track) {
        long queueTime = getLengthOfQueue(guild, track);
        return getTimeFormatted(queueTime);
    }

    /**
     * This will return the the track's length in a format of HH:MM:SS
     *
     * @param track - The track you want to format
     * @return String
     */
    public String getSongLengthFormatted(AudioTrack track) {
        long queueTime = track.getDuration();
        return getTimeFormatted(queueTime);
    }


    /**
     * This will return the the track's remaining length in a format of HH:MM:SS
     *
     * @param track - The track you want to format
     * @return String
     */
    public String getSongLengthRemainingFormatted(AudioTrack track) {
        long queueTime = track.getDuration() - track.getPosition();
        return getTimeFormatted(queueTime);
    }

    private String getTimeFormatted(long time) {
        long hours = TimeUnit.MILLISECONDS.toHours(time);
        time -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
        time -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time);

        return "" + (hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds < 10 ? "0" + seconds : seconds);
    }

    private void sendEmbedMessage(Mono<MessageChannel> textChannel, Consumer<EmbedCreateSpec> embed) {
        textChannel.subscribe(c -> c.createMessage(spec -> spec.setEmbed(embed)).subscribe());
    }

    private String loadAndPlay(final ICommandContext commandContext, String value) {
        return runCommand(commandContext, gmm -> {

            if (commandContext.getValue().equals("")) {
                if (gmm.getPlayer().isPaused()) {
                    gmm.getPlayer().setPaused(false);
                    return "Resumed playing";
                } else {
                    return "Please provide a url (Youtube, Twitch, Soundcloud) or search query.";
                }
            }

            RequestStrategy strategy = RequestStrategyFactory.makeRequestStrategy(application, value);
            strategy.handle(value, playerManager).doOnError(e ->
                    commandContext.getMessage().getChannel()
                            .flatMap(c -> c.createMessage("Unfortunately the song you requested, failed please make sure the resource exists."))
                            .subscribe())
                    .subscribe(mt -> {
                        log.info("Music Track: " + mt);
                        if (mt.isPlaylist()) {
                            AudioPlaylist playlist = mt.getPlaylist();
                            Message message = commandContext.getMessage();
                            if (commandContext.hasParameter("loadPlaylist")) {

                                playlist.getTracks().forEach(gmm.getQueue()::enQueue);
                                message.getChannel()
                                        .flatMap(c -> c.createMessage("Loaded Playlist: " + playlist.getTracks().size() + " Tracks."))
                                        .subscribe();
                            } else {
                                gmm.getQueue().enQueue(playlist.getSelectedTrack());
                                sendEmbedMessage(message.getChannel(),
                                        getEmbedObject(playlist.getSelectedTrack(),
                                                "Using song requested, use -loadPlaylist to submit whole playlist",
                                                true,
                                                message.getGuild()));
                            }
                        } else {
                            gmm.getQueue().enQueue(mt.getAudioTrack());
                            sendEmbedMessage(commandContext.getMessage().getChannel(),
                                    getEmbedObject(mt.getAudioTrack(), "Song requested.", true,
                                            commandContext.getMessage().getGuild()));
                        }
                    });
            return "";
        });
    }

    @Override
    public void onReload() {

    }

    @Override
    public void onBoot(IPluginSettings settings) {
        settings.setNamespace(config.getNamespace());
    }

    @Override
    public void onShutdown() {

    }
}
