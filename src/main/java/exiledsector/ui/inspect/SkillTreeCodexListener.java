package exiledsector.ui.inspect;

import com.fs.starfarer.api.campaign.listeners.CodexEventListener;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin;
import exiledsector.i18n.I18n;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.PhantomHullModStatus;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SkillTreeCodexListener implements CodexEventListener {

    private final List<CodexEntryPlugin> added = new ArrayList<>();

    @Override
    public void reportAboutToOpenCodex() {
        try {
            for (CodexEntryPlugin entry : new ArrayList<>(CodexDataV2.ENTRIES.values())) {
                if (entry.getParam() instanceof FleetMemberAPI member) {
                    unlinkPhantomHullMods(entry, member);
                    if (!hasSkillTreeEntry(entry)) {
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
        for (CodexEntryPlugin entry : added) {
            CodexDataV2.unlinkAndRemoveTempEntry(entry);
        }
        added.clear();
    }

    private void attach(CodexEntryPlugin memberEntry, FleetMemberAPI member) {
        ShipTreeLookup.ShipTree tree = I18n.forGameText(() -> ShipTreeLookup.find(member));
        if (tree == null) {
            return;
        }
        SkillTreeCodexEntry treeEntry = new SkillTreeCodexEntry(SkillTreeCodexEntry.ID_PREFIX + memberEntry.getId(), member, tree);
        CodexDataV2.ENTRIES.put(treeEntry.getId(), treeEntry);
        memberEntry.addRelatedEntry(treeEntry);
        treeEntry.addRelatedEntry(memberEntry);
        added.add(treeEntry);
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
