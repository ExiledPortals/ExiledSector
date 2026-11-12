package exiledsector.charts.officer.turn;

import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTerritoryPlacer;

import java.util.Collection;

public interface TurnTerrain {

    Collection<AtlasCell> lanes(AtlasCell cell);

    boolean canClaim(AtlasCell cell);

    static TurnTerrain of(AtlasGenerator generator, AtlasState state, AtlasCell openRunCell) {
        return new TurnTerrain() {
            @Override
            public Collection<AtlasCell> lanes(AtlasCell cell) {
                return generator.lanes(cell);
            }

            @Override
            public boolean canClaim(AtlasCell cell) {
                return AtlasTerritoryPlacer.canHoldTerritory(generator, state, cell, openRunCell);
            }
        };
    }
}
