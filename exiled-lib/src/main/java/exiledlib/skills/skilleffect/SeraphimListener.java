package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;

import java.util.IdentityHashMap;
import java.util.Map;

final class SeraphimListener extends ShipCombatListener {

    static final String HEAL_PERCENT_KEY = "exiledSector_seraphimHealPercent";
    static final String HEAL_CAP_PERCENT_KEY = "exiledSector_seraphimHealCapPercent";
    static final String DAMAGE_BONUS_PERCENT_KEY = "exiledSector_seraphimDamageBonusPercent";
    static final String FULL_BONUS_DAMAGE_KEY = "exiledSector_seraphimFullBonusDamage";
    private static final String MOD_ID_PREFIX = "exiledSector_seraphim_";
    private static final float ZERO_FLUX_ALWAYS_ON_LEVEL = 2f;

    private float damageDealt;
    private float hullRepaired;
    private int appliedDamagePercent;
    private boolean zeroFluxUnlocked;

    SeraphimListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
        DealtDamageRouter.register(this);
    }

    float damageDealt() {
        return damageDealt;
    }

    float hullRepaired() {
        return hullRepaired;
    }

    void onDamageDealt(CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (!(target instanceof ShipAPI targetShip) || targetShip.isHulk() || !CombatQueries.isHostile(ownerShip, targetShip)
                || !ownerIsAliveNotHulk()) {
            return;
        }
        float dealt = result.getDamageToHull() + result.getTotalDamageToArmor();
        if (dealt <= 0f) {
            return;
        }
        damageDealt += dealt;
        repairHull(dealt);
        updateDamageBonus();
    }

    private void repairHull(float dealt) {
        float repairCap = ownerShip.getMaxHitpoints() * magnitude(HEAL_CAP_PERCENT_KEY) / 100f;
        float missingHull = ownerShip.getMaxHitpoints() - ownerShip.getHitpoints();
        float repair = Math.min(dealt * magnitude(HEAL_PERCENT_KEY) / 100f, Math.min(repairCap - hullRepaired, missingHull));
        if (repair > 0f) {
            ownerShip.setHitpoints(ownerShip.getHitpoints() + repair);
            hullRepaired += repair;
        }
    }

    private void updateDamageBonus() {
        float fullBonusDamage = magnitude(FULL_BONUS_DAMAGE_KEY);
        float progress = fullBonusDamage <= 0f ? 1f : Math.min(1f, damageDealt / fullBonusDamage);
        int damagePercent = Math.round(magnitude(DAMAGE_BONUS_PERCENT_KEY) * progress);
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        if (damagePercent != appliedDamagePercent) {
            appliedDamagePercent = damagePercent;
            stats.getBallisticWeaponDamageMult().modifyPercent(modId, damagePercent);
            stats.getEnergyWeaponDamageMult().modifyPercent(modId, damagePercent);
        }
        if (progress >= 1f && !zeroFluxUnlocked) {
            zeroFluxUnlocked = true;
            stats.getZeroFluxMinimumFluxLevel().modifyFlat(modId, ZERO_FLUX_ALWAYS_ON_LEVEL);
        }
    }

    static final class DealtDamageRouter implements DamageListener {

        private final Map<ShipAPI, SeraphimListener> listenersByDealer = new IdentityHashMap<>();

        static void register(SeraphimListener listener) {
            DealtDamageRouter router = inCombat() ? SkillEffectSupport.ensureEngineListener(DealtDamageRouter.class, DealtDamageRouter::new) : null;
            if (router != null) {
                router.listenersByDealer.put(listener.ownerShip, listener);
            }
        }

        static void routeDroneDamage(ShipAPI firingShip, ShipAPI drone) {
            DealtDamageRouter router = inCombat() ? SkillEffectSupport.existingEngineListener(DealtDamageRouter.class) : null;
            SeraphimListener listener = router == null ? null : router.listenersByDealer.get(firingShip);
            if (listener != null) {
                router.listenersByDealer.put(drone, listener);
            }
        }

        private static boolean inCombat() {
            return Global.getSettings() != null && Global.getCurrentState() == GameState.COMBAT && Global.getCombatEngine() != null;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (source instanceof ShipAPI dealer) {
                SeraphimListener listener = listenersByDealer.get(dealer);
                if (listener != null) {
                    listener.onDamageDealt(target, result);
                }
            }
        }
    }
}
