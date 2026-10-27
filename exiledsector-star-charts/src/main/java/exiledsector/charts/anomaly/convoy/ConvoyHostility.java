package exiledsector.charts.anomaly.convoy;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;

import java.util.List;

final class ConvoyHostility {

    static final List<String> HOSTILE_FLAGS = List.of(MemFlags.MEMORY_KEY_MAKE_HOSTILE, MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE,
            MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON);
    static final List<String> UNREPUTABLE_FLAGS = List.of(MemFlags.MEMORY_KEY_NO_REP_IMPACT, MemFlags.MEMORY_KEY_LOW_REP_IMPACT);

    private ConvoyHostility() {
    }

    static void makePeaceful(CampaignFleetAPI fleet) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        HOSTILE_FLAGS.forEach(memory::unset);
        memory.set(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true);
        UNREPUTABLE_FLAGS.forEach(flag -> memory.set(flag, true));
    }

    static void makeHostile(CampaignFleetAPI fleet, ConvoyRole role) {
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.unset(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
        memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        memory.set(MemFlags.MEMORY_KEY_SAW_PLAYER_WITH_TRANSPONDER_ON, true);
        if (role != ConvoyRole.FREIGHTERS) {
            memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
            memory.set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
        }
        UNREPUTABLE_FLAGS.forEach(flag -> memory.set(flag, true));
    }

    static boolean isPeaceful(CampaignFleetAPI fleet) {
        return fleet != null && fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE);
    }
}
