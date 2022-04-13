package org.bf2.arch.bot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ArchBotConfig {

    public static class TeamRef {
        final String org; //bf2
        final String team; //architects

        public TeamRef(@JsonProperty("org") String org,
                       @JsonProperty("team") String team) {
            this.org = org;
            this.team = team;
        }
    }

    final TeamRef adrCreationApprovers;

    @JsonCreator
    public ArchBotConfig(
            @JsonProperty("adrCreationApprovers") TeamRef adrCreationApprovers) {
        this.adrCreationApprovers = adrCreationApprovers;
    }
}
