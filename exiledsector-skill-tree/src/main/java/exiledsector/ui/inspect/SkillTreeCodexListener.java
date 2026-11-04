package exiledsector.ui.inspect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.listeners.CodexEventListener;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin;
import exiledsector.i18n.I18n;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.PhantomHullModStatus;
import exiledsector.skills.SkillTreeSetting;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SkillTreeCodexListener implements CodexEventListener {

    private final List<CodexEntryPlugin> addedEntries = new ArrayList<>();

    @Override
    public void reportAboutToOpenCodex() {
        ItemCodex.refreshUnlocks(Global.getSector());
        try {
            for (CodexEntryPlugin entry : new ArrayList<>(CodexDataV2.ENTRIES.values())) {
                if (entry.getParam() instanceof FleetMemberAPI member) {
                    unlinkPhantomHullMods(entry, member);
                    if (SkillTreeSetting.enabled() && !hasSkillTreeEntry(entry)) {
                        attach(entry, member);
                    }
                }
            }
        } catch (RuntimeException e) {
            Logger.getLogger(SkillTreeCodexListener.class).error("Could not add Exiled Sector skill tree codex entries", e);
        }
    }

    @Override
    public void reportClosedCodex() {
        for (CodexEntryPlugin entry : addedEntries) {
            CodexDataV2.unlinkAndRemoveTempEntry(entry);
        }
        addedEntries.clear();
    }

    private void attach(CodexEntryPlugin memberEntry, FleetMemberAPI member) {
        FleetMemberAPI treeOwner = member;
        ShipTreeLookup.ShipTree shipTree = treeOf(member);
        if (shipTree == null) {
            treeOwner = parentStationInCombat(member);
            shipTree = treeOf(treeOwner);
        }
        if (shipTree == null) {
            return;
        }
        List<CodexEntryPlugin> moduleEntries = moduleEntriesOf(memberEntry);
        SkillTreeCodexEntry treeEntry = new SkillTreeCodexEntry(SkillTreeCodexEntry.ID_PREFIX + memberEntry.getId(), treeOwner, shipTree);
        CodexDataV2.ENTRIES.put(treeEntry.getId(), treeEntry);
        link(memberEntry, treeEntry);
        moduleEntries.forEach(moduleEntry -> link(moduleEntry, treeEntry));
        linkInstalledItems(treeEntry, shipTree);
        addedEntries.add(treeEntry);
    }

    private static ShipTreeLookup.ShipTree treeOf(FleetMemberAPI member) {
        return member == null ? null : I18n.forGameText(() -> ShipTreeLookup.find(member));
    }

    private static FleetMemberAPI parentStationInCombat(FleetMemberAPI moduleMember) {
        CombatEngineAPI engine = Global.getSettings() == null || Global.getCurrentState() != GameState.COMBAT ? null : Global.getCombatEngine();
        if (engine == null) {
            return null;
        }
        for (ShipAPI ship : engine.getShips()) {
            ShipAPI station = ship.getParentStation();
            if (station != null && (ship.getVariant() == moduleMember.getVariant() || ship.getFleetMember() == moduleMember)) {
                return station.getFleetMember();
            }
        }
        return null;
    }

    private static List<CodexEntryPlugin> moduleEntriesOf(CodexEntryPlugin stationEntry) {
        List<CodexEntryPlugin> moduleEntries = new ArrayList<>();
        for (CodexEntryPlugin related : stationEntry.getRelatedEntries()) {
            if (related.getParam2() instanceof FleetMemberAPI && !related.hasTag(CodexDataV2.TAG_EMPTY_MODULE)) {
                moduleEntries.add(related);
            }
        }
        return moduleEntries;
    }

    private static void link(CodexEntryPlugin entry, CodexEntryPlugin treeEntry) {
        entry.addRelatedEntry(treeEntry);
        treeEntry.addRelatedEntry(entry);
    }

    private static void linkInstalledItems(SkillTreeCodexEntry treeEntry, ShipTreeLookup.ShipTree shipTree) {
        List<Item> installed = new ArrayList<>();
        for (String itemId : shipTree.skillData().getSocketedItems().values()) {
            Item item = ItemStore.lookup(itemId);
            if (item != null) {
                installed.add(item);
            }
        }
        for (ShipSummaryPart part : ShipSummaryParts.all()) {
            installed.addAll(part.installedItems(shipTree.skillData(), shipTree.variant()));
        }
        for (Item item : installed) {
            CodexEntryPlugin itemEntry = CodexDataV2.getEntry(ItemCodex.entryId(item.definitionId()));
            if (itemEntry != null && itemEntry.isVisible()) {
                treeEntry.addRelatedEntry(itemEntry);
            }
        }
    }

    private static void unlinkPhantomHullMods(CodexEntryPlugin memberEntry, FleetMemberAPI member) {
        ShipVariantAPI variant = member.getVariant();
        if (variant == null) {
            return;
        }
        Set<String> hullsOwnLinks = null;
        for (String hullModId : variant.getHullMods()) {
            if (!PhantomHullModStatus.isActive(hullModId) || !InstalledHullMods.isInstalledBySkillTree(variant, hullModId)) {
                continue;
            }
            if (hullsOwnLinks == null) {
                hullsOwnLinks = hullsOwnRelatedEntryIds(member);
            }
            String hullModEntryId = CodexDataV2.getHullmodEntryId(hullModId);
            if (hullsOwnLinks.contains(hullModEntryId)) {
                continue;
            }
            memberEntry.removeRelatedEntry(hullModEntryId);
            CodexEntryPlugin hullModEntry = CodexDataV2.getEntry(hullModEntryId);
            if (hullModEntry != null) {
                hullModEntry.removeRelatedEntry(memberEntry.getId());
            }
        }
    }

    private static Set<String> hullsOwnRelatedEntryIds(FleetMemberAPI member) {
        CodexEntryPlugin hullEntry = member.getHullSpec() == null ? null : CodexDataV2.getEntry(CodexDataV2.getFleetMemberEntryId(member));
        return hullEntry == null ? Set.of() : hullEntry.getRelatedEntryIds();
    }

    private static boolean hasSkillTreeEntry(CodexEntryPlugin entry) {
        if (entry instanceof SkillTreeCodexEntry) {
            return true;
        }
        for (CodexEntryPlugin related : entry.getRelatedEntries()) {
            if (related instanceof SkillTreeCodexEntry) {
                return true;
            }
        }
        return false;
    }
}
