package exiledsector.skills.npc;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.impl.campaign.intel.PersonBountyIntel;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator.CustomBountyData;
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionFleetAutoDespawn;
import exiledsector.ui.refit.UiReflection;
import org.apache.log4j.Logger;

import java.util.List;

public final class NpcBounties {

    static final String MAGICLIB_TARGET_TAG = "MagicLib_Bounty_target_fleet";
    static final String MAGICLIB_TARGET_MEMORY_KEY = "$" + MAGICLIB_TARGET_TAG;
    static final int PERSON_BOUNTY_TOP_LEVEL = 10;
    static final float MAGICLIB_DIFFICULTY = 1f;
    private static final String DESPAWN_MISSION_FIELD = "mission";
    private static final List<String> CUSTOM_BOUNTY_DATA_FIELDS = List.of("data", "dataLow", "dataNormal", "dataHigh");
    private static final Logger LOG = Logger.getLogger(NpcBounties.class);

    private NpcBounties() {
    }

    public record Bounty(float difficulty, boolean personBounty) {
    }

    public static Bounty find(CampaignFleetAPI fleet) {
        if (fleet == null) {
            return null;
        }
        List<FleetEventListener> listeners = fleet.getEventListeners();
        if (listeners != null) {
            for (FleetEventListener listener : listeners) {
                if (listener instanceof PersonBountyIntel personBounty) {
                    return new Bounty(personBountyDifficulty(personBounty.getLevel()), true);
                }
                Float listenerDifficulty = listener instanceof BaseCustomBounty customBounty ? customBountyDifficulty(customBounty, fleet) : null;
                if (listenerDifficulty != null) {
                    return new Bounty(listenerDifficulty, false);
                }
            }
        }
        List<EveryFrameScript> scripts = fleet.getScripts();
        if (scripts != null) {
            for (EveryFrameScript script : scripts) {
                if (script instanceof MissionFleetAutoDespawn
                        && missionOf(script) instanceof BaseCustomBounty customBounty) {
                    Float scriptDifficulty = customBountyDifficulty(customBounty, fleet);
                    if (scriptDifficulty != null) {
                        return new Bounty(scriptDifficulty, false);
                    }
                }
            }
        }
        if (fleet.hasTag(MAGICLIB_TARGET_TAG)
                || fleet.getMemoryWithoutUpdate() != null && fleet.getMemoryWithoutUpdate().getBoolean(MAGICLIB_TARGET_MEMORY_KEY)) {
            return new Bounty(MAGICLIB_DIFFICULTY, false);
        }
        return null;
    }

    static float personBountyDifficulty(int bountyLevel) {
        return clampedDifficulty(bountyLevel / (float) PERSON_BOUNTY_TOP_LEVEL);
    }

    static float customBountyDifficulty(int difficulty) {
        int span = CustomBountyCreator.MAX_DIFFICULTY - CustomBountyCreator.MIN_DIFFICULTY;
        return span <= 0 ? 1f : clampedDifficulty((difficulty - CustomBountyCreator.MIN_DIFFICULTY) / (float) span);
    }

    private static Float customBountyDifficulty(BaseCustomBounty customBounty, CampaignFleetAPI fleet) {
        for (String fieldName : CUSTOM_BOUNTY_DATA_FIELDS) {
            CustomBountyData bountyData = readField(customBounty, fieldName, CustomBountyData.class);
            if (bountyData != null && bountyData.fleet == fleet) {
                return customBountyDifficulty(bountyData.difficulty);
            }
        }
        return null;
    }

    private static Object missionOf(EveryFrameScript despawnScript) {
        return readField(despawnScript, DESPAWN_MISSION_FIELD, Object.class);
    }

    // a reflection failure must never stop a fleet from levelling, so any throwable falls back to "not a bounty"
    @SuppressWarnings("java:S1181")
    private static <T> T readField(Object target, String fieldName, Class<T> type) {
        try {
            return UiReflection.fieldNamed(target, fieldName, type);
        } catch (Throwable e) {
            LOG.debug("Could not read " + fieldName + " from " + target.getClass().getName(), e);
            return null;
        }
    }

    private static float clampedDifficulty(float difficulty) {
        return Math.max(0f, Math.min(1f, difficulty));
    }
}
