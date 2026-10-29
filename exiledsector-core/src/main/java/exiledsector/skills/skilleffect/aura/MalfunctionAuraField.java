package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class MalfunctionAuraField extends AuraField<MalfunctionAuraListener> {

    static final float RANGE = 1000f;
    static final String MOD_ID = "exiledLib_malfunctionAura";
    private static final String CUSTOM_DATA_KEY = "exiledLib_malfunctionAuraField";

    private final AppliedAuraValues appliedPercent = new AppliedAuraValues(AppliedAuraValues.Change.EXACT);

    MalfunctionAuraField() {
        super(true);
    }

    public static void register(MalfunctionAuraListener source) {
        AuraField.register(source, CUSTOM_DATA_KEY, MalfunctionAuraField.class, MalfunctionAuraField::new);
    }

    @Override
    void update(float elapsedSeconds) {
        appliedPercent.update(strongestPerTarget(sources, ENGINE_SHIPS_NEAR),
                target -> target.getMutableStats().getWeaponMalfunctionChance().unmodify(MOD_ID),
                (target, percent) -> target.getMutableStats().getWeaponMalfunctionChance().modifyFlat(MOD_ID, percent / 100f));
    }

    static Map<ShipAPI, Float> strongestPerTarget(List<MalfunctionAuraListener> sources, ShipsNear shipsNear) {
        Map<ShipAPI, Float> strongest = new IdentityHashMap<>();
        for (MalfunctionAuraListener source : sources) {
            float percent = source.malfunctionPercent();
            if (percent <= 0f) {
                continue;
            }
            ShipAPI sourceShip = source.source();
            for (ShipAPI target : shipsNear.find(sourceShip, RANGE, other -> !other.isFighter() && CombatQueries.isAliveNotHulk(other)
                    && CombatQueries.isHostile(sourceShip, other)
                    && CombatQueries.withinRadius(other.getLocation(), sourceShip.getLocation(), RANGE))) {
                strongest.merge(target, percent, Math::max);
            }
        }
        return strongest;
    }
}
