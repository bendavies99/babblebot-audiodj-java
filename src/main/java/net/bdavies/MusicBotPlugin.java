package net.bdavies;

import uk.co.bjdavies.api.command.Command;
import uk.co.bjdavies.api.command.ICommandContext;
import uk.co.bjdavies.api.plugins.IPlugin;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public class MusicBotPlugin implements IPlugin {
    @Override
    public String getName() {
        return "MusicBot";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "Ben <ben.davies99@outlook.com>";
    }

    @Override
    public String getMinimumServerVersion() {
        return "1.2.7";
    }

    @Override
    public String getMaximumServerVersion() {
        return "0";
    }

    @Override
    public String getNamespace() {
        return "music-";
    }

    @Override
    public void onReload() {
    }

    @Override
    public void onBoot() {

    }

    @Override
    public void onShutdown() {

    }

    @Command(description = "describe the music bot")
    public String about(ICommandContext commandContext) {
        return "Music Bot, Ben Davies";
    }
}
