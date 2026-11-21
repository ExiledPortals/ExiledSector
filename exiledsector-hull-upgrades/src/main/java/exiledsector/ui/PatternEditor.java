package exiledsector.ui;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.PlayerFleet;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.crafting.material.CraftingMaterials;
import exiledsector.items.hull.ShipSection;
import exiledsector.items.pattern.HullPattern;
import exiledsector.items.pattern.HullPatternActivity;
import exiledsector.items.pattern.HullPatternClones;
import exiledsector.items.pattern.HullPatternRules;
import exiledsector.items.pattern.HullPatternShips;
import exiledsector.items.pattern.HullPatterns;
import exiledsector.items.pattern.HullTypes;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
import exiledsector.ui.socket.HullPatternPanel;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class PatternEditor {

    private final FleetMemberAPI member;
    private final SectionEditor sectionEditor;
    private int shownPatternRevision = Integer.MIN_VALUE;
    private int shownEditRevision = Integer.MIN_VALUE;
    private int shownFeedstock = Integer.MIN_VALUE;
    private HullPatternPanel.View shownView;
    private HullPatternPanel.View lookView;
    private SectionsDisplay.PatternLook shownLook;

    PatternEditor(FleetMemberAPI member, SectionEditor sectionEditor) {
        this.member = member;
        this.sectionEditor = sectionEditor;
    }

    boolean refreshIfChanged() {
        int patternRevision = HullPatterns.revision();
        int editRevision = sectionEditor.revision();
        int feedstock = wallet().count(HullPatternRules.FEEDSTOCK_ID);
        if (shownView != null && patternRevision == shownPatternRevision && editRevision == shownEditRevision && feedstock == shownFeedstock) {
            return false;
        }
        shownPatternRevision = patternRevision;
        shownEditRevision = editRevision;
        shownFeedstock = feedstock;
        HullPatternPanel.View view = buildView(feedstock);
        boolean changed = !view.equals(shownView);
        shownView = view;
        return changed;
    }

    HullPatternPanel.View view() {
        if (shownView == null) {
            refreshIfChanged();
        }
        return shownView;
    }

    SectionsDisplay.PatternLook sectionLook() {
        HullPatternPanel.View shown = view();
        if (shown != lookView || shownLook == null) {
            lookView = shown;
            shownLook = new SectionsDisplay.PatternLook(litSections(shown), section -> sectionHint(shown, section));
        }
        return shownLook;
    }

    private static Set<ShipSection> litSections(HullPatternPanel.View shown) {
        Set<ShipSection> lit = EnumSet.noneOf(ShipSection.class);
        if (shown.state() == HullPatternPanel.State.PATTERN_HERE) {
            shown.sockets().stream().filter(HullPatternPanel.SocketEntry::selected).forEach(entry -> lit.add(entry.section()));
        } else if (shown.state() == HullPatternPanel.State.FOLLOWER) {
            shown.followerSockets().stream().filter(socket -> socket.status() == HullPatternPanel.FollowerStatus.CLONED)
                    .forEach(socket -> lit.add(socket.section()));
        }
        return lit;
    }

    private static StyledText sectionHint(HullPatternPanel.View shown, ShipSection section) {
        if (shown.state() != HullPatternPanel.State.PATTERN_HERE) {
            return null;
        }
        for (HullPatternPanel.SocketEntry entry : shown.sockets()) {
            if (entry.section() != section) {
                continue;
            }
            if (entry.selected()) {
                return Translation.styled("pattern.section.hintDeselect");
            }
            return entry.block() == null ? Translation.styled("pattern.section.hintSelect")
                    : StyledText.of(HullPatternPanel.selectBlockText(entry.block(), shown.selectCost()));
        }
        return null;
    }

    HullPatternPanel.Listener listener(Runnable closed) {
        return new HullPatternPanel.Listener() {
            @Override
            public void create() {
                finish(HullPatternRules.create(member, inFleet(member)) != null, true);
            }

            @Override
            public void deletePattern() {
                finish(HullPatternRules.delete(currentPattern()), false);
            }

            @Override
            public void select(ShipSection section) {
                HullPattern pattern = ownPattern();
                ShipSections.UnlockedSection patternSection = pattern == null ? null : HullPatternRules.patternSection(sectionEditor.sections(), section);
                finish(pattern != null && HullPatternRules.select(pattern, patternSection, wallet()), true);
            }

            @Override
            public void deselect(ShipSection section) {
                HullPattern pattern = ownPattern();
                finish(pattern != null && HullPatternRules.deselect(pattern, section), false);
            }

            @Override
            public void link(String shipId) {
                HullPattern pattern = ownPattern();
                FleetMemberAPI candidate = HullPatternActivity.fleetMember(shipId);
                finish(pattern != null && HullPatternRules.link(pattern, candidate, candidate != null, wallet()), true);
            }

            @Override
            public void unlink(String shipId) {
                HullPattern pattern = HullPatterns.get().ofFollower(shipId);
                finish(pattern != null && HullPatternRules.unlink(pattern, shipId), false);
            }

            @Override
            public void closed() {
                closed.run();
            }
        };
    }

    private void finish(boolean applied, boolean paidSound) {
        if (!applied) {
            SkillTreeSounds.refused();
            return;
        }
        sectionEditor.patternChanged(paidSound);
        shownView = null;
    }

    private HullPattern currentPattern() {
        HullPatterns patterns = HullPatterns.get();
        HullPattern own = patterns.ofPatternShip(member.getId());
        if (own != null) {
            return own;
        }
        HullPattern followed = patterns.ofFollower(member.getId());
        return followed != null ? followed : patterns.forHullType(HullTypes.of(member));
    }

    private HullPattern ownPattern() {
        return HullPatterns.get().ofPatternShip(member.getId());
    }

    private static CraftingMaterials wallet() {
        return CraftingMaterials.forPlayer(PlayerFleet.cargoOrNull());
    }

    private static boolean inFleet(FleetMemberAPI shipMember) {
        return shipMember != null && HullPatternActivity.fleetMember(shipMember.getId()) != null;
    }

    private HullPatternPanel.View buildView(int feedstock) {
        HullPatterns patterns = HullPatterns.get();
        String hullType = HullTypes.of(member);
        String hullName = HullTypes.displayName(hullType);
        HullPattern own = patterns.ofPatternShip(member.getId());
        HullPattern followed = own == null ? patterns.ofFollower(member.getId()) : null;
        HullPattern sameType = own == null && followed == null ? patterns.forHullType(hullType) : null;
        if (own != null) {
            return patternView(own, hullName, feedstock);
        }
        if (followed != null) {
            return followerView(followed);
        }
        if (sameType != null) {
            return view(HullPatternPanel.State.PATTERN_ELSEWHERE, hullName, patternShip(sameType), sameType, List.of(), List.of(), List.of());
        }
        HullPatternRules.CreateBlock block = HullPatternRules.createBlock(patterns, member, inFleet(member));
        HullPatternPanel.State state = block == HullPatternRules.CreateBlock.NOT_IN_FLEET ? HullPatternPanel.State.NOT_IN_FLEET
                : block == HullPatternRules.CreateBlock.NOT_PATTERNABLE ? HullPatternPanel.State.NOT_PATTERNABLE : HullPatternPanel.State.NO_PATTERN;
        return view(state, hullName, null, null, List.of(), List.of(), List.of());
    }

    private HullPatternPanel.View view(HullPatternPanel.State state, String hullName, HullPatternPanel.PatternShip patternShip, HullPattern pattern,
                                       List<HullPatternPanel.SocketEntry> sockets, List<HullPatternPanel.ShipEntry> ships,
                                       List<HullPatternPanel.FollowerSocket> followerSockets) {
        return new HullPatternPanel.View(state, member.getId(), member.getShipName(), HullTypes.of(member), hullName, patternShip,
                pattern == null ? 0 : HullPatternRules.selectCost(pattern), pattern == null ? 0 : HullPatternRules.linkCost(pattern),
                pattern == null ? 0 : pattern.selectedCount(), pattern == null ? 0 : pattern.followerCount(), sockets, ships, followerSockets);
    }

    private HullPatternPanel.View patternView(HullPattern pattern, String hullName, int feedstock) {
        List<ShipSections.UnlockedSection> ownSections = sectionEditor.sections();
        Set<ShipSection> shownSections = new LinkedHashSet<>();
        ownSections.forEach(unlocked -> shownSections.add(unlocked.section()));
        shownSections.addAll(pattern.selectedSections());
        List<HullPatternPanel.SocketEntry> sockets = new ArrayList<>();
        for (ShipSection section : ShipSection.all()) {
            if (shownSections.contains(section)) {
                sockets.add(socketEntry(pattern, section, HullPatternRules.patternSection(ownSections, section), feedstock));
            }
        }
        return view(HullPatternPanel.State.PATTERN_HERE, hullName, null, pattern, sockets, shipEntries(pattern, feedstock), List.of());
    }

    private static HullPatternPanel.SocketEntry socketEntry(HullPattern pattern, ShipSection section, ShipSections.UnlockedSection unlocked,
                                                            int feedstock) {
        HullPatternPanel.SocketStatus status;
        Item item = unlocked == null ? null : unlocked.item();
        if (unlocked == null) {
            status = HullPatternPanel.SocketStatus.LOCKED;
        } else if (!unlocked.active()) {
            status = HullPatternPanel.SocketStatus.INACTIVE;
        } else if (item == null) {
            status = HullPatternPanel.SocketStatus.EMPTY;
        } else if (HullPatternClones.isUnique(item)) {
            status = HullPatternPanel.SocketStatus.UNIQUE;
        } else {
            status = HullPatternPanel.SocketStatus.ITEM;
        }
        boolean showsItem = status == HullPatternPanel.SocketStatus.ITEM;
        return new HullPatternPanel.SocketEntry(section, status, showsItem ? item.name() : null, showsItem ? item.rarity().color() : null,
                pattern.isSelected(section), HullPatternRules.selectBlock(pattern, unlocked, feedstock));
    }

    private List<HullPatternPanel.ShipEntry> shipEntries(HullPattern pattern, int feedstock) {
        List<HullPatternPanel.ShipEntry> ships = new ArrayList<>();
        Set<String> listed = new LinkedHashSet<>();
        for (FleetMemberAPI candidate : PlayerFleet.members()) {
            if (candidate.getId().equals(pattern.patternShipId()) || !pattern.hullType().equals(HullTypes.of(candidate))) {
                continue;
            }
            listed.add(candidate.getId());
            ships.add(new HullPatternPanel.ShipEntry(candidate.getId(), candidate.getShipName(), candidate, false, pattern.isFollower(candidate.getId()),
                    HullPatternRules.linkBlock(pattern, candidate, true, feedstock)));
        }
        for (String followerId : pattern.followerIds()) {
            if (!listed.contains(followerId)) {
                ships.add(new HullPatternPanel.ShipEntry(followerId, Objects.toString(pattern.followerName(followerId), followerId), null, true, true,
                        null));
            }
        }
        return ships;
    }

    private HullPatternPanel.View followerView(HullPattern pattern) {
        List<HullPatternPanel.FollowerSocket> followerSockets = new ArrayList<>();
        ShipSkillData ownData = ShipSkillDataManager.find(member.getId());
        List<ShipSections.UnlockedSection> ownSections = sectionEditor.sections();
        for (ShipSection section : pattern.selectedSections()) {
            ShipSections.UnlockedSection unlocked = HullPatternRules.patternSection(ownSections, section);
            followerSockets.add(followerSocket(section, unlocked, ownData));
        }
        return view(HullPatternPanel.State.FOLLOWER, HullTypes.displayName(pattern.hullType()), patternShip(pattern), pattern, List.of(), List.of(),
                followerSockets);
    }

    private static HullPatternPanel.FollowerSocket followerSocket(ShipSection section, ShipSections.UnlockedSection unlocked, ShipSkillData ownData) {
        if (unlocked == null) {
            return new HullPatternPanel.FollowerSocket(section, HullPatternPanel.FollowerStatus.NEEDS_UNLOCK, null, null);
        }
        if (unlocked.cloned()) {
            return new HullPatternPanel.FollowerSocket(section, HullPatternPanel.FollowerStatus.CLONED, unlocked.item().name(),
                    unlocked.item().rarity().color());
        }
        boolean ownItem = ownData != null && ownData.getSectionItem(section.id()) != null;
        return new HullPatternPanel.FollowerSocket(section, ownItem ? HullPatternPanel.FollowerStatus.OWN : HullPatternPanel.FollowerStatus.NOTHING,
                null, null);
    }

    private static HullPatternPanel.PatternShip patternShip(HullPattern pattern) {
        HullPatternShips.Location location = HullPatternShips.locate(pattern.patternShipId());
        String shipName = location.member() == null ? HullTypes.displayName(pattern.hullType()) : location.member().getShipName();
        return new HullPatternPanel.PatternShip(shipName, location.where(), location.marketName());
    }
}
