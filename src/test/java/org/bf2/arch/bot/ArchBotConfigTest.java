package org.bf2.arch.bot;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchBotConfigTest {

    @Test
    public void testConfig() throws IOException {
        ObjectMapper mapper = new YAMLMapper();
        ArchBotConfig archBotConfig = mapper.readValue(this.getClass().getResource("/config.yaml"), ArchBotConfig.class);
        assertEquals("my-org", archBotConfig.adrCreationApprovers.org);
        assertEquals("my-team", archBotConfig.adrCreationApprovers.team);
    }

}