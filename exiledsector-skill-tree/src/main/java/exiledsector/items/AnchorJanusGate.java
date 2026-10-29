package exiledsector.items;

import com.fs.starfarer.api.campaign.SectorAPI;
import exiledsector.ModSettings;
import exiledsector.skills.tags.SkillTags;

public final class AnchorJanusGate {

    public static final String ALLOW_BEFORE_JANUS_FIELD_ID = "exiledSector_anchorsBeforeJanus";
    public static final boolean DEFAULT_ALLOW_BEFORE_JANUS = false;

    private AnchorJanusGate() {
    }

    public static boolean open(SectorAPI sector) {
        return JanusDevice.obtained(sector) || ModSettings.booleanOr(ALLOW_BEFORE_JANUS_FIELD_ID, DEFAULT_ALLOW_BEFORE_JANUS);
    }

    public static boolean applies(String definitionId) {
        return definitionId != null && !SkillTags.EXOTECH_REGION.equals(WormholeAnchorDefinitions.volume(definitionId));
    }

    public static boolean withholds(String definitionId, SectorAPI sector) {
        return applies(definitionId) && !open(sector);
    }
}
