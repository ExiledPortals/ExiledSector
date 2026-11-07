package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.skilleffect.ExpiringStacks;
import exiledsector.skills.skilleffect.ScaledBonus;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;
import exiledsector.skills.skilleffect.StatMode;

import static exiledsector.skills.skilleffect.ScaledBonus.fixed;
import static exiledsector.skills.skilleffect.StatTarget.scaled;

public final class PirateFrenzy extends ShipCombatListener implements AdvanceableListener {

    public static final int MAX_STACKS = 5;
    public static final float STACK_SECONDS = 10f;
    static final float RATE_OF_FIRE_PERCENT_PER_STACK = 5f;
    static final float FLUX_COST_REDUCTION_PERCENT_PER_STACK = 5f;
    private static final String MOD_ID_PREFIX = "exiledSector_pirateFrenzy_";
    private static final ScaledBonus FRENZY_BONUS = new ScaledBonus(
            fixed(WeaponStatFamily.RATE_OF_FIRE.target(WeaponScope.ALL), StatMode.PERCENT, RATE_OF_FIRE_PERCENT_PER_STACK),
            fixed(scaled(WeaponStatFamily.FLUX_COST.target(WeaponScope.ALL), -1f), StatMode.PERCENT, FLUX_COST_REDUCTION_PERCENT_PER_STACK));

    private final ExpiringStacks<Void> frenzyStacks = new ExpiringStacks<>(MAX_STACKS, STACK_SECONDS);
    private final ScaledBonus.Tracker frenzyBonus = FRENZY_BONUS.tracker();
    private float frenzyClockSeconds;

    PirateFrenzy(ShipAPI ownerShip) {
        super(ownerShip, MOD_ID_PREFIX);
    }

    public static PirateFrenzy of(ShipAPI ship) {
        return SkillEffectSupport.ensureListener(ship, PirateFrenzy.class, PirateFrenzy::new);
    }

    public static StyledText describeStackBonuses() {
        return Translation.msg("pirateFrenzy.rateOfFire").arg("value", RATE_OF_FIRE_PERCENT_PER_STACK).styled()
                .append("\n")
                .append(Translation.msg("pirateFrenzy.fluxCost").arg("value", FLUX_COST_REDUCTION_PERCENT_PER_STACK).styled());
    }

    public int stacks() {
        return frenzyStacks.size();
    }

    public void gain() {
        if (!ownerIsAliveNotHulk()) {
            return;
        }
        frenzyStacks.add(frenzyClockSeconds);
        applyIfChanged();
    }

    @Override
    public void advance(float amount) {
        if (!ownerIsAliveNotHulk()) {
            frenzyStacks.clear();
        } else {
            frenzyClockSeconds += amount;
        }
        frenzyStacks.dropExpired(frenzyClockSeconds);
        applyIfChanged();
    }

    private void applyIfChanged() {
        frenzyBonus.apply(ownerShip.getMutableStats(), modId, frenzyStacks.size());
    }
}
