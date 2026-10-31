package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.charts.loot.DropType;

import java.util.List;
import java.util.Random;

public final class ExcavationRules {

    public static final String LOOT_SITE = "exiledSector_chart_excavation";
    public static final String LOOT_SCOPE = "excavation";
    public static final int MAX_SHIP_SIZE = 2;
    static final int MIN_SITES = 1;
    static final int MAX_SITES = 3;
    static final int MIN_PLANETS = 3;
    static final float OPEN_SHAFT_CHANCE = 0.5f;
    static final float DERELICT_CHANCE = 0.3f;
    static final float FLEET_SIZE_SHARE = 0.5f;
    static final float LOOT_STEPS = 1f;
    static final float LOOT_STEPS_PER_TIER = 0.25f;
    static final float ITEM_MULT = 1f;
    static final float LEAN_PERCENT = 200f;
    static final float FULL_LOOT_EFFECTIVENESS = 0.75f;
    static final float MIN_RAID_SHARE = 0.25f;
    static final List<DropType> LEANS = List.of(DropType.WEAPON, DropType.BLUEPRINT, DropType.AI_CORE, DropType.RESOURCE, DropType.CURRENCY,
            DropType.FEEDSTOCK, DropType.SUBROUTINE, DropType.STAR_CHART);

    private ExcavationRules() {
    }

    public static int siteCount(Random random, int extraSites) {
        return MIN_SITES + random.nextInt(MAX_SITES - MIN_SITES + 1) + Math.max(0, extraSites);
    }

    public static ExcavationSite.Access access(Random random) {
        return random.nextFloat() < OPEN_SHAFT_CHANCE ? ExcavationSite.Access.OPEN : ExcavationSite.Access.COLLAPSED;
    }

    public static DropType lean(SectorAPI sector, Random random) {
        List<DropType> droppable = LEANS.stream().filter(type -> type.canDrop(sector)).toList();
        List<DropType> choices = droppable.isEmpty() ? List.of(DropType.RESOURCE) : droppable;
        return choices.get(random.nextInt(choices.size()));
    }

    public static float raidShare(float raidEffectiveness) {
        float share = raidEffectiveness / FULL_LOOT_EFFECTIVENESS;
        return Math.max(MIN_RAID_SHARE, Math.min(1f, share));
    }

    public static boolean fitsShaft(HullSize size) {
        return size == HullSize.FRIGATE || size == HullSize.DESTROYER;
    }

    public static boolean holdsAbove(HullSize size) {
        return size == HullSize.CRUISER || size == HullSize.CAPITAL_SHIP;
    }
}
