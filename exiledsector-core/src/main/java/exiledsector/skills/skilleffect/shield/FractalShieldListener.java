package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.shipsystem.FragmentCover;
import exiledsector.skills.skilleffect.shipsystem.FragmentSwarms;

final class FractalShieldListener extends ShipCombatListener implements DamageListener, AdvanceableListener {

    static final String ABSORBED_PERCENT_KEY = "exiledLib_fractalShieldAbsorbedPercent";
    static final float DAMAGE_PER_FRAGMENT = 30f;
    static final float MAX_ABSORBED_PERCENT = 90f;
    private static final String MOD_ID_PREFIX = "exiledLib_fractalShield_";
    private static final float SWARM_CHECK_SECONDS = 0.5f;

    private final IntervalUtil swarmCheck = new IntervalUtil(SWARM_CHECK_SECONDS, SWARM_CHECK_SECONDS);
    private final FragmentCover absorbedDamageCover = new FragmentCover(DAMAGE_PER_FRAGMENT);
    private float absorbedPercent;

    FractalShieldListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        swarmCheck.advance(amount);
        if (swarmCheck.intervalElapsed()) {
            boolean covering = ownerIsAliveNotHulk() && FragmentSwarms.hasFragments(FragmentSwarms.swarmOf(ownerShip), 1);
            applyReduction(covering ? Math.min(MAX_ABSORBED_PERCENT, magnitude(ABSORBED_PERCENT_KEY)) : 0f);
        }
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (target != ownerShip || absorbedPercent <= 0f || result.getDamageToShields() <= 0f) {
            return;
        }
        if (!absorbedDamageCover.charge(FragmentSwarms.swarmOf(ownerShip), result.getDamageToShields(), absorbedPercent)) {
            applyReduction(0f);
        }
    }

    private void applyReduction(float percent) {
        if (percent == absorbedPercent) {
            return;
        }
        absorbedPercent = percent;
        MutableStat shieldDamageTaken = ownerShip.getMutableStats().getShieldDamageTakenMult();
        if (percent > 0f) {
            shieldDamageTaken.modifyPercent(modId, -percent);
        } else {
            shieldDamageTaken.unmodify(modId);
        }
    }
}
