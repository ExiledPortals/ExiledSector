package exiledsector.skills;

import exiledsector.domain.DomainSwitch;
import exiledsector.effects.ShipTreeSync;

public final class SkillTreeSetting {

    public static final String ENABLED_FIELD_ID = "exiledSector_skillTreeEnabled";
    public static final boolean DEFAULT_ENABLED = true;
    public static final DomainSwitch SWITCH = new DomainSwitch(ENABLED_FIELD_ID, DEFAULT_ENABLED, ShipTreeSync::treeSwitched);

    private SkillTreeSetting() {
    }

    public static void reloadSettings() {
        SWITCH.reload();
    }

    public static boolean enabled() {
        return SWITCH.on();
    }

    public static void setEnabledForTests(boolean enabled) {
        SWITCH.setForTests(enabled);
    }
}
