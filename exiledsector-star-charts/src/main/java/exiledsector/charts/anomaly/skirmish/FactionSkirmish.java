package exiledsector.charts.anomaly.skirmish;

import exiledsector.charts.anomaly.ChartAnomaly;
import exiledsector.charts.atlas.AtlasBorders;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasOpening;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.atlas.AtlasTraits;

public final class FactionSkirmish implements ChartAnomaly {

    public static final String ID = "faction_skirmish";
    public static final FactionSkirmish INSTANCE = new FactionSkirmish();

    private FactionSkirmish() {
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean fitsAt(AtlasTraits traits) {
        if (traits.specialId() != null || traits.infestationId() != null || !SkirmishFactions.takesSides(traits.factionId())) {
            return false;
        }
        return traits.neighbourFactionIds().stream().anyMatch(neighbour -> !neighbour.equals(traits.factionId()) && SkirmishFactions.takesSides(neighbour));
    }

    @Override
    public boolean holdsAt(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return SkirmishFactions.takesSides(defender(generator, state, cell)) && attacker(generator, state, cell) != null;
    }

    public static String defender(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasOpening.ownerOf(generator, state, cell);
    }

    public static String attacker(AtlasGenerator generator, AtlasState state, AtlasCell cell) {
        return AtlasBorders.attacker(generator, state, cell, SkirmishFactions::takesSides);
    }
}
