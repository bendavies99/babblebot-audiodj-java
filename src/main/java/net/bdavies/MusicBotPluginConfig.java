package net.bdavies;

import lombok.Getter;
import uk.co.bjdavies.api.plugins.PluginConfig;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
@PluginConfig.Setup
public class MusicBotPluginConfig {
    /**
     * This is the namespace you want the music bot commands to run e.g. music-
     */
    @Getter
    private String namespace = "";


    /**
     * This is your youtube api token to allow your users to search for youtube songs and print out youtube thumbnails
     */
    @Getter
    private String youtubeApiToken = "https://developers.google.com/youtube/v3/getting-started";
}
