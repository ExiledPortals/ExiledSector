package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import exiledsector.charts.loot.ChartFleetLoot;
import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;

public final class ExcavationDeliveries {

    static final String CACHE_KEY = "$exiledSector_excavationDeliveredCache";
    static final String RAID_DIFFICULTY_KEY = "$exiledSector_excavationDeliveryRaidPercent";
    static final String LOOT_KEY = "$exiledSector_excavationDeliveryLootPercent";
    static final float PERCENT = 100f;

    private ExcavationDeliveries() {
    }

    public static List<String> standingPlanetIds(ChartRun run) {
        return run == null ? List.of() : ExcavationStates.standing(run).stream().map(ExcavationSite::planetId).toList();
    }

    public static boolean accepts(ChartRun run, SectorEntityToken planet) {
        ExcavationSite site = standingAt(run, planet);
        return site != null && (site.access() == ExcavationSite.Access.COLLAPSED || ExcavationFleets.fleetFor(planet, run, site) != null);
    }

    public static void joinGuardians(ChartRun run, SectorEntityToken planet, CampaignFleetAPI convoyFleet) {
        CampaignFleetAPI guardians = guardiansAt(run, planet);
        if (guardians != null && !joinShaftShips(convoyFleet, guardians).isEmpty()) {
            ChartFleetLoot.recordStart(guardians);
        }
    }

    public static void deliverCache(ChartRun run, SectorEntityToken planet, CargoAPI cache, float raidDifficultyPercent, float lootPercent) {
        ExcavationSite site = standingAt(run, planet);
        if (site == null) {
            return;
        }
        boolean hasCache = cache != null && !cache.isEmpty();
        if (site.access() == ExcavationSite.Access.OPEN) {
            CampaignFleetAPI guardians = guardiansAt(run, planet);
            if (guardians != null) {
                if (hasCache) {
                    BaseSalvageSpecial.addExtraSalvage(guardians, cache);
                }
                raise(guardians.getMemoryWithoutUpdate(), LOOT_KEY, lootPercent);
            }
            return;
        }
        MemoryAPI memory = planet.getMemoryWithoutUpdate();
        if (hasCache) {
            CargoAPI stored = memory.get(CACHE_KEY) instanceof CargoAPI cargo ? cargo : null;
            if (stored == null) {
                memory.set(CACHE_KEY, cache);
            } else {
                stored.addAll(cache);
            }
        }
        raise(memory, RAID_DIFFICULTY_KEY, raidDifficultyPercent);
        raise(memory, LOOT_KEY, lootPercent);
    }

    private static ExcavationSite standingAt(ChartRun run, SectorEntityToken planet) {
        return run == null || planet == null ? null : ExcavationStates.standingAt(run, planet.getId());
    }

    private static CampaignFleetAPI guardiansAt(ChartRun run, SectorEntityToken planet) {
        ExcavationSite site = standingAt(run, planet);
        return site == null || site.access() != ExcavationSite.Access.OPEN ? null : ExcavationFleets.fleetFor(planet, run, site);
    }

    static List<FleetMemberAPI> joinShaftShips(CampaignFleetAPI convoyFleet, CampaignFleetAPI guardians) {
        List<FleetMemberAPI> joined = new ArrayList<>();
        if (convoyFleet == null || convoyFleet.getFleetData() == null) {
            return joined;
        }
        FleetMemberAPI convoyFlagship = convoyFleet.getFlagship();
        for (FleetMemberAPI member : convoyFleet.getFleetData().getMembersListCopy()) {
            if (member != convoyFlagship && !member.isCivilian() && member.getHullSpec() != null
                    && ExcavationRules.fitsExcavation(member.getHullSpec().getHullSize())) {
                convoyFleet.getFleetData().removeFleetMember(member);
                guardians.getFleetData().addFleetMember(member);
                joined.add(member);
            }
        }
        if (!joined.isEmpty()) {
            guardians.getFleetData().sort();
        }
        return joined;
    }

    private static void raise(MemoryAPI memory, String key, float percent) {
        if (percent > percent(memory, key)) {
            memory.set(key, percent);
        }
    }

    private static float percent(MemoryAPI memory, String key) {
        return memory != null && memory.get(key) instanceof Float stored ? stored : 0f;
    }

    static float lootPercent(SectorEntityToken entity) {
        return entity == null ? 0f : percent(entity.getMemoryWithoutUpdate(), LOOT_KEY);
    }

    public static float raidDifficultyMult(SectorEntityToken planet) {
        return 1f + Math.max(0f, planet == null ? 0f : percent(planet.getMemoryWithoutUpdate(), RAID_DIFFICULTY_KEY)) / PERCENT;
    }

    static CargoAPI takeCache(SectorEntityToken planet) {
        MemoryAPI memory = planet == null ? null : planet.getMemoryWithoutUpdate();
        CargoAPI cache = memory != null && memory.get(CACHE_KEY) instanceof CargoAPI stored ? stored : null;
        if (cache != null) {
            memory.unset(CACHE_KEY);
        }
        return cache;
    }
}
