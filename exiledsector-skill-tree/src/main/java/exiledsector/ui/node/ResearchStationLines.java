package exiledsector.ui.node;

import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.ResearchStationNodes;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;

import java.util.List;

enum ResearchStationLines {
    NONE(List.of(), List.of()),
    STATION_CREWED(List.of(Keys.ALLOWS), List.of(Keys.VIEW_HINT)),
    STATION_INTEGRATED(List.of(Keys.ALLOWS), List.of(Keys.VIEW_HINT)),
    STATION_AUTOMATED(List.of(Keys.ALLOWS, "node.researchStation.automated"), List.of(Keys.VIEW_HINT)),
    SUBSYSTEM_INACTIVE(List.of(), List.of("node.researchStation.subsystemInactive")),
    SUBSYSTEM_SELECTABLE(List.of(), List.of("node.researchStation.selectHint")),
    SUBSYSTEM_SELECTED(List.of(), List.of("node.researchStation.releaseHint"));

    private static final class Keys {
        private static final String ALLOWS = "node.researchStation.allows";
        private static final String VIEW_HINT = "node.researchStation.viewHint";
    }

    private final List<String> leadingKeys;
    private final List<String> trailingKeys;

    ResearchStationLines(List<String> leadingKeys, List<String> trailingKeys) {
        this.leadingKeys = leadingKeys;
        this.trailingKeys = trailingKeys;
    }

    static ResearchStationLines of(SkillNode node, ShipSkillData data, boolean automated) {
        if (ResearchStationNodes.isStation(node)) {
            if (automated) {
                return STATION_AUTOMATED;
            }
            return data.isIntegrated(node.getId()) ? STATION_INTEGRATED : STATION_CREWED;
        }
        if (!ResearchStationNodes.isSubsystem(node)) {
            return NONE;
        }
        if (data.isAllocated(node.getId())) {
            return SUBSYSTEM_SELECTED;
        }
        return ResearchStationNodes.isActive(data, automated) ? SUBSYSTEM_SELECTABLE : SUBSYSTEM_INACTIVE;
    }

    boolean offersIntegration() {
        return this != STATION_AUTOMATED;
    }

    List<StyledText> leadingLines() {
        return leadingKeys.stream().map(Translation::styled).toList();
    }

    List<DescriptionLine> trailingLines() {
        return trailingKeys.stream().map(key -> new DescriptionLine(Translation.styled(key), false)).toList();
    }
}
