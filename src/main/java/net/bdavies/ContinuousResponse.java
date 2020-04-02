package net.bdavies;

import discord4j.core.object.util.Snowflake;
import lombok.Getter;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public class ContinuousResponse {

    @Getter
    private final Snowflake guildId;
    @Getter
    private final Snowflake channelId;
    @Getter
    private final String[] options;

    public ContinuousResponse(Snowflake guildId, Snowflake channelId, String[] options) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.options = options;
    }
}
