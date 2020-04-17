package net.bdavies;

import com.google.inject.Inject;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.bdavies.music.GuildMusicManager;
import net.bdavies.request.RequestStrategy;
import net.bdavies.request.RequestStrategyFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
import uk.co.bjdavies.command.parser.DiscordMessageParser;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
@Slf4j
@Plugin(value = "musicbot", author = "Ben <ben.davies99@outlook.com>", minServerVersion = "3.0.0-pre.1", namespace = "")
public class MusicBotPlugin implements IPluginEvents {

    /**
     * This is the player manager allows encoding tracks and getting data from Youtube, Soundcloud, etc.
     */
    @Getter
    private final AudioPlayerManager playerManager;

    /**
     * Application
     */
    @Getter
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

    private final List<ContinuousResponse> responses = new ArrayList<>();

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

    private Mono<GuildMusicManager> addToMusicManagersIfDoesntExist(Guild g) {
        if (musicManagers.containsKey(g.getIdLong())) {
            return Mono.just(musicManagers.get(g.getIdLong()));
        }
        log.info("MusicManager not found creating one...");
        GuildMusicManager manager =
                new GuildMusicManager(playerManager, this, discordFacade, g.getAudioManager());
        musicManagers.put(g.getIdLong(), manager);
        return Mono.just(manager).log("addToMusicManagersIfDoesntExist");
    }

    private Mono<GuildMusicManager> getAudioPlayer(Mono<Guild> guild) {
        return guild.flatMap(this::addToMusicManagersIfDoesntExist).log("getAudioPlayer");
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
    public void play(ICommandContext commandContext) {
        String value = commandContext.hasParameter("url") ?
                commandContext.getParameter("url") :
                commandContext.getValue();

        loadAndPlay(commandContext, value);
    }

    @Command(aliases = "summon", description = "Summon the bot the voice channel you are in.")
    public Mono<String> summon(ICommandContext commandContext) {

        Guild g = commandContext.getMessage().getGuild();
        Member m = g.getMember(commandContext.getMessage().getAuthor());

        return Mono.create(sink -> {
            if (m != null) {
                GuildVoiceState voiceState = m.getVoiceState();
                if (voiceState != null) {
                    VoiceChannel channel = voiceState.getChannel();
                    if (channel != null) {
                        getAudioPlayer(Mono.just(g)).subscribe(audio -> {
                            audio.getManager().openAudioConnection(channel);
                            audio.setHasBeenSummoned(true);
                            sink.success("I have summoned to " + channel.getName());
                        });
                    } else {
                        sink.success("You are not in a voice channel, I have no where to join...");
                    }
                }
            }

        });
    }

    @Command(description = "Roxanne Drinking game, just for fun.")
    public Flux<String> roxanne(ICommandContext commandContext) {
        return Flux.create(sink -> {
            sink.next("Starting roxanne, Disclaimer: Drink reasonably and be 18+.");
            Timer timer = new Timer();
            List<Integer> times = Arrays.asList(19000, 33000, 48000, 55000, 62000, 66000, 70000, 73000, 76000, 80000,
                    119000, 126000, 133000, 137000, 140000, 145000, 148000, 150000,
                    154000, 158000, 161000, 164000, 168000, 172000, 175000, 178000,
                    182000, 185000, 188000);

            loadAndPlay(commandContext, "https://www.youtube.com/watch?v=3T1c7GkzRQQ");
            AtomicInteger index = new AtomicInteger();
            times.forEach(t -> timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sink.next("```\nDrink" + createExplanationsMarks(index.getAndIncrement()) + "\n```");
                }

                private String createExplanationsMarks(int index) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < index; i++) {
                        stringBuilder.append("!");
                    }
                    return stringBuilder.toString();
                }
            }, t + 2000));
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sink.next("```\nDone!!! Relax, I hope your drunk now lol\n```");
                    sink.complete();
                }
            }, times.get(times.size() - 1) + 100);
        });
    }

    @Command(aliases = {"exile", "part"},
            description = "Exile the bot from the voice channel, only if your in the channel")
    public void exile(ICommandContext context) {
        runCommand(context, gmm -> {

            return Mono.create(sink -> {
                Guild g = context.getMessage().getGuild();
                //noinspection ReactiveStreamsNullableInLambdaInTransform
                discordFacade.getOurUser()
                        .map(g::getMember)
                        .subscribe(self -> {
                            try {
                                Member m = g.getMember(context.getMessage().getAuthor());
                                VoiceChannel selfChannel = Objects.requireNonNull(self.getVoiceState()).getChannel();
                                assert m != null;
                                VoiceChannel authorChannel = Objects.requireNonNull(m.getVoiceState()).getChannel();

                                assert selfChannel != null;
                                assert authorChannel != null;
                                if (selfChannel.getId().equals(authorChannel.getId())) {
                                    gmm.getManager().closeAudioConnection();
                                    gmm.setHasBeenSummoned(false);
                                    sink.success("Left Channel: " + authorChannel.getName());
                                }

                            } catch (NullPointerException e) {
                                sink.success("Unable to leave channel, please make sure the bot is in the same " +
                                        "voice channel you are in.");
                            }
                        });
            });
        });
    }

    @Command(description = "Skip a track use the value of all to clear the queue (useful for someone who has requested a playlist)", exampleValue = "all")
    @CommandExample("!skip all")
    public void skip(ICommandContext context) {
        runCommand(context, gmm -> {
            if (context.getValue().toLowerCase().contains("all")) {
                gmm.getQueue().empty();
                gmm.getQueue().pop();
                return Mono.just("Cleared Queue");
            }

            gmm.getQueue().pop();

            return Mono.just("Track skipped.");
        });
    }

    @Command(description = "Pause the current playing track.")
    public void pause(ICommandContext context) {
        runCommand(context, gmm -> {
            if (gmm.getPlayer().isPaused()) {
                return Mono.just("Bot already paused use !music-resume");
            }

            gmm.getPlayer().setPaused(true);
            return Mono.just("Track paused.");
        });
    }

    @Command(description = "Pause the current playing track.")
    public void resume(ICommandContext context) {
        runCommand(context, gmm -> {
            if (!gmm.getPlayer().isPaused()) {
                return Mono.just("Bot already playing use !music-pause");
            }

            gmm.getPlayer().setPaused(false);
            return Mono.just("Track resumed.");
        });
    }

    @Command(description = "skip to a point in a track")
    @CommandParam(value = "mins", canBeEmpty = false, exampleValue = "1")
    @CommandParam(value = "seconds", canBeEmpty = false, exampleValue = "1")
    @CommandParam(value = "hours", canBeEmpty = false, exampleValue = "1")
    public void skipTo(ICommandContext context) {
        runCommand(context, gmm -> {
            if (gmm.getPlayer().getPlayingTrack() == null) {
                return Mono.just("I am not a playing a track");
            }

            if (context.hasParameter("mins")) {
                int v = Integer.parseInt(context.getParameter("mins"));
                v = v * 1000 * 60;
                long value = Long.parseLong(Integer.toString(v));
                gmm.getPlayer().getPlayingTrack().setPosition(value);
                return Mono.just("Track skipped to " + context.getParameter("mins") + " mins.");
            }

            if (context.hasParameter("seconds")) {
                int v = Integer.parseInt(context.getParameter("seconds"));
                v = v * 1000;
                long value = Long.parseLong(Integer.toString(v));
                gmm.getPlayer().getPlayingTrack().setPosition(value);
                return Mono.just("Track skipped to " + context.getParameter("seconds") + " seconds.");
            }

            if (context.hasParameter("hours")) {
                int v = Integer.parseInt(context.getParameter("hours"));
                v = v * 1000;
                long value = Long.parseLong(Integer.toString(v));
                gmm.getPlayer().getPlayingTrack().setPosition(value);
                return Mono.just("Track skipped to " + context.getParameter("hours") + " hours.");
            }

            long value = Long.parseLong(context.getValue());

            gmm.getPlayer().getPlayingTrack().setPosition(value);

            return Mono.just("Track skipped to part.");
        });
    }

    private void runCommand(ICommandContext context, Function<GuildMusicManager, Mono<String>> consumer) {
        this.getAudioPlayer(Mono.just(context.getMessage().getGuild())).subscribe(gmm -> {
            if (!gmm.isHasBeenSummoned()) {
                context.getCommandResponse()
                        .sendString("You have not summoned me, Command will not work unless you use "
                                + this.application.getConfig().getDiscordConfig().getCommandPrefix()
                                + this.config.getNamespace() + "summon");
            } else {
                context.getCommandResponse().sendString(consumer.apply(gmm));
            }
        });
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
    public Consumer<EmbedBuilder> getEmbedObject(AudioTrack track, String desc, boolean isRequest, Guild guild) {
        return embed -> embed.setTitle(track.getInfo().title)
                .setFooter("Provided by BabbleBot 2020. Authored by Ben Davies")
                //.setThumbnail(JSONUtils.getVideoThumbnail(track.getInfo().identifier))
                .setColor(Objects.requireNonNull(guild.getMember(Objects.requireNonNull(discordFacade.getOurUser().block())))
                        .getColor())
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
    public long getLengthOfQueue(Guild guild, AudioTrack requestedTrack) {
        AtomicLong time = new AtomicLong();
        getAudioPlayer(Mono.just(guild)).doOnNext(gmm -> {
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
    public String getQueueTimeFormatted(Guild guild, AudioTrack track) {
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

    private void sendEmbedMessage(MessageChannel textChannel, Consumer<EmbedBuilder> embed) {
        EmbedBuilder builder = new EmbedBuilder();
        embed.accept(builder);
        textChannel.sendMessage(builder.build()).submit();
    }

    private void loadAndPlay(final ICommandContext commandContext, String value) {
        runCommand(commandContext, gmm -> {
            if (commandContext.getValue().equals("") && value.equals("")) {
                if (gmm.getPlayer().isPaused()) {
                    gmm.getPlayer().setPaused(false);
                    return Mono.just("Resumed playing: " + gmm.getPlayer().getPlayingTrack().getInfo().title);
                } else {
                    return Mono.just("Please provide a url to play from.");
                }
            }

            RequestStrategy strategy = RequestStrategyFactory.makeRequestStrategy(config, application, value);
            return Mono.create(sink -> strategy.handle(config, value, playerManager).subscribe(mt -> {

                // This means it is a continuous response.
                if (mt.isOptions()) {
                    Guild g = commandContext.getMessage().getGuild();
                    Optional<ContinuousResponse> found = responses.stream()
                            .filter(r -> r.getGuildId().equals(g.getId()))
                            .filter(r -> r.getChannelId().equals(commandContext.getMessage().getChannel().getId()))
                            .findAny();
                    found.ifPresent(responses::remove);

                    ContinuousResponse continuousResponse = new ContinuousResponse(g.getId(),
                            commandContext.getMessage().getChannel().getId(),
                            Arrays.stream(mt.getOptions()).map(o -> o.split("&")[1])
                                    .toArray(String[]::new));
                    responses.add(continuousResponse);

                    commandContext.getCommandResponse().sendEmbed(spec -> {
                        spec.setTitle("Here is what has been found: (Pick one)");
                        StringBuilder sb = new StringBuilder("```md\n");
                        AtomicInteger index = new AtomicInteger(1);
                        Arrays.stream(mt.getOptions()).map(o -> o.split("&")[0]).forEach(o ->
                                sb.append("[").append(index.getAndIncrement()).append("]: ").append(o).append("\n"));
                        sb.append("[c]: cancel \n");
                        sb.append("```");
                        spec.setDescription(sb.toString());
                    });
                    sink.success();
                } else if (mt.isPlaylist()) {
                    AudioPlaylist playlist = mt.getPlaylist();
                    Message message = commandContext.getMessage();
                    if (commandContext.hasParameter("loadPlaylist")) {

                        playlist.getTracks().forEach(gmm.getQueue()::enQueue);
                        sink.success("Loaded Playlist: " + playlist.getTracks().size() + " Tracks.");
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

            }, (t) -> sink.success("The url you are trying to request failed, please try a different source.")));
        });
    }

    @Override
    public void onReload() {

    }

    @Override
    public void onBoot(IPluginSettings settings) {

        if (config.getYoutubeApiToken().contains("getting-started")) {
            log.warn("Youtube search has been disabled, please enter your youtube api key into the config.");
        }

        settings.setNamespace(config.getNamespace());

        discordFacade.registerEventHandler(GuildMessageReceivedEvent.class, (e) -> {
            if (!e.getMessage().getAuthor().isBot() && !e.getMessage()
                    .getContentStripped().contains(application.getConfig().getDiscordConfig().getCommandPrefix())) {
                Guild g = e.getGuild();
                Optional<ContinuousResponse> sr = responses.stream()
                        .filter(c -> c.getChannelId().equals(e.getMessage().getChannel().getId())
                                && c.getGuildId().equals(g.getId()))
                        .findFirst();
                sr.ifPresent(continuousResponse -> handleContinuousMessage(continuousResponse, e.getMessage()));
            }
        });
    }


    private void handleContinuousMessage(ContinuousResponse continuousResponse, Message m) {
        log.info("Handling continuous response...");
        String content = m.getContentStripped().trim();
        try {
            int idx = Integer.parseInt(content) - 1;
            String[] options = continuousResponse.getOptions();

            if (idx > 2 || idx < 0) {
                m.getChannel().sendMessage("Invalid option please request again. End of request...")
                        .submit();
                responses.remove(continuousResponse);
                return;
            }

            String url = options[idx];

            DiscordMessageParser parser = new DiscordMessageParser(m);
            loadAndPlay(parser.parseString(url), url);

        } catch (NumberFormatException e) {
            if (content.toLowerCase().equals("c")) {
                m.getChannel().sendMessage("" +
                        "Command Canceled...")
                        .submit();
            } else {
                m.getChannel().sendMessage("Invalid option please request again. End of request...")
                        .submit();
            }
        }

        responses.remove(continuousResponse);
    }

    @Override
    public void onShutdown() {
    }
}
