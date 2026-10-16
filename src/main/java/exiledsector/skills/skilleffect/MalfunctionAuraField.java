package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class MalfunctionAuraField extends BaseEveryFrameCombatPlugin {

    static final float RANGE = 1000f;
    static final float UPDATE_INTERVAL_SECONDS = 0.25f;
    static final String MOD_ID = "exiledSector_malfunctionAura";
    private static final String CUSTOM_DATA_KEY = "exiledSector_malfunctionAuraField";

    private final List<MalfunctionAuraListener> sources = new ArrayList<>();
    private final Map<ShipAPI, Float> appliedPercent = new IdentityHashMap<>();
    private float secondsSinceUpdate = UPDATE_INTERVAL_SECONDS;

    static void register(MalfunctionAuraListener source) {
        if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        MalfunctionAuraField field = (MalfunctionAuraField) engine.getCustomData().get(CUSTOM_DATA_KEY);
        if (field == null) {
            field = new MalfunctionAuraField();
            engine.getCustomData().put(CUSTOM_DATA_KEY, field);
            engine.addPlugin(field);
        }
        field.sources.add(source);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        secondsSinceUpdate += amount;
        if (secondsSinceUpdate < UPDATE_INTERVAL_SECONDS) {
            return;
        }
        secondsSinceUpdate = 0f;
        sources.removeIf(source -> !engine.isEntityInPlay(source.source()) || !CombatQueries.isAliveNotHulk(source.source()));
        applyMalfunctions(strongestPerTarget(sources, (source, radius, filter) -> CombatQueries.shipsNear(source.getLocation(), radius, filter)));
    }

    static Map<ShipAPI, Float> strongestPerTarget(List<MalfunctionAuraListener> sources, GravityDragField.ShipsNear shipsNear) {
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

    private void applyMalfunctions(Map<ShipAPI, Float> malfunctions) {
        for (ShipAPI previouslyAffected : new ArrayList<>(appliedPercent.keySet())) {
            if (!malfunctions.containsKey(previouslyAffected)) {
                previouslyAffected.getMutableStats().getWeaponMalfunctionChance().unmodify(MOD_ID);
                appliedPercent.remove(previouslyAffected);
            }
        }
        malfunctions.forEach((target, percent) -> {
            if (!percent.equals(appliedPercent.get(target))) {
                target.getMutableStats().getWeaponMalfunctionChance().modifyFlat(MOD_ID, percent / 100f);
                appliedPercent.put(target, percent);
            }
        });
    }
}
