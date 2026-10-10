package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
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
import exiledsector.skills.SkillItemCost;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTree;
import org.magiclib.util.MagicIncompatibleHullmods;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PhantomConflictWatch {

    static final String VANILLA_HULL_MOD_PACKAGE = "com.fs.starfarer.api.impl.hullmods.";
    private static final String OWN_HULL_MOD_PREFIX = "exiledSector_";
    private static final Map<String, Revert> PENDING_REVERTS = new LinkedHashMap<>();
    private static volatile boolean revertsPending;

    record Revert(String memberId, String phantomHullModId, String causeHullModId, boolean wholeHull) {
    }

    private PhantomConflictWatch() {
    }

    static Set<String> withoutLearnedConflicts(Set<String> phantomHullModIds, ShipVariantAPI variant) {
        if (phantomHullModIds.isEmpty() || variant == null) {
            return phantomHullModIds;
        }
        ShipHullSpecAPI hullSpec = variant.getHullSpec();
        String baseHullId = hullSpec == null ? null : hullSpec.getBaseHullId();
        Set<String> placeable = new LinkedHashSet<>();
        for (String phantomHullModId : phantomHullModIds) {
            if (LearnedPhantomConflicts.conflictFor(phantomHullModId, baseHullId,
                    hullModId -> InstalledHullMods.hasHullModOfItsOwn(variant, hullModId)) == null) {
                placeable.add(phantomHullModId);
            }
        }
        return placeable.size() == phantomHullModIds.size() ? phantomHullModIds : placeable;
    }

    static void inspect(FleetMemberAPI member, ShipVariantAPI variant, Set<String> wantedPhantomHullModIds, boolean playerTree) {
        if (variant == null || wantedPhantomHullModIds.isEmpty()) {
            return;
        }
        for (String phantomHullModId : wantedPhantomHullModIds) {
            if (InstalledHullMods.isInstalledBySkillTree(variant, phantomHullModId) && !variant.hasHullMod(phantomHullModId)) {
                handleStrip(member, variant, phantomHullModId, playerTree);
            }
        }
    }

    private static void handleStrip(FleetMemberAPI member, ShipVariantAPI variant, String phantomHullModId, boolean playerTree) {
        String namedCause = magicLibCause(variant, phantomHullModId);
        if (namedCause != null && (!variant.hasHullMod(namedCause) || isRemovable(variant, namedCause))) {
            return;
        }
        List<String> suspects = namedCause != null ? List.of(namedCause) : suspects(variant, phantomHullModId);
        boolean allFixed = !suspects.isEmpty() && suspects.stream().noneMatch(suspect -> isRemovable(variant, suspect));
        String causeHullModId = null;
        boolean wholeHull = false;
        if (allFixed && suspects.size() == 1) {
            causeHullModId = suspects.get(0);
            LearnedPhantomConflicts.learnHullMod(phantomHullModId, causeHullModId);
        } else if (allFixed && variant.getHullSpec() != null) {
            wholeHull = true;
            LearnedPhantomConflicts.learnHull(phantomHullModId, variant.getHullSpec().getBaseHullId());
        }
        if (playerTree && member != null) {
            queueRevert(new Revert(member.getId(), phantomHullModId, causeHullModId, wholeHull));
        }
    }

    private static String magicLibCause(ShipVariantAPI variant, String phantomHullModId) {
        List<String> reason = MagicIncompatibleHullmods.getReason(variant);
        if (reason == null || reason.size() < 2 || !phantomHullModId.equals(reason.get(0))) {
            return null;
        }
        String cause = reason.get(1);
        return cause == null || cause.isBlank() || Global.getSettings().getHullModSpec(cause) == null ? null : cause;
    }

    static List<String> suspects(ShipVariantAPI variant, String phantomHullModId) {
        List<String> suspects = new ArrayList<>();
        for (String hullModId : variant.getHullMods()) {
            if (hullModId.equals(phantomHullModId) || hullModId.startsWith(OWN_HULL_MOD_PREFIX)
                    || InstalledHullMods.isInstalledBySkillTree(variant, hullModId) || isVanilla(hullModId)) {
                continue;
            }
            suspects.add(hullModId);
        }
        return suspects;
    }

    private static boolean isVanilla(String hullModId) {
        HullModSpecAPI spec = Global.getSettings().getHullModSpec(hullModId);
        String effectClass = spec == null ? null : spec.getEffectClass();
        return effectClass == null || effectClass.startsWith(VANILLA_HULL_MOD_PACKAGE);
    }

    static boolean isRemovable(ShipVariantAPI variant, String hullModId) {
        boolean builtIn = variant.getHullSpec() != null && variant.getHullSpec().isBuiltInMod(hullModId);
        return !builtIn && !variant.getPermaMods().contains(hullModId) && !variant.getSMods().contains(hullModId);
    }

    private static synchronized void queueRevert(Revert revert) {
        PENDING_REVERTS.putIfAbsent(revert.memberId() + "|" + revert.phantomHullModId(), revert);
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
            PENDING_REVERTS.remove(revert.memberId() + "|" + revert.phantomHullModId());
            reverted |= apply(revert);
        }
        revertsPending = !PENDING_REVERTS.isEmpty();
        return reverted;
    }

    private static boolean apply(Revert revert) {
        ShipSkillData data = ShipSkillDataManager.get(revert.memberId());
        List<SkillNode> providers = new ArrayList<>();
        for (AllocatedNode allocated : AllocatedNode.of(data)) {
            if (allocated.effectiveType().getPhantomHullModIds().contains(revert.phantomHullModId())) {
                providers.add(allocated.node());
            }
        }
        if (providers.isEmpty()) {
            return false;
        }
        CargoAPI cargo = playerCargo();
        for (SkillNode provider : providers) {
            if (!data.isAllocated(provider.getId())) {
                continue;
            }
            for (SkillNode released : RespecPlan.of(data, SkillTree.topology(), data.resolveStartingRootId(), provider)) {
                Set<String> allocatedBefore = new LinkedHashSet<>(data.getAllocatedNodeIds());
                data.deallocate(released);
                for (String releasedId : allocatedBefore) {
                    if (!data.isAllocated(releasedId)) {
                        refund(cargo, data.takeItemCharge(releasedId));
                    }
                }
            }
        }
        FleetMemberAPI member = playerFleetMember(revert.memberId());
        if (member != null) {
            ShipTreeSync.memberChanged(member, member.getVariant());
            member.setStatUpdateNeeded(true);
        }
        announce(member, providers.get(0), revert);
        return true;
    }

    private static void refund(CargoAPI cargo, SkillItemCost charged) {
        if (cargo != null && charged != null) {
            cargo.addCommodity(charged.itemId(), charged.quantity());
        }
    }

    private static CargoAPI playerCargo() {
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
        return playerFleet == null ? null : playerFleet.getCargo();
    }

    private static FleetMemberAPI playerFleetMember(String memberId) {
        CampaignFleetAPI playerFleet = Global.getSector() == null ? null : Global.getSector().getPlayerFleet();
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

    private static void announce(FleetMemberAPI member, SkillNode removedNode, Revert revert) {
        CampaignUIAPI campaignUi = Global.getSector() == null ? null : Global.getSector().getCampaignUI();
        if (campaignUi == null) {
            return;
        }
        String message = I18n.forGameText(() -> {
            String shipName = member == null ? "" : member.getShipName();
            Message text;
            if (revert.causeHullModId() != null) {
                text = Translation.msg("phantomConflict.removed").arg("hullmod", HullModNames.displayName(revert.causeHullModId()));
            } else if (revert.wholeHull()) {
                text = Translation.msg("phantomConflict.removedByHull");
            } else {
                text = Translation.msg("phantomConflict.removedByUnknown");
            }
            return text.arg("ship", shipName).arg("node", removedNode.getType().getDisplayName()).text();
        });
        campaignUi.addMessage(message.replace("%", "%%"), Misc.getNegativeHighlightColor());
    }

    static synchronized void clearPendingReverts() {
        PENDING_REVERTS.clear();
        revertsPending = false;
    }
}
