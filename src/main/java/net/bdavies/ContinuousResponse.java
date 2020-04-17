package net.bdavies;

import lombok.Getter;

/**
 * @author ben.davies99@outlook.com (Ben Davies)
 * @since 1.0.0
 */
public class ContinuousResponse {

    @Getter
    private final String guildId;
    @Getter
    private final String channelId;
    @Getter
    private final String[] options;

    public ContinuousResponse(String guildId, String channelId, String[] options) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.options = options;
    }
}
