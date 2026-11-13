package exiledsector.charts.atlas;

import com.fs.starfarer.api.impl.campaign.ids.Factions;
import exiledsector.charts.officer.AtlasOfficer;
import exiledsector.charts.officer.OfficerText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

public final class OfficerPortraits {

    interface PortraitSource {

        String seeded(AtlasOfficer officer);

        List<String> options(String factionId, AtlasOfficer officer);
    }

    private static final PortraitSource GAME = new PortraitSource() {

        @Override
        public String seeded(AtlasOfficer officer) {
            return OfficerText.seededPortraitPath(officer);
        }

        @Override
        public List<String> options(String factionId, AtlasOfficer officer) {
            return OfficerText.portraitOptions(factionId, officer);
        }
    };

    private OfficerPortraits() {
    }

    public static boolean assign(AtlasState state, TerritoryContext context) {
        return assign(state, context, GAME);
    }

    static boolean assign(AtlasState state, TerritoryContext context, PortraitSource source) {
        List<AtlasOfficer> officers = state.officers().stream().sorted(Comparator.comparingInt(AtlasOfficer::recordId)).toList();
        Set<String> used = new HashSet<>();
        officers.stream().filter(AtlasOfficer::hasPortrait).forEach(officer -> used.add(officer.portraitPath()));
        List<AtlasOfficer> unassigned = officers.stream().filter(officer -> !officer.hasPortrait()).toList();
        boolean changed = false;
        for (AtlasOfficer officer : unassigned) {
            if (context.takesPartInOpenRun(state, officer)) {
                changed |= give(state, officer, source.seeded(officer), used);
            }
        }
        for (AtlasOfficer officer : unassigned) {
            if (!context.takesPartInOpenRun(state, officer)) {
                changed |= give(state, officer, pick(officer, used, source), used);
            }
        }
        return changed;
    }

    private static boolean give(AtlasState state, AtlasOfficer officer, String portraitPath, Set<String> used) {
        if (portraitPath == null || portraitPath.isEmpty()) {
            return false;
        }
        AtlasOfficer portrayed = officer.withPortrait(portraitPath);
        if (!portrayed.hasPortrait()) {
            return false;
        }
        state.putOfficer(portrayed);
        used.add(portraitPath);
        return true;
    }

    private static String pick(AtlasOfficer officer, Set<String> used, PortraitSource source) {
        String seeded = source.seeded(officer);
        if (seeded == null || seeded.isEmpty() || !used.contains(seeded)) {
            return seeded;
        }
        Random random = new Random(officer.portraitSeed());
        for (String factionId : Stream.of(officer.factionId(), Factions.INDEPENDENT).distinct().toList()) {
            List<String> options = new ArrayList<>(source.options(factionId, officer));
            Collections.shuffle(options, random);
            for (String option : options) {
                if (!used.contains(option)) {
                    return option;
                }
            }
        }
        return seeded;
    }
}
