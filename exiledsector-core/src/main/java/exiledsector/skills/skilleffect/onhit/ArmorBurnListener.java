package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.ShipCombatListener;
import org.lwjgl.util.vector.Vector2f;

final class ArmorBurnListener extends ShipCombatListener implements DamageDealtModifier {

    static final String BURN_PERCENT_KEY = "exiledSector_armorBurnDamagePercent";
    static final float BURN_SECONDS = 3f;

    ArmorBurnListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    // java:S3516: the engine reads a null return as "leave the damage unchanged"; the burn is queued as a side effect instead
    @SuppressWarnings("java:S3516")
    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (shieldHit || !(target instanceof ShipAPI targetShip) || CombatQueries.weaponBehind(param) == null
                || !CombatQueries.isHostile(ownerShip, targetShip)) {
            return null;
        }
        float burn = CombatQueries.hitDamage(damage) * magnitude(BURN_PERCENT_KEY) / 100f
                * targetShip.getMutableStats().getArmorDamageTakenMult().getModifiedValue();
        ArmorGridAPI grid = targetShip.getArmorGrid();
        int[] cell = burn > 0f && grid != null ? grid.getCellAtLocation(point) : null;
        if (cell != null) {
            ArmorDecay.on(targetShip).queue(grid, cell[0], cell[1], burn, BURN_SECONDS);
        }
        return null;
    }
}
