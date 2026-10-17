package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class WingSharedFateListener implements DamageTakenModifier, DamageListener {

    static final String SHARE_MOD_ID = "exiledSector_wingSharedFate";
    static final float MAX_SHARED_PERCENT = 100f;
    private static final float KILLING_BLOW_DAMAGE = 1_000_000f;

    private static boolean applyingSharedDamage;

    private final ShipAPI fighter;
    private final float sharedPercent;
    private final List<ShipAPI> pendingWingmates = new ArrayList<>();
    private Object pendingSource;
    private float pendingRatio;
    private float pendingKeptFraction;
    private boolean sharePending;

    private WingSharedFateListener(ShipAPI fighter, float sharedPercent) {
        this.fighter = fighter;
        this.sharedPercent = sharedPercent;
    }

    static void attach(ShipAPI fighter, float sharedPercent) {
        if (fighter != null && sharedPercent > 0f && !fighter.hasListenerOfClass(WingSharedFateListener.class)) {
            fighter.addListener(new WingSharedFateListener(fighter, sharedPercent));
        }
    }

    record Split(float keptFraction, float perWingmateRatio) {
    }

    static Split split(float sharedPercent, int wingmates) {
        if (wingmates <= 0 || sharedPercent <= 0f) {
            return new Split(1f, 0f);
        }
        int wingSize = wingmates + 1;
        float sharedFraction = Math.min(sharedPercent, MAX_SHARED_PERCENT) / 100f * wingmates / wingSize;
        float keptFraction = 1f - sharedFraction;
        return new Split(keptFraction, sharedFraction / wingmates / keptFraction);
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        sharePending = false;
        if (applyingSharedDamage) {
            return null;
        }
        List<ShipAPI> wingmates = wingmates();
        Split split = split(sharedPercent, wingmates.size());
        if (split.perWingmateRatio() <= 0f) {
            return null;
        }
        damage.getModifier().modifyMult(SHARE_MOD_ID, split.keptFraction());
        pendingWingmates.clear();
        pendingWingmates.addAll(wingmates);
        pendingSource = param;
        pendingRatio = split.perWingmateRatio();
        pendingKeptFraction = split.keptFraction();
        sharePending = true;
        return SHARE_MOD_ID;
    }

    static boolean tooBigToShare(float wholeHit, float fighterMaximum) {
        return wholeHit > 0f && wholeHit >= fighterMaximum;
    }

    private static float fluxCapacity(ShipAPI ship) {
        FluxTrackerAPI flux = ship.getFluxTracker();
        return flux == null ? 0f : flux.getMaxFlux();
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (!sharePending || fighter.getParamAboutToApplyDamage() != pendingSource) {
            return;
        }
        sharePending = false;
        float keptFlux = result.getDamageToShields();
        float keptHull = result.getDamageToHull();
        float sharedFlux = keptFlux * pendingRatio;
        float sharedHull = keptHull * pendingRatio;
        if (tooBigToShare(keptFlux / pendingKeptFraction, fluxCapacity(fighter))) {
            if (fighter.getFluxTracker() != null) {
                fighter.getFluxTracker().increaseFlux(keptFlux / pendingKeptFraction - keptFlux, true);
            }
            sharedFlux = 0f;
        }
        if (tooBigToShare(keptHull / pendingKeptFraction, fighter.getMaxHitpoints())) {
            damageHull(fighter, keptHull / pendingKeptFraction - keptHull, fighter);
            sharedHull = 0f;
        }
        for (ShipAPI wingmate : pendingWingmates) {
            if (!CombatQueries.isAliveNotHulk(wingmate)) {
                continue;
            }
            if (sharedFlux > 0f && wingmate.getFluxTracker() != null) {
                wingmate.getFluxTracker().increaseFlux(sharedFlux, true);
            }
            if (sharedHull > 0f) {
                damageHull(wingmate, sharedHull, fighter);
            }
        }
        pendingWingmates.clear();
    }

    private List<ShipAPI> wingmates() {
        FighterWingAPI wing = fighter.getWing();
        List<ShipAPI> wingmates = new ArrayList<>();
        if (wing == null) {
            return wingmates;
        }
        for (ShipAPI member : wing.getWingMembers()) {
            if (member != fighter && CombatQueries.isAliveNotHulk(member) && !member.isFinishedLanding()) {
                wingmates.add(member);
            }
        }
        return wingmates;
    }

    private static void damageHull(ShipAPI wingmate, float hullDamage, ShipAPI sharingFighter) {
        if (wingmate.getHitpoints() > hullDamage) {
            wingmate.setHitpoints(wingmate.getHitpoints() - hullDamage);
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        applyingSharedDamage = true;
        try {
            engine.applyDamage(wingmate, new Vector2f(wingmate.getLocation()), KILLING_BLOW_DAMAGE, DamageType.ENERGY, 0f, true, false, sharingFighter);
        } finally {
            applyingSharedDamage = false;
        }
    }
}
