package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.HitSideEffectListener;
import org.lwjgl.util.vector.Vector2f;

final class ArmorBurnListener extends HitSideEffectListener {

    static final String BURN_PERCENT_KEY = "exiledSector_armorBurnDamagePercent";
    static final float BURN_SECONDS = 3f;

    ArmorBurnListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    protected void onHit(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        ShipAPI targetShip = CombatQueries.hostileShip(ownerShip, target);
        if (shieldHit || targetShip == null || CombatQueries.weaponBehind(param) == null) {
            return;
        }
        float burn = CombatQueries.hitDamage(damage) * magnitude(BURN_PERCENT_KEY) / 100f
                * targetShip.getMutableStats().getArmorDamageTakenMult().getModifiedValue();
        ArmorGridAPI grid = targetShip.getArmorGrid();
        int[] cell = burn > 0f && grid != null ? grid.getCellAtLocation(point) : null;
        if (cell != null) {
            ArmorDecay.on(targetShip).queue(grid, cell[0], cell[1], burn, BURN_SECONDS);
        }
    }
}
