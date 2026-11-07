package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.skills.skilleffect.DamagePercentListener;

final class AccretionListener extends DamagePercentListener implements DamageListener {

    static final String DAMAGE_PERCENT_PER_STACK_KEY = "exiledSector_accretionDamagePercentPerStack";
    private static final String MOD_ID_PREFIX = "exiledSector_accretionDamage_";

    private final AccretionStacks stacks;
    private boolean lossCounted;

    AccretionListener(ShipAPI fighter, ShipAPI carrier) {
        super(fighter, carrier, MOD_ID_PREFIX);
        this.stacks = AccretionStacks.of(carrier);
    }

    private static float combatTime() {
        CombatEngineAPI engine = Global.getCombatEngine();
        return engine == null ? 0f : engine.getTotalElapsedTime(false);
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (lossCounted || target != ownerShip || ownerShip.getHitpoints() > 0f) {
            return;
        }
        lossCounted = true;
        stacks.add(combatTime());
    }

    @Override
    protected float damagePercent(Object param, CombatEntityAPI target) {
        float percentPerStack = magnitude(DAMAGE_PERCENT_PER_STACK_KEY);
        return percentPerStack <= 0f ? 0f : percentPerStack * stacks.activeAt(combatTime());
    }
}
