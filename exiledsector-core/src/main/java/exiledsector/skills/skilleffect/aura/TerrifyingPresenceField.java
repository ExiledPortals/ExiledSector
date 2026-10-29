package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.CombatQueries;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class TerrifyingPresenceField extends AuraField<TerrifyingPresenceListener> {

    static final float RANGE = 1000f;
    static final String MOD_ID = "exiledLib_terrifyingPresence";
    private static final String CUSTOM_DATA_KEY = "exiledLib_terrifyingPresenceField";

    private final AppliedAuraValues appliedPenalty = new AppliedAuraValues(AppliedAuraValues.Change.EXACT);

    TerrifyingPresenceField() {
        super(true);
    }

    public static void register(TerrifyingPresenceListener source) {
        AuraField.register(source, CUSTOM_DATA_KEY, TerrifyingPresenceField.class, TerrifyingPresenceField::new);
    }

    @Override
    void update(float elapsedSeconds) {
        appliedPenalty.update(penaltyPerTarget(sources, ENGINE_SHIPS_NEAR),
                target -> target.getMutableStats().getAutofireAimAccuracy().unmodify(MOD_ID),
                (target, penalty) -> target.getMutableStats().getAutofireAimAccuracy().modifyFlat(MOD_ID, -penalty));
    }

    static Map<ShipAPI, Float> penaltyPerTarget(List<TerrifyingPresenceListener> sources, ShipsNear shipsNear) {
        Map<ShipAPI, Float> penalties = new IdentityHashMap<>();
        for (TerrifyingPresenceListener source : sources) {
            float penalty = source.accuracyPenalty();
            if (penalty <= 0f) {
                continue;
            }
            ShipAPI sourceShip = source.source();
            for (ShipAPI target : shipsNear.find(sourceShip, RANGE, other -> CombatQueries.isAliveNotHulk(other)
                    && CombatQueries.isHostile(sourceShip, other)
                    && CombatQueries.withinRadius(other.getLocation(), sourceShip.getLocation(), RANGE))) {
                penalties.merge(target, penalty, Float::sum);
            }
        }
        return penalties;
    }
}
