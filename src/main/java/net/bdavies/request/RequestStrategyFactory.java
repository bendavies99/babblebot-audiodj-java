package net.bdavies.request;

import uk.co.bjdavies.api.IApplication;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public class RequestStrategyFactory {
    public static RequestStrategy makeRequestStrategy(IApplication app, String url) {
        return app.get(StandardRequestStrategy.class);
    }
}
