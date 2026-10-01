package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.npc.NpcFreedOp;
import exiledsector.skills.npc.NpcHullMods;
import exiledsector.skills.npc.NpcLayout;
import exiledsector.skills.npc.NpcLayouts;
import exiledsector.skills.npc.NpcLevelTable;
import exiledsector.skills.npc.NpcShipSelector;
import exiledsector.skills.npc.NpcSkillTreeBuilder;
import exiledsector.skills.npc.NpcTreeBuild;
import exiledsector.skills.npc.NpcTreeConfig;
import exiledsector.skills.npc.NpcTreeRecords;
import exiledsector.skills.npc.NpcTreeTag;
import exiledsector.skills.progression.ShipLevelConfig;
import exiledsector.skills.tags.ShipProfile;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class NpcFleetLeveller {

    private NpcFleetLeveller() {
    }

    public static void ensure(CampaignFleetAPI fleet) {
        if (!isLevellable(fleet) || !NpcTreeConfig.isEnabled()) {
            return;
        }
        Map<String, String> records = NpcTreeRecords.of(fleet.getMemoryWithoutUpdate());
        String seedPrefix = null;
        int playerLevel = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            String record = records.get(member.getId());
            if (record == null) {
                if (seedPrefix == null) {
                    playerLevel = Global.getSector().getPlayerStats().getLevel();
                    seedPrefix = Global.getSector().getSeedString() + "|" + fleet.getId() + "|";
                }
                record = decide(member, playerLevel, new Random((seedPrefix + member.getId()).hashCode()));
                records.put(member.getId(), record);
            }
            if (NpcTreeRecords.isLevelled(record)) {
                apply(member, record);
            }
        }
    }

    static boolean isLevellable(CampaignFleetAPI fleet) {
        return fleet != null && !fleet.isPlayerFleet() && !fleet.isStationMode()
                && fleet.getContainingLocation() != null && fleet.getFleetData() != null;
    }

    static String decide(FleetMemberAPI member, int playerLevel, Random random) {
        if (!NpcShipSelector.isCandidate(member) || !NpcShipSelector.isChosen(member, random)) {
            return NpcTreeRecords.NOT_LEVELLED;
        }
        ShipProfile profile = ShipProfile.of(member);
        List<NpcLayout> eligible = NpcLayouts.eligibleFor(profile);
        if (eligible.isEmpty()) {
            return NpcTreeRecords.NOT_LEVELLED;
        }
        NpcLayout layout = eligible.get(random.nextInt(eligible.size()));
        int nodeCount = Math.min(NpcLevelTable.roll(playerLevel, random), ShipLevelConfig.maxAllocatedNodesBesidesRoot());
        NpcHullMods hullMods = NpcHullMods.of(member.getVariant());
        NpcTreeBuild build = NpcSkillTreeBuilder.build(layout, nodeCount, profile, hullMods, NpcFreedOp.of(member, hullMods));
        return NpcTreeTag.encode(layout.id(), build.data());
    }

    static void apply(FleetMemberAPI member, String tag) {
        ShipVariantAPI current = member.getVariant();
        if (current.hasHullMod(SkillTreeHullMod.ID) && tag.equals(NpcTreeTag.find(current))) {
            return;
        }
        ShipVariantAPI variant = SkillTreeInstaller.ownedVariant(member);
        NpcTreeTag.removeAll(variant);
        variant.addTag(tag);
        stripConflictingHullMods(variant, SkillDataResolver.resolve(member, variant));
        if (!variant.hasHullMod(SkillTreeHullMod.ID)) {
            variant.addMod(SkillTreeHullMod.ID);
        }
        member.setStatUpdateNeeded(true);
    }

    private static void stripConflictingHullMods(ShipVariantAPI variant, ShipSkillData data) {
        Set<String> removable = NpcHullMods.of(variant).removable();
        for (AllocatedNode allocated : AllocatedNode.of(data)) {
            for (String hullModId : allocated.exclusiveHullModIds()) {
                if (removable.contains(hullModId)) {
                    variant.removeMod(hullModId);
                }
            }
        }
    }
}
