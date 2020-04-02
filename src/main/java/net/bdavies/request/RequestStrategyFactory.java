package net.bdavies.request;

import net.bdavies.MusicBotPluginConfig;
import uk.co.bjdavies.api.IApplication;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public class RequestStrategyFactory {
    public static RequestStrategy makeRequestStrategy(MusicBotPluginConfig config, IApplication app, String url) {
        if (!config.getYoutubeApiToken().contains("https://")) {
            if (url.contains("http://") || url.contains("https://") || url.contains("youtube.com")) {
                return app.get(StandardRequestStrategy.class);
            } else return app.get(SearchRequestStrategy.class);
        } else {
            return app.get(StandardRequestStrategy.class);
        }
    }
}
