package exiledsector.items.pattern;

import exiledsector.items.hull.ShipSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HullPattern {

    private final String hullType;
    private final String patternShipId;
    private final Set<String> selectedSectionIds = new LinkedHashSet<>();
    private final Map<String, String> followerNamesById = new LinkedHashMap<>();

    public HullPattern(String hullType, String patternShipId) {
        this.hullType = hullType;
        this.patternShipId = patternShipId;
    }

    public String hullType() {
        return hullType;
    }

    public String patternShipId() {
        return patternShipId;
    }

    public boolean isSelected(ShipSection section) {
        return section != null && selectedSectionIds.contains(section.id());
    }

    public List<ShipSection> selectedSections() {
        List<ShipSection> sections = new ArrayList<>();
        for (String sectionId : selectedSectionIds) {
            ShipSection section = ShipSection.byIdOrNull(sectionId);
            if (section != null) {
                sections.add(section);
            }
        }
        return sections;
    }

    public int selectedCount() {
        return selectedSections().size();
    }

    boolean select(ShipSection section) {
        return selectedSectionIds.add(section.id());
    }

    boolean deselect(ShipSection section) {
        return selectedSectionIds.remove(section.id());
    }

    public boolean isFollower(String shipId) {
        return shipId != null && followerNamesById.containsKey(shipId);
    }

    public List<String> followerIds() {
        return List.copyOf(followerNamesById.keySet());
    }

    public int followerCount() {
        return followerNamesById.size();
    }

    public String followerName(String shipId) {
        return followerNamesById.get(shipId);
    }

    boolean link(String shipId, String shipName) {
        if (followerNamesById.containsKey(shipId)) {
            return false;
        }
        followerNamesById.put(shipId, shipName);
        return true;
    }

    boolean unlink(String shipId) {
        return followerNamesById.remove(shipId) != null;
    }

    void rememberFollowerName(String shipId, String shipName) {
        if (shipName != null && followerNamesById.containsKey(shipId)) {
            followerNamesById.put(shipId, shipName);
        }
    }
}
