package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.effects.ResolvedTree;
import exiledsector.effects.ShipEffectSource;

public final class ChartPlayerModifierSource implements ShipEffectSource {

    public static final ChartPlayerModifierSource INSTANCE = new ChartPlayerModifierSource();

    private ChartPlayerModifierSource() {
    }

    @Override
    public void applyToShip(MutableShipStatsAPI stats) {
        ChartPlayerModifiers.applyToShip(stats);
    }

    @Override
    public void applyToFighter(ShipAPI fighter, ShipAPI ship) {
        ChartPlayerModifiers.applyToFighter(fighter, ship);
    }

    @Override
    public void afterShipCreation(ShipAPI ship, ResolvedTree resolvedTree) {
        ChartPlayerModifiers.afterShipCreation(ship);
    }
}
