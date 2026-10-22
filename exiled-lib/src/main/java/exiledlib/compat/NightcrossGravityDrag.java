package exiledlib.compat;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;

public final class NightcrossGravityDrag {

    public static final String MOD_SUFFIX = "_gravitydrag_target_data";

    private NightcrossGravityDrag() {
    }

    public static float percentOn(ShipAPI target) {
        MutableStat.StatMod drag = target.getMutableStats().getTimeMult().getMultMods().get(target.getId() + MOD_SUFFIX);
        return drag == null ? 0f : Math.max(0f, (1f - drag.getValue()) * 100f);
    }
}
