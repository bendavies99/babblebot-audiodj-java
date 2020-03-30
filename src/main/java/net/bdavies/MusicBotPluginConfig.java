package net.bdavies;

import lombok.Getter;
import uk.co.bjdavies.api.plugins.PluginConfig;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
@PluginConfig.Setup
public class MusicBotPluginConfig {
    @Getter
    private String namespace = "";
}
