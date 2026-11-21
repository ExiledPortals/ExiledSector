package exiledsector.skills.skilleffect.aura;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import exiledsector.persistence.CombatData;
import exiledsector.skills.skilleffect.CombatQueries;
import exiledsector.skills.skilleffect.TickTimer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

abstract class AuraField<S extends AuraField.Source> extends BaseEveryFrameCombatPlugin {

    static final float UPDATE_INTERVAL_SECONDS = 0.25f;
    static final ShipsNear ENGINE_SHIPS_NEAR = (source, radius, filter) -> CombatQueries.shipsNear(source.getLocation(), radius, filter);

    interface Source {
        ShipAPI source();
    }

    interface ShipsNear {
        List<ShipAPI> find(ShipAPI source, float radius, Predicate<ShipAPI> filter);
    }

    interface SourceStrength<S> {
        float of(S source);
    }

    final List<S> sources = new ArrayList<>();
    private final TickTimer updateTimer;

    AuraField(boolean updatesOnFirstFrame) {
        updateTimer = updatesOnFirstFrame ? TickTimer.dueOnFirstAdvance(UPDATE_INTERVAL_SECONDS) : new TickTimer(UPDATE_INTERVAL_SECONDS);
    }

    public static <S extends Source, F extends AuraField<S>> void register(S source, String customDataKey, Class<F> fieldType, Supplier<F> fieldFactory) {
        register(source, customDataKey, fieldType, fieldFactory, (engine, field) -> {
        });
    }

    public static <S extends Source, F extends AuraField<S>> void register(S source, String customDataKey, Class<F> fieldType, Supplier<F> fieldFactory,
                                                                   BiConsumer<CombatEngineAPI, F> onCreated) {
        if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        F field = CombatData.getOrCreatePlugin(engine, customDataKey, fieldType, () -> {
            F created = fieldFactory.get();
            onCreated.accept(engine, created);
            return created;
        });
        field.sources.add(source);
    }

    @Override
    public final void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        everyFrame(engine);
        if (!updateTimer.advance(amount)) {
            return;
        }
        sources.removeIf(source -> !engine.isEntityInPlay(source.source()) || !CombatQueries.isAliveNotHulk(source.source()));
        update(updateTimer.elapsed());
    }

    static <S extends Source> Map<ShipAPI, Float> strongestPerShip(List<S> sources, ShipsNear shipsNear, float range,
                                                                   SourceStrength<S> strength, BiPredicate<ShipAPI, ShipAPI> covers) {
        return strongestPerShip(sources, shipsNear, source -> range, strength, covers);
    }

    static <S extends Source> Map<ShipAPI, Float> strongestPerShip(List<S> sources, ShipsNear shipsNear, SourceStrength<S> rangeOfSource,
                                                                   SourceStrength<S> strength, BiPredicate<ShipAPI, ShipAPI> covers) {
        Map<ShipAPI, Float> strongest = new IdentityHashMap<>();
        for (S source : sources) {
            float sourceStrength = strength.of(source);
            if (sourceStrength <= 0f) {
                continue;
            }
            float range = rangeOfSource.of(source);
            ShipAPI sourceShip = source.source();
            for (ShipAPI covered : shipsNear.find(sourceShip, range, other -> covers.test(sourceShip, other)
                    && CombatQueries.withinRadius(other.getLocation(), sourceShip.getLocation(), range))) {
                strongest.merge(covered, sourceStrength, Float::max);
            }
        }
        return strongest;
    }

    static boolean isHostileShip(ShipAPI sourceShip, ShipAPI other) {
        return !other.isFighter() && CombatQueries.isAliveNotHulk(other) && CombatQueries.isHostile(sourceShip, other);
    }

    void everyFrame(CombatEngineAPI engine) {
    }

    abstract void update(float elapsedSeconds);
}
