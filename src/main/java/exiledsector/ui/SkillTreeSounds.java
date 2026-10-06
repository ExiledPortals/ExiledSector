package exiledsector.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import exiledsector.skills.SkillTier;

public final class SkillTreeSounds {

    static final String ALLOCATE_SMALL = "technology1";
    static final String ALLOCATE_MEDIUM = "technology3";
    static final String ALLOCATE_LARGE = "technology5";
    static final String DEALLOCATE = "ui_char_decrease_skill";
    static final String HYPERSPACE_OUT = "ui_sustained_burn_on";
    static final String HYPERSPACE_IN = "ui_sustained_burn_off";
    static final String WORMHOLE_JUMP = "ui_slipsurge_off";
    static final String PANEL_OPEN = "ui_select_command_ui_icon";
    static final String SOCKET = "ui_industry_install_any_item";
    static final long NODE_SOUND_GAP_NANOS = 100_000_000L;

    private static long lastNodeSound = Long.MIN_VALUE;

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

    static void resetForTests() {
        lastNodeSound = Long.MIN_VALUE;
    }

    private static void playNodeSound(String soundId) {
        long now = System.nanoTime();
        if (lastNodeSound != Long.MIN_VALUE && now - lastNodeSound < NODE_SOUND_GAP_NANOS) {
            return;
        }
        lastNodeSound = now;
        play(soundId);
    }

    private static void play(String soundId) {
        SoundPlayerAPI player = Global.getSoundPlayer();
        if (player != null) {
            player.playUISound(soundId, 1f, 1f);
        }
    }
}
