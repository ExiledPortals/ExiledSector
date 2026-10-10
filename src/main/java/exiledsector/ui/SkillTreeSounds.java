package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import exiledsector.ModSettings;
import exiledsector.skills.SkillTier;

public final class SkillTreeSounds {

    public static final String ENABLED_FIELD_ID = "exiledSector_skillTreeSounds";
    public static final boolean DEFAULT_ENABLED = true;

    static final String ALLOCATE_SMALL = "technology1";
    static final String ALLOCATE_MEDIUM = "technology3";
    static final String ALLOCATE_LARGE = "technology5";
    static final String DEALLOCATE = "ui_char_decrease_skill";
    static final String REFUSED = "ui_char_can_not_increase_skill_or_aptitude";
    static final String HYPERSPACE_OUT = "ui_sustained_burn_on";
    static final String HYPERSPACE_IN = "ui_sustained_burn_off";
    static final String WORMHOLE_JUMP = "ui_slipsurge_off";
    static final String PANEL_OPEN = "ui_select_command_ui_icon";
    static final String SOCKET = "ui_industry_install_any_item";
    static final String CRAFT = "ui_cargo_machinery_drop";
    static final long NODE_SOUND_GAP_NANOS = 100_000_000L;

    private static long lastNodeSoundNanos = Long.MIN_VALUE;

    private SkillTreeSounds() {
    }

    public static void allocated(SkillTier tier) {
        playNodeSound(switch (tier) {
            case KEYSTONE, ROOT -> ALLOCATE_LARGE;
            case NOTABLE, WORMHOLE -> ALLOCATE_MEDIUM;
            case SMALL, SOCKET -> ALLOCATE_SMALL;
        });
    }

    public static void deallocated() {
        playNodeSound(DEALLOCATE);
    }

    public static void refused() {
        playNodeSound(REFUSED);
    }

    public static void hyperspaceOut() {
        play(HYPERSPACE_OUT);
    }

    public static void hyperspaceIn() {
        play(HYPERSPACE_IN);
    }

    public static void wormholeJumped() {
        play(WORMHOLE_JUMP);
    }

    public static void panelOpened() {
        play(PANEL_OPEN);
    }

    public static void socketed() {
        play(SOCKET);
    }

    public static void crafted() {
        play(CRAFT);
    }

    static void resetForTests() {
        lastNodeSoundNanos = Long.MIN_VALUE;
    }

    private static void playNodeSound(String soundId) {
        long now = System.nanoTime();
        if (lastNodeSoundNanos != Long.MIN_VALUE && now - lastNodeSoundNanos < NODE_SOUND_GAP_NANOS) {
            return;
        }
        lastNodeSoundNanos = now;
        play(soundId);
    }

    static boolean enabled() {
        return ModSettings.booleanOr(ENABLED_FIELD_ID, DEFAULT_ENABLED);
    }

    static void play(String soundId) {
        SoundPlayerAPI player = Global.getSoundPlayer();
        if (player != null && enabled()) {
            player.playUISound(soundId, 1f, 1f);
        }
    }
}
