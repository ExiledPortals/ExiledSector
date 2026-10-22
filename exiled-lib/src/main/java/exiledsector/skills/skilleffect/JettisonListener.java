package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static exiledsector.skills.skilleffect.ScaledBonus.percent;
import static exiledsector.skills.skilleffect.StatTarget.liveStat;

final class JettisonListener extends ShipCombatListener implements AdvanceableListener {

    static final String TOP_SPEED_PERCENT_PER_MODULE_KEY = "exiledSector_jettisonTopSpeedPercentPerModule";
    static final String MANEUVERABILITY_PERCENT_PER_MODULE_KEY = "exiledSector_jettisonManeuverabilityPercentPerModule";
    private static final String MOD_ID_PREFIX = "exiledSector_jettison_";
    private static final float CHECK_SECONDS = 0.25f;
    private static final ScaledBonus JETTISON_BONUS = new ScaledBonus(
            percent(liveStat(MutableShipStatsAPI::getMaxSpeed), TOP_SPEED_PERCENT_PER_MODULE_KEY),
            percent(Maneuverability.target(), MANEUVERABILITY_PERCENT_PER_MODULE_KEY));

    private final Set<ShipAPI> trackedModules = Collections.newSetFromMap(new IdentityHashMap<>());
    private final IntervalUtil checkInterval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private boolean modulesCounted;
    private int lostBeforeDeployment;
    private int appliedCount;

    JettisonListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        checkInterval.advance(amount);
        if (!checkInterval.intervalElapsed()) {
            return;
        }
        if (!modulesCounted) {
            modulesCounted = true;
            track(ownerShip.getChildModulesCopy());
            lostBeforeDeployment = Math.max(0, ModuleBoost.expectedModuleCount(ownerShip) - trackedModules.size());
        }
        int destroyed = lostBeforeDeployment + destroyedCount(trackedModules);
        if (destroyed != appliedCount) {
            appliedCount = destroyed;
            JETTISON_BONUS.apply(ownerShip.getMutableStats(), modId, destroyed);
        }
    }

    private void track(List<ShipAPI> modules) {
        if (modules == null) {
            return;
        }
        for (ShipAPI module : modules) {
            if (CombatQueries.isAliveNotHulk(module)) {
                trackedModules.add(module);
            }
        }
    }

    static int destroyedCount(Collection<ShipAPI> modules) {
        int destroyed = 0;
        for (ShipAPI module : modules) {
            if (!CombatQueries.isAliveNotHulk(module) || module.getHitpoints() <= 0f) {
                destroyed++;
            }
        }
        return destroyed;
    }
}
