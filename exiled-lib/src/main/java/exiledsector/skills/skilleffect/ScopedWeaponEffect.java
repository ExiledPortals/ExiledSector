package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.i18n.StyledText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScopedWeaponEffect implements SkillEffect {

    private static final List<ScopedWeaponEffect> ALL_EFFECTS = generate();

    private final WeaponScope scope;
    private final WeaponStatFamily family;
    private final StatMode mode;
    private final String name;

    private ScopedWeaponEffect(WeaponScope scope, WeaponStatFamily family, StatMode mode) {
        this.scope = scope;
        this.family = family;
        this.mode = mode;
        this.name = scope.namePrefix() + "WEAPON_" + family.name() + "_" + mode.name();
    }

    private static List<ScopedWeaponEffect> generate() {
        List<ScopedWeaponEffect> effects = new ArrayList<>();
        for (WeaponStatFamily family : WeaponStatFamily.values()) {
            for (WeaponScope scope : WeaponScope.values()) {
                for (StatMode mode : StatMode.values()) {
                    if (family.supports(scope, mode)) {
                        effects.add(new ScopedWeaponEffect(scope, family, mode));
                    }
                }
            }
        }
        return Collections.unmodifiableList(effects);
    }

    public static List<ScopedWeaponEffect> all() {
        return ALL_EFFECTS;
    }

    public static ScopedWeaponEffect find(WeaponStatFamily family, WeaponScope scope, StatMode mode) {
        for (ScopedWeaponEffect effect : ALL_EFFECTS) {
            if (effect.family == family && effect.scope == scope && effect.mode == mode) {
                return effect;
            }
        }
        return null;
    }

    public WeaponScope scope() {
        return scope;
    }

    public WeaponStatFamily family() {
        return family;
    }

    public StatMode mode() {
        return mode;
    }

    @Override
    public StatMode statMode() {
        return mode;
    }

    void applyTo(MutableShipStatsAPI stats, String modId, float magnitude) {
        family.target(scope).apply(stats, modId, mode, magnitude);
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
        applyTo(stats, modId, magnitude);
    }

    @Override
    public void applyAfterShipCreation(ShipAPI ship, String modId, float magnitude) {
        family.target(scope).applyAfterShipCreation(ship);
    }

    @Override
    public boolean supportsTemporaryGating() {
        return family.target(scope).supportsTemporaryGating();
    }

    @Override
    public boolean lowerIsBetter() {
        return family.lowerIsBetter();
    }

    @Override
    public StyledText description(float magnitude) {
        return family.description(scope, mode, magnitude);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
