package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
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

    final List<S> sources = new ArrayList<>();
    private float secondsSinceUpdate;

    AuraField(boolean updatesOnFirstFrame) {
        secondsSinceUpdate = updatesOnFirstFrame ? UPDATE_INTERVAL_SECONDS : 0f;
    }

    static <S extends Source, F extends AuraField<S>> void register(S source, String customDataKey, Class<F> fieldType, Supplier<F> fieldFactory) {
        register(source, customDataKey, fieldType, fieldFactory, (engine, field) -> {
        });
    }

    static <S extends Source, F extends AuraField<S>> void register(S source, String customDataKey, Class<F> fieldType, Supplier<F> fieldFactory,
                                                                   BiConsumer<CombatEngineAPI, F> onCreated) {
        if (Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT) {
            return;
        }
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) {
            return;
        }
        Object stored = engine.getCustomData().get(customDataKey);
        F field = fieldType.isInstance(stored) ? fieldType.cast(stored) : null;
        if (field == null) {
            field = fieldFactory.get();
            engine.getCustomData().put(customDataKey, field);
            engine.addPlugin(field);
            onCreated.accept(engine, field);
        }
        field.sources.add(source);
    }

    @Override
    public final void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }
        everyFrame(engine);
        secondsSinceUpdate += amount;
        if (secondsSinceUpdate < UPDATE_INTERVAL_SECONDS) {
            return;
        }
        float elapsedSeconds = secondsSinceUpdate;
        secondsSinceUpdate = 0f;
        sources.removeIf(source -> !engine.isEntityInPlay(source.source()) || !CombatQueries.isAliveNotHulk(source.source()));
        update(elapsedSeconds);
    }

    void everyFrame(CombatEngineAPI engine) {
    }

    abstract void update(float elapsedSeconds);
}
