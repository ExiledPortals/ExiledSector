package exiledsector.charts.anomaly.excavation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import exiledsector.charts.loot.ChartFleetLedger;
import exiledsector.charts.run.ChartRun;

import java.util.ArrayList;
import java.util.List;

public final class ExcavationDeliveries {

    static final String CACHE_KEY = "$exiledSector_excavationDeliveredCache";
    static final String RAID_DIFFICULTY_KEY = "$exiledSector_excavationDeliveryRaidPercent";
    static final String LOOT_KEY = "$exiledSector_excavationDeliveryLootPercent";
    static final String WAITING_SHIPS_KEY = "$exiledSector_excavationDeliveredShips";
    static final String WAITING_CACHE_KEY = "$exiledSector_excavationDeliveredGuardianCache";
    static final float PERCENT = 100f;

    private ExcavationDeliveries() {
    }

    public static List<String> standingPlanetIds(ChartRun run) {
        return run == null ? List.of() : ExcavationStates.standing(run).stream().map(ExcavationSite::planetId).toList();
    }

    public static boolean accepts(ChartRun run, SectorEntityToken planet) {
        ExcavationSite site = standingAt(run, planet);
        return site != null && (site.hasCollapsedShaft() || site.hasOpenShaft());
    }

    public static void joinGuardians(ChartRun run, SectorEntityToken planet, CampaignFleetAPI convoyFleet) {
        ExcavationSite site = standingAt(run, planet);
        if (site == null || !site.hasOpenShaft()) {
            return;
        }
        List<FleetMemberAPI> shaftShips = takeShaftShips(convoyFleet, ExcavationRules.maxShipSize(ExcavationTuning.ofRun(run)));
        if (shaftShips.isEmpty()) {
            return;
        }
        CampaignFleetAPI guardians = heldGuardians(planet);
        if (guardians != null) {
            addShips(guardians, shaftShips);
        } else {
            waitingShips(planet.getMemoryWithoutUpdate(), true).addAll(shaftShips);
        }
    }

    public static void deliverCache(ChartRun run, SectorEntityToken planet, CargoAPI cache, float raidDifficultyPercent, float lootPercent) {
        ExcavationSite site = standingAt(run, planet);
        if (site == null) {
            return;
        }
        boolean hasCache = cache != null && !cache.isEmpty();
        MemoryAPI memory = planet.getMemoryWithoutUpdate();
        raise(memory, LOOT_KEY, lootPercent);
        CampaignFleetAPI guardians = site.hasOpenShaft() ? heldGuardians(planet) : null;
        if (guardians != null) {
            if (hasCache) {
                BaseSalvageSpecial.addExtraSalvage(guardians, cache);
            }
            raise(guardians.getMemoryWithoutUpdate(), LOOT_KEY, lootPercent);
        } else if (hasCache && site.hasOpenShaft()) {
            store(memory, WAITING_CACHE_KEY, cache);
            hasCache = false;
        }
        if (!site.hasCollapsedShaft()) {
            return;
        }
        raise(memory, RAID_DIFFICULTY_KEY, raidDifficultyPercent);
        if (hasCache && guardians == null) {
            store(memory, CACHE_KEY, cache);
        }
    }

    static CampaignFleetAPI withDeliveries(SectorEntityToken planet, CampaignFleetAPI guardians, ExcavationGuardians kind) {
        if (guardians == null) {
            return null;
        }
        raise(guardians.getMemoryWithoutUpdate(), LOOT_KEY, lootPercent(planet));
        if (kind.secondCavern()) {
            return guardians;
        }
        MemoryAPI memory = planet.getMemoryWithoutUpdate();
        List<FleetMemberAPI> ships = waitingShips(memory, false);
        if (ships != null && !ships.isEmpty()) {
            addShips(guardians, ships);
        }
        if (memory.get(WAITING_CACHE_KEY) instanceof CargoAPI cache && !cache.isEmpty()) {
            BaseSalvageSpecial.addExtraSalvage(guardians, cache);
        }
        dropWaiting(planet);
        return guardians;
    }

    static void dropWaiting(SectorEntityToken planet) {
        MemoryAPI memory = planet.getMemoryWithoutUpdate();
        memory.unset(WAITING_SHIPS_KEY);
        memory.unset(WAITING_CACHE_KEY);
    }

    private static ExcavationSite standingAt(ChartRun run, SectorEntityToken planet) {
        return run == null || planet == null ? null : ExcavationStates.standingAt(run, planet.getId());
    }

    private static CampaignFleetAPI heldGuardians(SectorEntityToken planet) {
        return planet.getMemoryWithoutUpdate().get(ExcavationFleets.SITE_FLEET_KEY) instanceof CampaignFleetAPI held && !held.isEmpty() ? held : null;
    }

    static List<FleetMemberAPI> takeShaftShips(CampaignFleetAPI convoyFleet, int maxShipSize) {
        List<FleetMemberAPI> taken = new ArrayList<>();
        if (convoyFleet == null || convoyFleet.getFleetData() == null) {
            return taken;
        }
        FleetMemberAPI convoyFlagship = convoyFleet.getFlagship();
        for (FleetMemberAPI member : convoyFleet.getFleetData().getMembersListCopy()) {
            if (member != convoyFlagship && !member.isCivilian() && member.getHullSpec() != null
                    && ExcavationRules.fitsExcavation(member.getHullSpec().getHullSize(), maxShipSize)) {
                convoyFleet.getFleetData().removeFleetMember(member);
                taken.add(member);
            }
        }
        return taken;
    }

    static void addShips(CampaignFleetAPI guardians, List<FleetMemberAPI> ships) {
        for (FleetMemberAPI member : ships) {
            guardians.getFleetData().addFleetMember(member);
        }
        guardians.getFleetData().sort();
        ChartFleetLedger.recordStart(guardians);
    }

    @SuppressWarnings("unchecked") // only this class writes the waiting ships list
    private static List<FleetMemberAPI> waitingShips(MemoryAPI memory, boolean create) {
        if (memory.get(WAITING_SHIPS_KEY) instanceof List<?> stored) {
            return (List<FleetMemberAPI>) stored;
        }
        if (!create) {
            return null;
        }
        List<FleetMemberAPI> ships = new ArrayList<>();
        memory.set(WAITING_SHIPS_KEY, ships);
        return ships;
    }

    private static void store(MemoryAPI memory, String key, CargoAPI cache) {
        CargoAPI stored = memory.get(key) instanceof CargoAPI cargo ? cargo : null;
        if (stored == null) {
            memory.set(key, cache);
        } else {
            stored.addAll(cache);
        }
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
