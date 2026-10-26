package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.effects.ShipEffectSource;

public final class ChartFleetConditionSource implements ShipEffectSource {

    public static final ChartFleetConditionSource INSTANCE = new ChartFleetConditionSource();

    private ChartFleetConditionSource() {
    }

    @Override
    public void applyToShip(MutableShipStatsAPI stats) {
        ChartFleetConditions.applyToShip(stats);
    }

    @Override
    public void applyToFighter(ShipAPI fighter, ShipAPI ship) {
        ChartFleetConditions.applyToFighter(fighter, ship);
    }
}
