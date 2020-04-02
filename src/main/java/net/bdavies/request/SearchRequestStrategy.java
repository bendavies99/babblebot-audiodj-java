package net.bdavies.request;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.bdavies.MusicBotPluginConfig;
import net.bdavies.music.MusicTrack;
import net.bdavies.music.Video;
import net.bdavies.music.YoutubeMusicTrack;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public class SearchRequestStrategy implements RequestStrategy {
    @Override
    public Mono<MusicTrack> handle(MusicBotPluginConfig config, String request, AudioPlayerManager manager) {
        List<Video> videos = getVideosFromSearch(config, request).subList(0, 3);
        return Mono.just(new YoutubeMusicTrack(videos.stream().map(v ->
                v.getSnippet().getTitle().trim() + "&https://www.youtube.com/watch?v=" +
                        v.getId().getVideoId()).toArray(String[]::new)));
    }

    private List<Video> getVideosFromSearch(MusicBotPluginConfig config, String request) {
        ArrayList<Video> videos = new ArrayList<>();
        String url = "https://content.googleapis.com/youtube/v3/search?maxResults=3" +
                "&type=video&order=relevance&part=snippet&q="
                + URLEncoder.encode(request, StandardCharsets.UTF_8) + "&key=" + config.getYoutubeApiToken();

        Gson gson = new GsonBuilder().create();

        try {
            Reader reader = new InputStreamReader(new URL(url).openStream());

            ListRequest r = gson.fromJson(reader, ListRequest.class);
            videos.addAll(r.items);

        } catch (IOException e) {
            e.printStackTrace();
        }


        return videos;
    }

    private static class ListRequest {
        List<Video> items;
    }
}
