package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.shipsystem.FragmentSwarms;

import java.util.List;

final class HiveReplacementListener extends ShipCombatListener implements AdvanceableListener {

    static final String RESTORED_PERCENT_KEY = "exiledLib_hiveReplacementRestoredPercent";
    static final int FRAGMENTS_PER_REPLACEMENT = 5;
    private static final float RATE_DROP_EPSILON = 0.0001f;

    private List<FighterLaunchBayAPI> launchBays;
    private float[] lastRates;

    HiveReplacementListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        if (launchBays == null) {
            launchBays = ownerShip.getLaunchBaysCopy();
            lastRates = new float[launchBays.size()];
            for (int i = 0; i < lastRates.length; i++) {
                lastRates[i] = launchBays.get(i).getCurrRate();
            }
            return;
        }
        for (int i = 0; i < lastRates.length; i++) {
            FighterLaunchBayAPI bay = launchBays.get(i);
            float rate = bay.getCurrRate();
            float drop = lastRates[i] - rate;
            if (drop > RATE_DROP_EPSILON && ownerIsAliveNotHulk()
                    && FragmentSwarms.spend(FragmentSwarms.swarmOf(ownerShip), FRAGMENTS_PER_REPLACEMENT)) {
                rate = restoredRate(rate, drop, magnitude(RESTORED_PERCENT_KEY));
                bay.setCurrRate(rate);
            }
            lastRates[i] = rate;
        }
    }

    static float restoredRate(float droppedRate, float drop, float restoredPercent) {
        return droppedRate + drop * Math.max(0f, Math.min(restoredPercent, 100f)) / 100f;
    }
}
