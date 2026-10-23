package exerelin.utilities;

import java.util.HashMap;
import java.util.Map;

public class NexConfig {

    public static final Map<String, NexFactionConfig> CONFIGS_BY_FACTION = new HashMap<>();

    public static NexFactionConfig getFactionConfig(String factionId, boolean useDefault) {
        NexFactionConfig config = CONFIGS_BY_FACTION.get(factionId);
        return config == null && useDefault ? new NexFactionConfig() : config;
    }
}
