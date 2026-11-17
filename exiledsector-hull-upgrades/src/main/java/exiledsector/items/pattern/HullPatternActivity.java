package exiledsector.items.pattern;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.FleetMembers;
import exiledsector.PlayerFleet;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HullPatternActivity {

    private static Map<String, FleetMemberAPI> fleetMembersById;
    private static Map<String, List<ShipSections.UnlockedSection>> shownPatternSections = Map.of();

    private HullPatternActivity() {
    }

    public static FleetMemberAPI fleetMember(String shipId) {
        return shipId == null ? null : fleetMembers().get(shipId);
    }

    public static FleetMemberAPI activePatternMember(HullPattern pattern) {
        return pattern == null ? null : fleetMember(pattern.patternShipId());
    }

    public static List<ShipSections.UnlockedSection> activePatternSections(HullPattern pattern) {
        FleetMemberAPI patternMember = activePatternMember(pattern);
        ShipSkillData patternData = patternMember == null ? null : ShipSkillDataManager.find(pattern.patternShipId());
        return patternData == null ? null : ShipSections.forVariant(patternData, patternMember.getVariant());
    }

    public static void playerFleetSynced() {
        fleetMembersById = null;
        refreshFollowers(HullPatterns.get());
    }

    public static void memberChanged(FleetMemberAPI member) {
        HullPatterns patterns = HullPatterns.get();
        if (member != null && patterns.ofPatternShip(member.getId()) != null) {
            refreshFollowers(patterns);
        }
    }

    public static void reset() {
        fleetMembersById = null;
        shownPatternSections = Map.of();
    }

    static void refreshFollowers(HullPatterns patterns) {
        if (patterns.isEmpty()) {
            shownPatternSections = Map.of();
            return;
        }
        Map<String, List<ShipSections.UnlockedSection>> patternSections = new HashMap<>();
        boolean changed = false;
        for (HullPattern pattern : patterns.all()) {
            List<ShipSections.UnlockedSection> sections = activePatternSections(pattern);
            patternSections.put(pattern.hullType(), sections);
            if (!shownPatternSections.containsKey(pattern.hullType()) || !Objects.equals(shownPatternSections.get(pattern.hullType()), sections)) {
                changed = true;
                markFollowersForStatUpdate(pattern);
            }
        }
        changed |= !patternSections.keySet().equals(shownPatternSections.keySet());
        shownPatternSections = patternSections;
        if (changed) {
            HullPatterns.markChanged();
        }
    }

    static void markFollowersForStatUpdate(HullPattern pattern) {
        for (String followerId : pattern.followerIds()) {
            FleetMemberAPI follower = fleetMember(followerId);
            if (follower != null) {
                follower.setStatUpdateNeeded(true);
            }
        }
    }

    private static Map<String, FleetMemberAPI> fleetMembers() {
        if (fleetMembersById != null) {
            return fleetMembersById;
        }
        CampaignFleetAPI playerFleet = PlayerFleet.orNull();
        if (playerFleet == null) {
            return Map.of();
        }
        Map<String, FleetMemberAPI> membersById = new HashMap<>();
        for (FleetMemberAPI member : FleetMembers.of(playerFleet)) {
            membersById.put(member.getId(), member);
        }
        fleetMembersById = membersById;
        return membersById;
    }
}
