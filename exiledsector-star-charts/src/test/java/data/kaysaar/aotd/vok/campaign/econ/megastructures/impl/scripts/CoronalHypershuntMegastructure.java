package data.kaysaar.aotd.vok.campaign.econ.megastructures.impl.scripts;

import com.fs.starfarer.api.campaign.SectorEntityToken;

import java.util.HashSet;
import java.util.Set;

public class CoronalHypershuntMegastructure {

    public static final Set<String> IN_RANGE_ENTITY_IDS = new HashSet<>();

    public static boolean isWithinRangeOfAtLeastOneHypershunt(SectorEntityToken target) {
        return IN_RANGE_ENTITY_IDS.contains(target.getId());
    }
}
