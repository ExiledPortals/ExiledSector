package exiledsector.skills.skilleffect.movement;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.dweller.ShroudedMantleHullmod;
import com.fs.starfarer.api.impl.combat.dweller.ShroudedMantleHullmod.ShroudedMantleHullmodData;
import exiledsector.compat.PrivateField;

final class MantleLunge {

    private static final PrivateField<ShroudedMantleHullmodData> LUNGING = new PrivateField<>(ShroudedMantleHullmodData.class,
            "lunging", boolean.class, "Shrouded Mantle's lunge state is unreachable; Convulsive bursts no longer wait for its lunge");

    private MantleLunge() {
    }

    static boolean isLunging(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return false;
        }
        Object data = engine.getCustomData().get(ShroudedMantleHullmod.DATA_KEY + "_" + ship.getId());
        return data instanceof ShroudedMantleHullmodData mantleData && Boolean.TRUE.equals(LUNGING.read(mantleData));
    }
}
