package exiledsector.skills.skilleffect.shield;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.StatMode;
import exiledsector.skills.skilleffect.StatTarget;
import exiledsector.skills.skilleffect.TickTimer;
import exiledsector.skills.skilleffect.shipsystem.FragmentCover;
import exiledsector.skills.skilleffect.shipsystem.FragmentSwarms;

final class FractalShieldListener extends ShipCombatListener implements DamageListener, AdvanceableListener {

    static final String ABSORBED_PERCENT_KEY = "exiledSector_fractalShieldAbsorbedPercent";
    static final float DAMAGE_PER_FRAGMENT = 30f;
    static final float MAX_ABSORBED_PERCENT = 90f;
    private static final String MOD_ID_PREFIX = "exiledSector_fractalShield_";
    private static final float SWARM_CHECK_SECONDS = 0.5f;

    private static final ScaledBonus SHIELD_DAMAGE_REDUCTION = new ScaledBonus(
            ScaledBonus.fixed(StatTarget.liveStat(MutableShipStatsAPI::getShieldDamageTakenMult), StatMode.PERCENT, -1f));

    private final TickTimer swarmCheck = new TickTimer(SWARM_CHECK_SECONDS);
    private final FragmentCover absorbedDamageCover = new FragmentCover(DAMAGE_PER_FRAGMENT);
    private final ScaledBonus.Tracker shieldDamageReduction = SHIELD_DAMAGE_REDUCTION.tracker();

    FractalShieldListener(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    @Override
    public void advance(float amount) {
        if (swarmCheck.advance(amount)) {
            boolean covering = ownerIsAliveNotHulk() && FragmentSwarms.hasFragments(FragmentSwarms.swarmOf(ownerShip), 1);
            applyReduction(covering ? Math.min(MAX_ABSORBED_PERCENT, magnitude(ABSORBED_PERCENT_KEY)) : 0f);
        }
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (target != ownerShip || shieldDamageReduction.appliedScale() <= 0f || result.getDamageToShields() <= 0f) {
            return;
        }
        if (!absorbedDamageCover.charge(FragmentSwarms.swarmOf(ownerShip), result.getDamageToShields(), shieldDamageReduction.appliedScale())) {
            applyReduction(0f);
        }
    }

    private void applyReduction(float percent) {
        shieldDamageReduction.apply(ownerShip.getMutableStats(), modId, percent);
    }
}
