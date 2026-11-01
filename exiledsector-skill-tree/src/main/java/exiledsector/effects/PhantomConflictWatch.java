package exiledsector.effects;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.PlayerFleet;
import exiledsector.compat.MagicLibCompat;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Message;
import exiledsector.i18n.Translation;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.AllocatedNode;
import exiledsector.skills.HullModNames;
import exiledsector.skills.InstalledHullMods;
import exiledsector.skills.LearnedPhantomConflicts;
import exiledsector.skills.RespecPlan;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import exiledsector.ui.CampaignMessages;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PhantomConflictWatch {

    private static final Map<String, Revert> PENDING_REVERTS = new LinkedHashMap<>();
    private static volatile boolean revertsPending;

    sealed interface Revert permits LearnedRevert, DeclaredRevert {

        String memberId();

        String key();

        LearnedPhantomConflicts.Conflict conflictOn(ShipVariantAPI variant, ShipSkillData data);

        List<SkillNode> providers(ShipSkillData data);
    }

    record LearnedRevert(String memberId, String phantomHullModId) implements Revert {

        public String key() {
            return memberId + "|" + phantomHullModId;
        }

        public LearnedPhantomConflicts.Conflict conflictOn(ShipVariantAPI variant, ShipSkillData data) {
            return PhantomConflictWatch.conflictOn(variant, phantomHullModId);
        }

        public List<SkillNode> providers(ShipSkillData data) {
            List<SkillNode> providers = new ArrayList<>();
            for (AllocatedNode allocated : AllocatedNode.of(data)) {
                if (allocated.effectiveType().getPhantomHullModIds().contains(phantomHullModId)) {
                    providers.add(allocated.node());
                }
            }
            return providers;
        }
    }

    record DeclaredRevert(String memberId, String nodeId) implements Revert {

        public String key() {
            return memberId + "|node:" + nodeId;
        }

        public LearnedPhantomConflicts.Conflict conflictOn(ShipVariantAPI variant, ShipSkillData data) {
            AllocatedNode allocated = allocatedNode(data);
            String hullModId = allocated == null ? null : fixedConflict(allocated, variant);
            return hullModId == null ? null : new LearnedPhantomConflicts.Conflict(hullModId);
        }

        public List<SkillNode> providers(ShipSkillData data) {
            AllocatedNode allocated = allocatedNode(data);
            return allocated == null ? List.of() : List.of(allocated.node());
        }

        private AllocatedNode allocatedNode(ShipSkillData data) {
            for (AllocatedNode allocated : AllocatedNode.of(data)) {
                if (allocated.node().getId().equals(nodeId)) {
                    return allocated;
                }
            }
            return null;
        }
    }

    private PhantomConflictWatch() {
    }

    static Set<String> withoutLearnedConflicts(Set<String> phantomHullModIds, ShipVariantAPI variant) {
        if (phantomHullModIds.isEmpty() || variant == null || LearnedPhantomConflicts.isEmpty()) {
            return phantomHullModIds;
        }
        Set<String> placeable = new LinkedHashSet<>();
        for (String phantomHullModId : phantomHullModIds) {
            if (conflictOn(variant, phantomHullModId) == null) {
                placeable.add(phantomHullModId);
            }
        }
        return placeable.size() == phantomHullModIds.size() ? phantomHullModIds : placeable;
    }

    private static LearnedPhantomConflicts.Conflict conflictOn(ShipVariantAPI variant, String phantomHullModId) {
        ShipHullSpecAPI hullSpec = variant.getHullSpec();
        return LearnedPhantomConflicts.conflictFor(phantomHullModId, hullSpec == null ? null : hullSpec.getHullId(),
                hullModId -> InstalledHullMods.hasFixedHullModOfItsOwn(variant, hullModId));
    }

    static void queueRevertsForBlocked(FleetMemberAPI member, Set<String> wantedPhantomHullModIds, Set<String> placeable,
                                       boolean playerTree) {
        if (!playerTree || member == null || placeable.size() == wantedPhantomHullModIds.size()) {
            return;
        }
        for (String phantomHullModId : wantedPhantomHullModIds) {
            if (!placeable.contains(phantomHullModId)) {
                queueRevert(new LearnedRevert(member.getId(), phantomHullModId));
            }
        }
    }

    static void queueRevertsForFixedConflicts(FleetMemberAPI member, ShipVariantAPI variant, List<AllocatedNode> allocatedNodes,
                                              boolean playerTree) {
        if (!playerTree || member == null || variant == null) {
            return;
        }
        for (AllocatedNode allocated : allocatedNodes) {
            if (fixedConflict(allocated, variant) != null) {
                queueRevert(new DeclaredRevert(member.getId(), allocated.node().getId()));
            }
        }
    }

    private static String fixedConflict(AllocatedNode allocated, ShipVariantAPI variant) {
        for (String hullModId : allocated.exclusiveHullModIds()) {
            if (PlayerShipFacts.hasFixedHullMod(variant, hullModId)) {
                return hullModId;
            }
        }
        return null;
    }

    static void inspect(FleetMemberAPI member, ShipVariantAPI variant, Set<String> wantedPhantomHullModIds, boolean playerTree) {
        if (variant == null || wantedPhantomHullModIds.isEmpty()) {
            return;
        }
        learnFromRemovalAttempt(member, variant, wantedPhantomHullModIds, playerTree);
        ShipHullSpecAPI hullSpec = variant.getHullSpec();
        for (String phantomHullModId : wantedPhantomHullModIds) {
            if (hullSpec != null && InstalledHullMods.isInstalledBySkillTree(variant, phantomHullModId)
                    && !variant.hasHullMod(phantomHullModId)) {
                LearnedPhantomConflicts.learnHull(phantomHullModId, hullSpec.getHullId());
                queueIfPlayer(member, phantomHullModId, playerTree);
            }
        }
    }

    private static void learnFromRemovalAttempt(FleetMemberAPI member, ShipVariantAPI variant, Set<String> wantedPhantomHullModIds,
                                                boolean playerTree) {
        if (!variant.hasHullMod(MagicLibCompat.WARNING_HULLMOD_ID)) {
            return;
        }
        List<String> reason = MagicIncompatibleHullmods.getReason(variant);
        if (reason == null || reason.size() < 2) {
            return;
        }
        String phantomHullModId = reason.get(0);
        String causeHullModId = reason.get(1);
        if (!wantedPhantomHullModIds.contains(phantomHullModId) || causeHullModId == null || causeHullModId.isBlank()
                || !InstalledHullMods.hasFixedHullModOfItsOwn(variant, causeHullModId)) {
            return;
        }
        LearnedPhantomConflicts.learnHullMod(phantomHullModId, causeHullModId);
        queueIfPlayer(member, phantomHullModId, playerTree);
    }

    private static void queueIfPlayer(FleetMemberAPI member, String phantomHullModId, boolean playerTree) {
        if (playerTree && member != null) {
            queueRevert(new LearnedRevert(member.getId(), phantomHullModId));
        }
    }

    private static synchronized void queueRevert(Revert revert) {
        PENDING_REVERTS.putIfAbsent(revert.key(), revert);
        revertsPending = true;
    }

    public static boolean hasPendingReverts() {
        return revertsPending;
    }

    public static boolean applyPendingReverts() {
        return applyPendingReverts(null);
    }

    public static synchronized boolean applyPendingReverts(String onlyMemberId) {
        if (!revertsPending) {
            return false;
        }
        boolean reverted = false;
        for (Revert revert : List.copyOf(PENDING_REVERTS.values())) {
            if (onlyMemberId != null && !onlyMemberId.equals(revert.memberId())) {
                continue;
            }
            PENDING_REVERTS.remove(revert.key());
            reverted |= apply(revert);
        }
        revertsPending = !PENDING_REVERTS.isEmpty();
        return reverted;
    }

    private static boolean apply(Revert revert) {
        FleetMemberAPI member = playerFleetMember(revert.memberId());
        ShipSkillData data = ShipSkillDataManager.find(revert.memberId());
        if (member == null || data == null || member.getVariant() == null) {
            return false;
        }
        LearnedPhantomConflicts.Conflict conflict = revert.conflictOn(member.getVariant(), data);
        if (conflict == null) {
            return false;
        }
        List<SkillNode> providers = revert.providers(data);
        if (providers.isEmpty()) {
            return false;
        }
        CargoAPI cargo = PlayerFleet.cargoOrNull();
        for (SkillNode provider : providers) {
            if (!data.isAllocated(provider.getId())) {
                continue;
            }
            for (SkillNode released : RespecPlan.of(data, SkillTree.topology(), ShipTrees.resolveStartingRootId(data), provider)) {
                ShipTrees.deallocateReleasingCharges(data, released).forEach(charged -> charged.refundTo(cargo));
            }
        }
        ShipTreeSync.memberChanged(member, member.getVariant());
        member.setStatUpdateNeeded(true);
        member.updateStats();
        FighterBayOverflow.returnUnhousedWingsAndAnnounce(member, member.getVariant(), cargo);
        announce(member, providers.get(0), conflict);
        return true;
    }

    private static FleetMemberAPI playerFleetMember(String memberId) {
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (playerFleet == null) {
            return null;
        }
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            if (member.getId().equals(memberId)) {
                return member;
            }
        }
        return null;
    }

    private static void announce(FleetMemberAPI member, SkillNode removedNode, LearnedPhantomConflicts.Conflict conflict) {
        if (!CampaignMessages.available()) {
            return;
        }
        String message = I18n.forGameText(() -> {
            Message text = conflict.isWholeHull() ? Translation.msg("phantomConflict.removedByHull")
                    : Translation.msg("phantomConflict.removed").arg("hullmod", HullModNames.displayName(conflict.hullModId()));
            return text.arg("ship", member.getShipName()).arg("node", removedNode.getType().getDisplayName()).text();
        });
        CampaignMessages.plain(message, Misc.getNegativeHighlightColor());
    }

    public static synchronized void clearPendingReverts() {
        PENDING_REVERTS.clear();
        revertsPending = false;
    }
}
