package exiledsector.ui.infestation;

import java.awt.Color;

import static exiledsector.ui.infestation.InfestationHash.unit;

final class ShroudPuffs {

    static final Color SHROUD = new Color(175, 0, 28);
    static final Color SHROUD_FLASH = new Color(200, 10, 20);
    static final Color EYE_GLOW = new Color(255, 0, 50);
    static final Color DARKNESS = new Color(6, 0, 3);
    static final Color DEEP_PURPLE = new Color(40, 0, 24);
    static final float FLASH_SLOT_SECONDS = 4f;
    static final float FLASH_SECONDS = 0.9f;
    static final float FLASH_SIZE_MULT = 1.2f;
    private static final int CELL_SALT = 0x0CE1;
    private static final int FLASH_SALT = 0x0F1A;

    private ShroudPuffs() {
    }

    static int lifeIndex(float atSeconds, float lifeSeconds, float lifeOffset) {
        return (int) Math.floor(atSeconds / lifeSeconds + lifeOffset);
    }

    static float age(float atSeconds, float lifeSeconds, float lifeOffset) {
        return InfestationHash.fraction(atSeconds / lifeSeconds + lifeOffset);
    }

    static float fade(float age) {
        return (float) Math.sin(Math.PI * age);
    }

    static int cell(int puffKey, int life) {
        return (int) (unit(puffKey, life, CELL_SALT) * InfestationSprites.SHROUD_SHEET_CELLS);
    }

    static float flash(int puffKey, float atSeconds, float chance) {
        float slotted = atSeconds / FLASH_SLOT_SECONDS + unit(puffKey, 0, FLASH_SALT);
        int slot = (int) Math.floor(slotted);
        float intoSlotSeconds = (slotted - slot) * FLASH_SLOT_SECONDS;
        if (intoSlotSeconds >= FLASH_SECONDS || unit(puffKey, slot, FLASH_SALT + 1) >= chance) {
            return 0f;
        }
        return (float) Math.sin(Math.PI * intoSlotSeconds / FLASH_SECONDS);
    }
}
