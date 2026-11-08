package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class IntegrationLines {

    public record State(boolean integrated, int nextCost, boolean showsHint) {

        public static State of(ShipSkillData data, SkillNode node, boolean integrationAvailable) {
            boolean integrated = data.isIntegrated(node.getId());
            return new State(integrated, IntegratedNodes.nextCost(data), integrationAvailable && !data.isNpcBuild() && data.isAllocated(node.getId()));
        }
    }

    private IntegrationLines() {
    }

    public static NodeDescription describe(SkillType type, HullSize hullSize, State state) {
        if (!type.isIntegrable()) {
            return SkillNode.describeMechanics(type, hullSize);
        }
        return state.integrated() ? integrated(type, hullSize) : withHint(type, hullSize, state);
    }

    public static List<DescriptionLine> preview(SkillType type, HullSize hullSize, State state) {
        if (!type.isIntegrable() || state.integrated()) {
            return List.of();
        }
        List<DescriptionLine> lines = new ArrayList<>();
        lines.add(muted(Translation.msg("node.integration.preview").count(state.nextCost()).text()));
        for (DescriptionLine line : SkillNode.describeEffects(type, hullSize, true)) {
            lines.add(muted(line.plain()));
        }
        return lines;
    }

    static NodeDescription integrated(SkillType type, HullSize hullSize) {
        NodeDescription integratedDescription = SkillNode.describeMechanics(type, hullSize, true);
        Set<String> normalLines = new HashSet<>();
        SkillNode.describeEffects(type, hullSize, false).forEach(line -> normalLines.add(line.plain()));
        List<DescriptionLine> effects = new ArrayList<>();
        effects.add(new DescriptionLine(Translation.styled("node.integration.integrated"), false));
        for (DescriptionLine line : integratedDescription.effects()) {
            effects.add(normalLines.contains(line.plain()) ? line : new DescriptionLine(line.text().withUnstyledAs(Style.BUILT_IN), line.lowerIsBetter()));
        }
        return new NodeDescription(effects, integratedDescription.details());
    }

    static NodeDescription withHint(SkillType type, HullSize hullSize, State state) {
        NodeDescription normal = SkillNode.describeMechanics(type, hullSize, false);
        List<DescriptionLine> effects = new ArrayList<>(normal.effects());
        if (state.showsHint()) {
            effects.add(new DescriptionLine(Translation.styled("node.integration.hint"), false));
        }
        return new NodeDescription(effects, normal.details());
    }

    private static DescriptionLine muted(String plain) {
        return new DescriptionLine(StyledText.styled(plain, Style.MUTED), false);
    }
}
