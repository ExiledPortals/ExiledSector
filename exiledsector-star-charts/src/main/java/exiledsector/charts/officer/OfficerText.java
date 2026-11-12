package exiledsector.charts.officer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import exiledsector.charts.atlas.AtlasCell;
import exiledsector.charts.atlas.AtlasGenerator;
import exiledsector.charts.atlas.AtlasLine;
import exiledsector.charts.atlas.AtlasNames;
import exiledsector.charts.atlas.AtlasTerritoryPlacer;
import exiledsector.charts.officer.reward.OfficerRewardText;
import exiledsector.charts.officer.scar.ScarText;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public final class OfficerText {

    static final String HISTORY_KEY_PREFIX = "chart.officer.history.";
    private static final Map<String, String> FULL_NAMES = new HashMap<>();

    private OfficerText() {
    }

    static PersonAPI namedPerson(FactionAPI faction, AtlasOfficer officer) {
        return faction.createRandomPerson(new Random(officer.nameSeed()));
    }

    public static String fullName(AtlasOfficer officer) {
        SectorAPI sector = Global.getSector();
        FactionAPI faction = sector == null ? null : sector.getFaction(officer.factionId());
        if (faction == null) {
            return "";
        }
        return FULL_NAMES.computeIfAbsent(officer.factionId() + "|" + officer.nameSeed(), key -> namedPerson(faction, officer).getNameString());
    }

    public static String title(AtlasOfficer officer) {
        return title(OfficerRanks.rank(officer.rankId()).displayName(), ScarText.epithet(fullName(officer), new TreeMap<>(officer.scars())));
    }

    public static List<AtlasLine> detailLines(AtlasOfficer officer) {
        List<AtlasLine> lines = new ArrayList<>();
        AtlasLine carriedLine = OfficerRewardText.carriedLine(officer.carried());
        if (carriedLine != null) {
            lines.add(carriedLine);
        }
        for (StyledText scarLine : ScarText.tooltipLines(new TreeMap<>(officer.scars()), OfficerFleets.flagshipHull(officer))) {
            lines.add(new AtlasLine(scarLine.toMarkup(), AtlasLine.Tone.STYLED));
        }
        return lines;
    }

    static String title(String rankName, String fullName) {
        return Translation.msg("chart.officer.title").arg("rank", rankName).arg("name", fullName).text().trim();
    }

    public static HistoryEntry latestEntry(AtlasOfficer officer) {
        return officer.history().isEmpty() ? null : HistoryEntry.parse(officer.history().get(officer.history().size() - 1));
    }

    public static String latestHistory(AtlasGenerator generator, AtlasOfficer officer) {
        return historyLine(generator, latestEntry(officer));
    }

    public static String historyLine(AtlasGenerator generator, HistoryEntry entry) {
        if (entry == null || !Translation.has(HISTORY_KEY_PREFIX + entry.event())) {
            return "";
        }
        return Translation.msg(HISTORY_KEY_PREFIX + entry.event()).arg("region", regionOf(generator, entry.latest()))
                .arg("from", regionOf(generator, entry.earliest())).text();
    }

    public static String regionOf(AtlasGenerator generator, AtlasCell cell) {
        return AtlasNames.region(generator.node(cell).regionId());
    }

    public record HistoryEntry(String event, AtlasCell earliest, AtlasCell latest) {

        public static HistoryEntry parse(String entry) {
            String[] parts = entry == null ? new String[0] : entry.split(AtlasTerritoryPlacer.HISTORY_SEPARATOR, -1);
            if (parts.length < 2 || parts[0].isEmpty()) {
                return null;
            }
            try {
                return new HistoryEntry(parts[0], AtlasCell.unpack(Long.parseLong(parts[1])), AtlasCell.unpack(Long.parseLong(parts[parts.length - 1])));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
