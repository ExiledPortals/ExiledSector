package exiledlib.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import exiledlib.skills.skilleffect.CombatQueries;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

final class WingSharedFateListener implements DamageTakenModifier, DamageListener {

    static final String SHARE_MOD_ID = "exiledSector_wingSharedFate";
    static final float MAX_SHARED_PERCENT = 100f;
    static final float KILLING_BLOW_MULTIPLE = 3f;
    static final float KILLING_BLOW_HULL_LEFT = 1f;
    static final float OVERLOAD_MARGIN = 1f;
    static final String SHIELDSISTER_REDIRECT_SUFFIX = "fighterDamageNullifyer";
    static final String DAMPER_FLOOR_SUFFIX = "DamperFieldFightersScript";

    private static boolean applyingSharedDamage;

    private final ShipAPI fighter;
    private final float sharedPercent;
    private final List<ShipAPI> recipients = new ArrayList<>();
    private Object pendingParam;
    private float pendingRatio;
    private float pendingKeptFraction;
    private boolean sharePending;
    private DamageAPI empScaledDamage;
    private float unscaledEmp;

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

    static boolean isApplyingSharedDamage() {
        return applyingSharedDamage;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        restoreEmp();
        sharePending = false;
        recipients.clear();
        if (applyingSharedDamage || protectedByAnotherMod(fighter)) {
            return null;
        }
        collectRecipients(Global.getCombatEngine(), shieldHit);
        Split split = split(sharedPercent, recipients.size());
        if (split.perWingmateRatio() <= 0f) {
            return null;
        }
        damage.getModifier().modifyMult(SHARE_MOD_ID, split.keptFraction());
        if (!shieldHit && damage.getFluxComponent() > 0f) {
            empScaledDamage = damage;
            unscaledEmp = damage.getFluxComponent();
            damage.setFluxComponent(unscaledEmp / split.keptFraction());
        }
        pendingParam = param;
        pendingRatio = split.perWingmateRatio();
        pendingKeptFraction = split.keptFraction();
        sharePending = true;
        return SHARE_MOD_ID;
    }

    static boolean tooBigToShare(float wholeHit, float fighterMaximum) {
        return wholeHit > 0f && wholeHit >= fighterMaximum;
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (!sharePending) {
            return;
        }
        sharePending = false;
        restoreEmp();
        if (fighter.getParamAboutToApplyDamage() == pendingParam) {
            CombatEngineAPI engine = Global.getCombatEngine();
            shareFlux(result.getDamageToShields(), engine);
            shareHull(result.getDamageToHull(), CombatQueries.attackingShip(source, pendingParam), engine);
        }
        recipients.clear();
        pendingParam = null;
    }

    private void restoreEmp() {
        if (empScaledDamage != null) {
            empScaledDamage.setFluxComponent(unscaledEmp);
            empScaledDamage = null;
        }
    }

    private void collectRecipients(CombatEngineAPI engine, boolean shieldHit) {
        FighterWingAPI wing = fighter.getWing();
        if (wing == null) {
            return;
        }
        List<ShipAPI> members = wing.getWingMembers();
        for (int i = 0; i < members.size(); i++) {
            ShipAPI member = members.get(i);
            if (member != fighter && canReceive(member, engine, shieldHit)) {
                recipients.add(member);
            }
        }
    }

    private static boolean canReceive(ShipAPI member, CombatEngineAPI engine, boolean shieldHit) {
        if (!isOutWithWing(member, engine) || member.isPhased()) {
            return false;
        }
        boolean hasRoom = shieldHit ? hasShieldRoom(member) : hullDamageTakenMult(member) > 0f;
        return hasRoom && !protectedByAnotherMod(member);
    }

    static boolean isOutWithWing(ShipAPI member, CombatEngineAPI engine) {
        return canTakeDamage(member, engine) && !member.isLanding() && !member.isLiftingOff() && !member.isFinishedLanding();
    }

    private static boolean canTakeDamage(ShipAPI ship, CombatEngineAPI engine) {
        return CombatQueries.isAliveNotHulk(ship) && ship.getHitpoints() > 0f && (engine == null || engine.isEntityInPlay(ship));
    }

    static boolean protectedByAnotherMod(ShipAPI ship) {
        List<DamageTakenModifier> modifiers = ship.getListeners(DamageTakenModifier.class);
        if (modifiers == null) {
            return false;
        }
        for (int i = 0; i < modifiers.size(); i++) {
            String className = modifiers.get(i).getClass().getName();
            if (className.endsWith(SHIELDSISTER_REDIRECT_SUFFIX) || (className.endsWith(DAMPER_FLOOR_SUFFIX) && carrierSystemActive(ship))) {
                return true;
            }
        }
        return false;
    }

    private static boolean carrierSystemActive(ShipAPI ship) {
        FighterWingAPI wing = ship.getWing();
        ShipAPI carrier = wing == null ? null : wing.getSourceShip();
        ShipSystemAPI system = carrier == null ? null : carrier.getSystem();
        return system != null && system.isActive();
    }

    private static boolean hasShieldRoom(ShipAPI ship) {
        ShieldAPI shield = ship.getShield();
        FluxTrackerAPI flux = ship.getFluxTracker();
        return shield != null && shield.isOn() && flux != null && !flux.isOverloadedOrVenting()
                && shieldEfficiency(ship) > 0f && spareFlux(flux) > 0f;
    }

    private static float shieldEfficiency(ShipAPI ship) {
        ShieldAPI shield = ship.getShield();
        return shield == null ? 0f : shield.getFluxPerPointOfDamage() * ship.getMutableStats().getShieldDamageTakenMult().getModifiedValue();
    }

    private static float spareFlux(FluxTrackerAPI flux) {
        return Math.max(0f, flux.getMaxFlux() - flux.getCurrFlux() - OVERLOAD_MARGIN);
    }

    private static float fluxCapacity(ShipAPI ship) {
        FluxTrackerAPI flux = ship.getFluxTracker();
        return flux == null ? 0f : flux.getMaxFlux();
    }

    private static float hullDamageTakenMult(ShipAPI ship) {
        return ship.getMutableStats().getHullDamageTakenMult().getModifiedValue();
    }

    private void shareFlux(float keptFlux, CombatEngineAPI engine) {
        if (keptFlux <= 0f) {
            return;
        }
        float wholeFlux = keptFlux / pendingKeptFraction;
        float struckEfficiency = shieldEfficiency(fighter);
        float returnedFlux = 0f;
        if (tooBigToShare(wholeFlux, fluxCapacity(fighter)) || struckEfficiency <= 0f) {
            returnedFlux = wholeFlux - keptFlux;
        } else {
            float sharedDamage = keptFlux * pendingRatio / struckEfficiency;
            float wholeDamage = wholeFlux / struckEfficiency;
            float unabsorbedDamage = 0f;
            for (int i = 0; i < recipients.size(); i++) {
                unabsorbedDamage += shareFluxWith(recipients.get(i), sharedDamage, wholeDamage, engine);
            }
            returnedFlux = unabsorbedDamage * struckEfficiency;
        }
        if (returnedFlux > 0f && canTakeDamage(fighter, engine) && fighter.getFluxTracker() != null) {
            fighter.getFluxTracker().increaseFlux(returnedFlux, true);
        }
    }

    private static float shareFluxWith(ShipAPI recipient, float sharedDamage, float wholeDamage, CombatEngineAPI engine) {
        if (!canTakeDamage(recipient, engine) || !hasShieldRoom(recipient)) {
            return sharedDamage;
        }
        float recipientEfficiency = shieldEfficiency(recipient);
        if (tooBigToShare(wholeDamage * recipientEfficiency, fluxCapacity(recipient))) {
            return sharedDamage;
        }
        float wantedFlux = sharedDamage * recipientEfficiency;
        float absorbedFlux = Math.min(wantedFlux, spareFlux(recipient.getFluxTracker()));
        recipient.getFluxTracker().increaseFlux(absorbedFlux, true);
        return (wantedFlux - absorbedFlux) / recipientEfficiency;
    }

    private void shareHull(float keptHull, ShipAPI attacker, CombatEngineAPI engine) {
        if (keptHull <= 0f) {
            return;
        }
        float wholeHull = keptHull / pendingKeptFraction;
        float struckVulnerability = hullDamageTakenMult(fighter);
        float returnedHull = 0f;
        if (tooBigToShare(wholeHull, fighter.getMaxHitpoints()) || struckVulnerability <= 0f) {
            returnedHull = wholeHull - keptHull;
        } else {
            float sharedDamage = keptHull * pendingRatio / struckVulnerability;
            float wholeDamage = wholeHull / struckVulnerability;
            float unabsorbedDamage = 0f;
            for (int i = 0; i < recipients.size(); i++) {
                unabsorbedDamage += shareHullWith(recipients.get(i), sharedDamage, wholeDamage, attacker, engine);
            }
            returnedHull = unabsorbedDamage * struckVulnerability;
        }
        if (returnedHull > 0f && canTakeDamage(fighter, engine)) {
            damageHull(fighter, returnedHull, attacker, engine);
        }
    }

    private static float shareHullWith(ShipAPI recipient, float sharedDamage, float wholeDamage, ShipAPI attacker, CombatEngineAPI engine) {
        float recipientVulnerability = hullDamageTakenMult(recipient);
        if (recipientVulnerability <= 0f || !canTakeDamage(recipient, engine)
                || tooBigToShare(wholeDamage * recipientVulnerability, recipient.getMaxHitpoints())) {
            return sharedDamage;
        }
        damageHull(recipient, sharedDamage * recipientVulnerability, attacker, engine);
        return 0f;
    }

    private static void damageHull(ShipAPI ship, float hullDamage, ShipAPI attacker, CombatEngineAPI engine) {
        if (ship.getHitpoints() > hullDamage) {
            ship.setHitpoints(ship.getHitpoints() - hullDamage);
            return;
        }
        if (engine == null) {
            return;
        }
        ship.setHitpoints(Math.min(ship.getHitpoints(), KILLING_BLOW_HULL_LEFT));
        boolean wasApplying = applyingSharedDamage;
        applyingSharedDamage = true;
        try {
            engine.applyDamage(ship, new Vector2f(ship.getLocation()), killingBlowDamage(ship), DamageType.ENERGY, 0f, true, false, attacker);
        } finally {
            applyingSharedDamage = wasApplying;
        }
    }

    static float killingBlowDamage(ShipAPI ship) {
        ArmorGridAPI armor = ship.getArmorGrid();
        MutableShipStatsAPI stats = ship.getMutableStats();
        float armorRating = armor == null ? 0f : armor.getArmorRating() / resistanceOnly(stats.getArmorDamageTakenMult().getModifiedValue());
        float damageTakenMult = resistanceOnly(stats.getHullDamageTakenMult().getModifiedValue())
                * resistanceOnly(stats.getEnergyDamageTakenMult().getModifiedValue())
                * resistanceOnly(stats.getProjectileDamageTakenMult().getModifiedValue());
        return KILLING_BLOW_MULTIPLE * (Math.max(0f, ship.getHitpoints()) + armorRating) / damageTakenMult;
    }

    private static float resistanceOnly(float damageTakenMult) {
        return damageTakenMult > 0f && damageTakenMult < 1f ? damageTakenMult : 1f;
    }
}
