package exiledsector.charts.officer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.VariantSource;
import exiledsector.charts.ChartIds;
import exiledsector.charts.atlas.AtlasRunNode;
import exiledsector.charts.atlas.AtlasState;
import exiledsector.charts.content.ChartAnomalyFleets;
import exiledsector.charts.generation.ChartGenContext;
import exiledsector.charts.officer.scar.HullFacts;
import exiledsector.charts.officer.scar.ScarMemory;
import exiledsector.charts.officer.scar.Scars;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;

import java.util.Random;
import java.util.Set;

public final class OfficerFleets {

    public static final String OFFICER_FLEET_MEMORY_KEY = "$exiledSector_officerFleet";
    public static final String RECORD_MEMORY_KEY = "$exiledSector_officerRecord";
    public static final String FLAGSHIP_MEMORY_KEY = "$exiledSector_officerFlagship";
    public static final String TREE_SEED_MEMORY_KEY = "$exiledSector_officerTreeSeed";
    static final String TREE_SEED_PREFIX = "officer|";
    static final Set<String> FIGHTING_TRAITS = Set.of(Personalities.AGGRESSIVE, Personalities.RECKLESS);

    private OfficerFleets() {
    }

    public static AtlasOfficer officerOf(ChartGenContext context) {
        return officerOf(context == null ? null : context.anomalyData(AtlasOfficer.RUN_DATA_KEY));
    }

    public static AtlasOfficer officerOf(AtlasRunNode node) {
        return officerOf(node == null ? null : node.anomalyData(AtlasOfficer.RUN_DATA_KEY));
    }

    public static AtlasOfficer officerOf(MemoryAPI fleetMemory) {
        Object recordId = fleetMemory == null ? null : fleetMemory.get(RECORD_MEMORY_KEY);
        return recordId instanceof Number number ? record(number.intValue()) : null;
    }

    private static AtlasOfficer officerOf(String runData) {
        Integer recordId = recordIdOfRunData(runData);
        return recordId == null ? null : record(recordId);
    }

    private static AtlasOfficer record(int recordId) {
        AtlasState state = AtlasState.getIfCreated(Global.getSector());
        return state == null ? null : state.officer(recordId);
    }

    public static Integer recordIdOf(AtlasRunNode node) {
        return recordIdOfRunData(node == null ? null : node.anomalyData(AtlasOfficer.RUN_DATA_KEY));
    }

    static Integer recordIdOfRunData(String runData) {
        if (runData == null) {
            return null;
        }
        try {
            return Integer.valueOf(runData);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void dress(CampaignFleetAPI fleet, ChartGenContext context) {
        AtlasOfficer officer = officerOf(context);
        if (fleet != null && officer != null) {
            dress(fleet, officer, factionOf(officer, fleet));
        }
    }

    public static void dress(CampaignFleetAPI fleet, int officerRecordId) {
        AtlasOfficer officer = record(officerRecordId);
        if (fleet != null && officer != null) {
            dress(fleet, officer, factionOf(officer, fleet));
        }
    }

    static void dress(CampaignFleetAPI fleet, AtlasOfficer officer, FactionAPI faction) {
        PersonAPI commander = fleet.getCommander();
        FleetMemberAPI flagship = officer.hasFlagship() ? addFlagship(fleet, officer, commander, faction) : fleet.getFlagship();
        if (commander != null) {
            dressCommander(commander, officer, faction);
        }
        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(OFFICER_FLEET_MEMORY_KEY, true);
        memory.set(RECORD_MEMORY_KEY, officer.recordId());
        if (flagship != null) {
            memory.set(FLAGSHIP_MEMORY_KEY, flagship.getId());
            memory.set(TREE_SEED_MEMORY_KEY, Long.toString(officer.treeSeed(), Character.MAX_RADIX));
            Scars.grant(fleet, flagship.getId(), officer.scars());
        }
        OfficerRank rank = OfficerRanks.rank(officer.rankId());
        if (rank.fleetStrengthPercent() > 0) {
            int current = memory.get(ChartIds.STRENGTH_BONUS_MEMORY_KEY) instanceof Number stored ? stored.intValue() : 0;
            memory.set(ChartIds.STRENGTH_BONUS_MEMORY_KEY, current + rank.fleetStrengthPercent());
        }
        String commanderName = commander == null ? "" : commander.getNameString();
        fleet.setName(I18n.forGameText(() -> Translation.msg("chart.officer.fleet.name").arg("rank", rank.displayName())
                .arg("name", commanderName).text()));
    }

    public static String flagshipTreeSeed(MemoryAPI fleetMemory, FleetMemberAPI member) {
        if (fleetMemory == null || member == null || !member.getId().equals(fleetMemory.getString(FLAGSHIP_MEMORY_KEY))) {
            return null;
        }
        String treeSeed = fleetMemory.getString(TREE_SEED_MEMORY_KEY);
        return treeSeed == null ? null : TREE_SEED_PREFIX + treeSeed;
    }

    public static int flagshipExtraLevels(MemoryAPI fleetMemory, FleetMemberAPI member) {
        if (member == null || !member.getId().equals(ScarMemory.flagshipId(fleetMemory))) {
            return 0;
        }
        return Scars.extraLevels(ScarMemory.scars(fleetMemory));
    }

    static HullFacts flagshipHull(AtlasOfficer officer) {
        SettingsAPI settings = Global.getSettings();
        if (settings == null || !officer.hasFlagship()) {
            return null;
        }
        try {
            return HullFacts.of(settings.getVariant(officer.flagshipVariantId()));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static FactionAPI factionOf(AtlasOfficer officer, CampaignFleetAPI fleet) {
        SectorAPI sector = Global.getSector();
        FactionAPI faction = sector == null ? null : sector.getFaction(officer.factionId());
        return faction != null ? faction : fleet.getFaction();
    }

    private static FleetMemberAPI addFlagship(CampaignFleetAPI fleet, AtlasOfficer officer, PersonAPI commander, FactionAPI faction) {
        FleetMemberAPI previous = fleet.getFlagship();
        if (!ChartAnomalyFleets.addCoreFlagship(fleet, officer.flagshipVariantId(), null, new Random(officer.shipNameSeed()))) {
            return previous;
        }
        FleetMemberAPI flagship = fleet.getFlagship();
        if (flagship == null) {
            return previous;
        }
        if (commander != null && previous != null && previous != flagship) {
            previous.setCaptain(flagship.getCaptain());
            flagship.setCaptain(commander);
            fleet.setCommander(commander);
        }
        flagship.setShipName(faction.pickRandomShipName(new Random(officer.shipNameSeed())));
        ShipVariantAPI variant = flagship.getVariant().clone();
        variant.setSource(VariantSource.REFIT);
        variant.addTag(Tags.VARIANT_UNBOARDABLE);
        for (String dmodId : officer.dmodIds()) {
            variant.addPermaMod(dmodId, false);
        }
        flagship.setVariant(variant, false, true);
        return flagship;
    }

    private static void dressCommander(PersonAPI commander, AtlasOfficer officer, FactionAPI faction) {
        PersonAPI named = OfficerText.namedPerson(faction, officer);
        commander.setName(named.getName());
        commander.setGender(named.getGender());
        commander.setPortraitSprite(faction.createRandomPerson(named.getGender(), new Random(officer.portraitSeed())).getPortraitSprite());
        if (FIGHTING_TRAITS.contains(officer.traitId())) {
            commander.setPersonality(officer.traitId());
        }
        commander.getMemoryWithoutUpdate().set(RECORD_MEMORY_KEY, officer.recordId());
    }
}
