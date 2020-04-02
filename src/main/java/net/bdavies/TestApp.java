package net.bdavies;

import uk.co.bjdavies.Application;
import uk.co.bjdavies.api.IApplication;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public class TestApp {

    public static void main(String[] args) {
        IApplication application = Application.make(TestApp.class, args);
        application.getPluginContainer().addPlugin("music", application.get(MusicBotPlugin.class));
    }

}
