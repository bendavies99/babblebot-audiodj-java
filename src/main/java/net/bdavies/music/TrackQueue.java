package net.bdavies.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeFormatInfo;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackFormat;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import uk.co.bjdavies.api.IApplication;
import uk.co.bjdavies.api.discord.IDiscordFacade;

import java.util.List;
import java.util.StringJoiner;
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

    private final IDiscordFacade facade;
    private final IApplication application;

    public TrackQueue(AudioPlayer player, IDiscordFacade facade, IApplication application) {
        this.player = player;
        this.facade = facade;
        this.application = application;
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

        if (underlyingQueue.size() == 0) {
            facade.updateBotPlayingText(application.getConfig()
                    .getDiscordConfig().getPlayingText());
        }

        if (endReason.mayStartNext) {
            pop();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        assert track != null;
        log.error("An error occurred while loading track: " + track.getIdentifier(), exception);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
//        if (track instanceof YoutubeAudioTrack) {
//            YoutubeAudioSourceManager sourceManager = (YoutubeAudioSourceManager) track.getSourceManager();
//            YoutubeTrackDetails details = sourceManager.getTrackDetailsLoader().loadDetails(sourceManager.getHttpInterface(), track.getIdentifier());
//            List<YoutubeTrackFormat> formats = details.getFormats(sourceManager.getHttpInterface(), sourceManager.getSignatureResolver());
//            YoutubeTrackFormat format = findBestSupportedFormat(formats);
//            if (format.getInfo().mimeType.split("/")[0].equals("video")) {
//                try {
//                    //URI signedUrl = sourceManager.getSignatureResolver().resolveFormatUrl(sourceManager.getHttpInterface(), details.getPlayerScript(), format);
//                    //log.info("Loading url {}", signedUrl.toString());
//
////                    FFmpeg fFmpeg = new FFmpeg(Paths.get("/usr/bin/ffmpeg"));
////                    fFmpeg.addInput(UrlInput.fromUrl(signedUrl.toString()));
////                    fFmpeg.addArguments("-f", "flv");
////                    fFmpeg.addArgument("-c:v");
////                    fFmpeg.addArgument("libx264");
////                    fFmpeg.addArgument("-c:a");
////                    fFmpeg.addArgument("copy");
////                    fFmpeg.addArgument("-ac");
////                    fFmpeg.addArgument("1");
////                    fFmpeg.addOutput(UrlOutput.toUrl("rtmp://hls.aaronburt.co.uk/live/audio?password=a_secret_password"));
////                    fFmpeg.execute();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
        facade.updateBotPlayingText("track: " + track.getInfo().title);
        super.onTrackStart(player, track);
    }

    private static boolean isBetterFormat(YoutubeTrackFormat format, YoutubeTrackFormat other) {
        YoutubeFormatInfo info = format.getInfo();

        if (info == null) {
            return false;
        } else if (other == null) {
            return true;
        } else if (info.ordinal() != other.getInfo().ordinal()) {
            return info.ordinal() > other.getInfo().ordinal();
        } else {
            return format.getBitrate() > other.getBitrate();
        }
    }

    private static YoutubeTrackFormat findBestSupportedFormat(List<YoutubeTrackFormat> formats) {
        YoutubeTrackFormat bestFormat = null;

        for (YoutubeTrackFormat format : formats) {
            if (isBetterFormat(format, bestFormat)) {
                bestFormat = format;
            }
        }

        if (bestFormat == null) {
            StringJoiner joiner = new StringJoiner(", ");
            formats.forEach(format -> joiner.add(format.getType().toString()));
            throw new IllegalStateException("No supported audio streams available, available types: " + joiner.toString());
        }

        return bestFormat;
    }
}
