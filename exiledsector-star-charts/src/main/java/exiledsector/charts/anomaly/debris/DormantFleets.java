package exiledsector.charts.anomaly.debris;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickParams;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import exiledlib.i18n.Translation;
import exiledsector.charts.ChartIds;
import exiledsector.charts.content.ChartContentGenerator;
import exiledsector.charts.content.ChartFaction;
import exiledsector.charts.content.ChartFactions;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.generation.ChartTier;
import exiledsector.charts.generation.ChartTiers;
import exiledsector.charts.loot.ChartFleetLoot;
import exiledsector.charts.loot.ChartVanillaLoot;
import exiledsector.items.WeightedPick;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class DormantFleets {

    public static final String FACTION_ID = "exiledSector_dormant";
    public static final String FLEET_TAG = ChartIds.PREFIX + "dormant_fleet";
    public static final String DORMANT_KEY = "$exiledSector_debrisDormant";
    public static final String CORRUPTED_KEY = "$exiledSector_debrisCorrupted";
    public static final String CURRENCY_DROPPED_KEY = "$exiledSector_debrisCurrencyDropped";
    static final String BUILD_SEED_KEY = "$exiledSector_debrisBuildSeed";
    static final String HIDDEN_MOD_ID = AnomalousDebris.TERRAIN_ID + "_hidden";
    static final List<Map.Entry<String, Float>> ROLES = List.of(Map.entry(ShipRoles.COMBAT_SMALL, 4f), Map.entry(ShipRoles.COMBAT_MEDIUM, 3f),
            Map.entry(ShipRoles.COMBAT_LARGE, 2f), Map.entry(ShipRoles.COMBAT_CAPITAL, 1f));
    static final List<String> REMOVED_ABILITIES = List.of(Abilities.GO_DARK, Abilities.TRANSVERSE_JUMP, Abilities.EMERGENCY_BURN,
            Abilities.SENSOR_BURST);
    static final int MOST_SHIPS = 30;
    static final int MOST_FAILED_PICKS = 6;
    static final int SEED_RADIX = Character.MAX_RADIX;

    record HullFaction(FactionAPI faction, float weight) {
    }

    private DormantFleets() {
    }

    public static CampaignFleetAPI create(String runId, int tier, ChartGenContext context, long seed) {
        SectorAPI sector = Global.getSector();
        dressFaction(sector.getFaction(FACTION_ID));
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(FACTION_ID, Translation.gameText("chart.debris.fleet.name"), true);
        fleet.setInflater(null);
        populate(fleet, hullFactions(sector), tier, context, seed, false);
        if (fleet.isEmpty()) {
            return null;
        }
        prepare(fleet, runId, tier, seed, new Random(seed));
        ChartVanillaLoot.addToFleet(fleet, false, context == null ? ChartGenContext.none() : context, tier);
        return fleet;
    }

    static void dressFaction(FactionAPI faction) {
        if (faction == null) {
            throw new IllegalStateException("the " + FACTION_ID + " faction is not loaded");
        }
        faction.setDisplayNameOverride(Translation.gameText("chart.debris.faction.name"));
        faction.setDisplayNameWithArticleOverride(Translation.gameText("chart.debris.faction.article"));
        faction.setDisplayIsOrAreOverride(Translation.gameText("chart.debris.faction.isOrAre"));
        faction.setRelationship(Factions.PLAYER, RepLevel.NEUTRAL);
    }

    static List<HullFaction> hullFactions(SectorAPI sector) {
        List<HullFaction> factions = new ArrayList<>();
        for (ChartFaction chartFaction : ChartFactions.all()) {
            FactionAPI faction = chartFaction.weight() > 0f ? sector.getFaction(chartFaction.factionId()) : null;
            if (faction != null) {
                factions.add(new HullFaction(faction, chartFaction.weight()));
            }
        }
        if (factions.isEmpty()) {
            factions.add(new HullFaction(sector.getFaction(Factions.INDEPENDENT), 1f));
        }
        return factions;
    }

    static void populate(CampaignFleetAPI fleet, List<HullFaction> factions, int tier, ChartGenContext context, long seed, boolean corrupted) {
        Random random = new Random(seed);
        ChartTier adjusted = (context == null ? ChartGenContext.none() : context).adjust(ChartTiers.forTier(tier));
        float points = DebrisRules.fleetPoints(adjusted, corrupted);
        float averageDMods = DefaultFleetInflater.getAverageDmodsForQuality(DebrisRules.quality(adjusted, corrupted));
        ShipPickParams pickParams = new ShipPickParams(ShipPickMode.ALL);
        pickParams.blockFallback = true;
        pickParams.maxFP = Integer.MAX_VALUE;
        float spent = 0f;
        int failedPicks = 0;
        for (int picks = 0; picks < MOST_SHIPS && spent < points && failedPicks < MOST_FAILED_PICKS; picks++) {
            FactionAPI faction = WeightedPick.pick(factions, HullFaction::weight, random).faction();
            String role = WeightedPick.pick(ROLES, Map.Entry::getValue, random).getKey();
            FleetMemberAPI member = addShip(fleet, faction, role, pickParams, random);
            if (member == null) {
                failedPicks++;
                continue;
            }
            spent += member.getFleetPointCost();
            crew(member, tier, corrupted, averageDMods, random);
        }
        fleet.getFleetData().sort();
        fleet.getFleetData().ensureHasFlagship();
        FleetMemberAPI flagship = fleet.getFlagship();
        if (flagship != null && flagship.getCaptain() != null) {
            fleet.setCommander(flagship.getCaptain());
        }
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
    }

    private static FleetMemberAPI addShip(CampaignFleetAPI fleet, FactionAPI faction, String role, ShipPickParams pickParams, Random random) {
        for (ShipRolePick pick : faction.pickShip(role, pickParams, null, random)) {
            FleetMemberAPI member = fleet.getFleetData().addFleetMember(pick.variantId);
            if (member != null) {
                return member;
            }
        }
        return null;
    }

    private static void crew(FleetMemberAPI member, int tier, boolean corrupted, float averageDMods, Random random) {
        ShipVariantAPI variant = member.getVariant().clone();
        variant.setSource(VariantSource.REFIT);
        member.setVariant(variant, false, false);
        int dMods = DefaultFleetInflater.getNumDModsToAdd(variant, averageDMods, random);
        if (dMods > 0) {
            DModManager.addDMods(member, false, dMods, random);
        }
        String coreId = DebrisRules.coreId(DebrisRules.coreLevel(tier, random), corrupted);
        AICoreOfficerPlugin core = Misc.getAICoreOfficerPlugin(coreId);
        if (core != null) {
            PersonAPI captain = core.createPerson(coreId, FACTION_ID, random);
            member.setCaptain(captain);
        }
        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
    }

    static void prepare(CampaignFleetAPI fleet, String runId, int tier, long seed, Random random) {
        RemnantSeededFleetManager.initRemnantFleetProperties(random, fleet, true);
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.unset(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN);
        memory.set(ChartIds.TIER_MEMORY_KEY, tier);
        memory.set(ChartIds.SEED_MEMORY_KEY, Long.toString(seed, SEED_RADIX));
        memory.set(ChartIds.fleetRunKey(runId), true);
        memory.set(BUILD_SEED_KEY, Long.toString(seed, SEED_RADIX));
        memory.set(DORMANT_KEY, true);
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        ChartFleetLoot.recordStart(fleet);
        for (String abilityId : REMOVED_ABILITIES) {
            if (fleet.hasAbility(abilityId)) {
                fleet.removeAbility(abilityId);
            }
        }
        fleet.setNullAIActionText(Translation.gameText("chart.debris.fleet.dormant"));
        fleet.setNoAutoDespawn(true);
        fleet.addTag(ChartIds.ENTITY_TAG);
        fleet.addTag(ChartIds.runTag(runId));
        fleet.addTag(ChartIds.CONTENT_TAG);
        fleet.addTag(FLEET_TAG);
        hide(fleet);
    }

    public static boolean isDebrisFleet(Object entity) {
        return entity instanceof CampaignFleetAPI fleet && fleet.hasTag(FLEET_TAG);
    }

    public static boolean isDormant(CampaignFleetAPI fleet) {
        return isDebrisFleet(fleet) && fleet.getMemoryWithoutUpdate().getBoolean(DORMANT_KEY);
    }

    public static boolean isCorrupted(CampaignFleetAPI fleet) {
        return isDebrisFleet(fleet) && fleet.getMemoryWithoutUpdate().getBoolean(CORRUPTED_KEY);
    }

    public static void hide(CampaignFleetAPI fleet) {
        fleet.getDetectedRangeMod().modifyMult(HIDDEN_MOD_ID, 0f);
    }

    public static boolean isHidden(CampaignFleetAPI fleet) {
        return fleet.getDetectedRangeMod().getMultBonus(HIDDEN_MOD_ID) != null;
    }

    public static void reveal(CampaignFleetAPI fleet) {
        if (fleet != null) {
            fleet.getDetectedRangeMod().unmodifyMult(HIDDEN_MOD_ID);
        }
    }

    public static boolean corrupt(CampaignFleetAPI fleet, ChartGenContext context) {
        if (!isDormant(fleet) || isCorrupted(fleet)) {
            return false;
        }
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        int tier = memory.get(ChartIds.TIER_MEMORY_KEY) instanceof Number storedTier ? storedTier.intValue() : ChartTiers.LOWEST_TIER;
        long seed = buildSeed(memory);
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            fleet.getFleetData().removeFleetMember(member);
        }
        populate(fleet, hullFactions(Global.getSector()), tier, context, seed, true);
        memory.set(CORRUPTED_KEY, true);
        memory.set(ChartIds.STRENGTH_BONUS_MEMORY_KEY, DebrisRules.CORRUPTED_STRENGTH_BONUS_PERCENT);
        ChartFleetLoot.recordStart(fleet);
        fleet.setName(Translation.gameText("chart.debris.fleet.corrupted.name"));
        return true;
    }

    static long buildSeed(MemoryAPI memory) {
        try {
            return Long.parseLong(memory.getString(BUILD_SEED_KEY), SEED_RADIX);
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    public static boolean wake(CampaignFleetAPI fleet) {
        if (!isDormant(fleet)) {
            return false;
        }
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.unset(DORMANT_KEY);
        memory.unset(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE);
        memory.unset(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT);
        memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        memory.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true);
        reveal(fleet);
        Vector2f home = new Vector2f(fleet.getLocation());
        if (fleet.getAI() == null) {
            fleet.setAI(Global.getFactory().createFleetAI(fleet));
            fleet.setLocation(home.x, home.y);
        }
        fleet.clearAssignments();
        LocationAPI location = fleet.getContainingLocation();
        if (location != null) {
            fleet.addAssignment(FleetAssignment.ORBIT_AGGRESSIVE, location.createToken(home.x, home.y), ChartContentGenerator.ASSIGNMENT_DAYS);
        }
        return true;
    }
}
