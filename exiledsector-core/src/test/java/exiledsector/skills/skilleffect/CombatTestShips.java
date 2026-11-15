package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class CombatTestShips {

    private CombatTestShips() {
    }

    public static ShipBuilder ship() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(stats.getDynamic()).thenReturn(mock(DynamicStatsAPI.class));
        return new ShipBuilder(stats);
    }

    public static ShipBuilder ship(MutableShipStatsAPI stats) {
        return new ShipBuilder(stats);
    }

    public static ShipAPI withDynamic(String key, float value) {
        return ship().dynamic(key, value).build();
    }

    public static final class ShipBuilder {

        private final ShipAPI ship = mock(ShipAPI.class);
        private final MutableShipStatsAPI stats;

        private ShipBuilder(MutableShipStatsAPI stats) {
            this.stats = stats;
            when(ship.getMutableStats()).thenReturn(stats);
        }

        public ShipBuilder dynamic(String key, float value) {
            when(dynamicStats().getValue(key, 0f)).thenReturn(value);
            return this;
        }

        public ShipBuilder dynamic(Map<String, Float> valuesByKey) {
            valuesByKey.forEach(this::dynamic);
            return this;
        }

        public ShipBuilder owner(int owner) {
            when(ship.getOwner()).thenReturn(owner);
            return this;
        }

        public ShipBuilder alive() {
            when(ship.isAlive()).thenReturn(true);
            return this;
        }

        public ShipBuilder at(Vector2f location) {
            when(ship.getLocation()).thenReturn(location);
            return this;
        }

        public ShipBuilder hullSize(HullSize hullSize) {
            when(ship.getHullSize()).thenReturn(hullSize);
            return this;
        }

        public ShipBuilder id(String id) {
            when(ship.getId()).thenReturn(id);
            return this;
        }

        public MutableShipStatsAPI stats() {
            return stats;
        }

        public DynamicStatsAPI dynamicStats() {
            return stats.getDynamic();
        }

        public ShipAPI build() {
            return ship;
        }
    }
}
