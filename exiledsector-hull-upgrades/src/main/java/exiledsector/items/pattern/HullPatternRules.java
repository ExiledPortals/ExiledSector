package exiledsector.items.pattern;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.items.crafting.ItemDisassembly;
import exiledsector.items.crafting.material.CraftingMaterials;
import exiledsector.items.hull.ShipSection;
import exiledsector.skills.ShipSections;

import java.util.List;

public final class HullPatternRules {

    public static final int FEEDSTOCK_PER_SOCKET = 500;
    public static final int MAX_SOCKETS = 5;
    public static final String FEEDSTOCK_ID = ItemDisassembly.FEEDSTOCK_ITEM_ID;

    public enum CreateBlock {NOT_IN_FLEET, NOT_PATTERNABLE, PATTERN_EXISTS}

    public enum SelectBlock {SECTION_LOCKED, REQUIREMENT_UNMET, HOLDS_UNIQUE, AT_LIMIT, CANNOT_AFFORD}

    public enum LinkBlock {NOT_IN_FLEET, NOT_PATTERNABLE, OTHER_HULL_TYPE, PATTERN_SHIP, CANNOT_AFFORD}

    private HullPatternRules() {
    }

    public static int selectCost(HullPattern pattern) {
        return FEEDSTOCK_PER_SOCKET * pattern.followerCount();
    }

    public static int linkCost(HullPattern pattern) {
        return FEEDSTOCK_PER_SOCKET * pattern.selectedCount();
    }

    public static boolean patternable(FleetMemberAPI member) {
        return member != null && !member.isFighterWing() && !member.isStation() && HullTypes.of(member) != null;
    }

    public static CreateBlock createBlock(HullPatterns patterns, FleetMemberAPI member, boolean inPlayerFleet) {
        if (!inPlayerFleet || member == null) {
            return CreateBlock.NOT_IN_FLEET;
        }
        if (!patternable(member)) {
            return CreateBlock.NOT_PATTERNABLE;
        }
        String hullType = HullTypes.of(member);
        return patterns.forHullType(hullType) != null ? CreateBlock.PATTERN_EXISTS : null;
    }

    public static HullPattern create(FleetMemberAPI patternMember, boolean inPlayerFleet) {
        if (createBlock(HullPatterns.get(), patternMember, inPlayerFleet) != null) {
            return null;
        }
        return HullPatterns.create(HullTypes.of(patternMember), patternMember.getId());
    }

    public static boolean delete(HullPattern pattern) {
        if (pattern == null) {
            return false;
        }
        HullPatternActivity.markFollowersForStatUpdate(pattern);
        return HullPatterns.get().delete(pattern);
    }

    public static ShipSections.UnlockedSection patternSection(List<ShipSections.UnlockedSection> patternSections, ShipSection section) {
        for (ShipSections.UnlockedSection unlocked : patternSections) {
            if (unlocked.section() == section) {
                return unlocked;
            }
        }
        return null;
    }

    public static SelectBlock selectBlock(HullPattern pattern, ShipSections.UnlockedSection patternSection, int feedstock) {
        if (patternSection == null) {
            return SelectBlock.SECTION_LOCKED;
        }
        if (!patternSection.active()) {
            return SelectBlock.REQUIREMENT_UNMET;
        }
        if (HullPatternClones.isUnique(patternSection.item())) {
            return SelectBlock.HOLDS_UNIQUE;
        }
        if (!pattern.isSelected(patternSection.section()) && pattern.selectedCount() >= MAX_SOCKETS) {
            return SelectBlock.AT_LIMIT;
        }
        return feedstock < selectCost(pattern) ? SelectBlock.CANNOT_AFFORD : null;
    }

    public static boolean select(HullPattern pattern, ShipSections.UnlockedSection patternSection, CraftingMaterials wallet) {
        if (patternSection == null || pattern.isSelected(patternSection.section())
                || selectBlock(pattern, patternSection, wallet.count(FEEDSTOCK_ID)) != null || !wallet.take(FEEDSTOCK_ID, selectCost(pattern))) {
            return false;
        }
        pattern.select(patternSection.section());
        changed(pattern);
        return true;
    }

    public static boolean deselect(HullPattern pattern, ShipSection section) {
        if (!pattern.deselect(section)) {
            return false;
        }
        changed(pattern);
        return true;
    }

    public static LinkBlock linkBlock(HullPattern pattern, FleetMemberAPI candidate, boolean inPlayerFleet, int feedstock) {
        if (candidate == null || !inPlayerFleet) {
            return LinkBlock.NOT_IN_FLEET;
        }
        if (candidate.getId().equals(pattern.patternShipId())) {
            return LinkBlock.PATTERN_SHIP;
        }
        if (!patternable(candidate)) {
            return LinkBlock.NOT_PATTERNABLE;
        }
        if (!pattern.hullType().equals(HullTypes.of(candidate))) {
            return LinkBlock.OTHER_HULL_TYPE;
        }
        return feedstock < linkCost(pattern) ? LinkBlock.CANNOT_AFFORD : null;
    }

    public static boolean link(HullPattern pattern, FleetMemberAPI candidate, boolean inPlayerFleet, CraftingMaterials wallet) {
        if (candidate == null || pattern.isFollower(candidate.getId()) || linkBlock(pattern, candidate, inPlayerFleet, wallet.count(FEEDSTOCK_ID)) != null
                || !wallet.take(FEEDSTOCK_ID, linkCost(pattern))) {
            return false;
        }
        pattern.link(candidate.getId(), candidate.getShipName());
        changed(pattern);
        return true;
    }

    public static boolean unlink(HullPattern pattern, String followerId) {
        if (!pattern.isFollower(followerId)) {
            return false;
        }
        HullPatternActivity.markFollowersForStatUpdate(pattern);
        pattern.unlink(followerId);
        HullPatterns.markChanged();
        return true;
    }

    private static void changed(HullPattern pattern) {
        HullPatternActivity.markFollowersForStatUpdate(pattern);
        HullPatterns.markChanged();
    }
}
