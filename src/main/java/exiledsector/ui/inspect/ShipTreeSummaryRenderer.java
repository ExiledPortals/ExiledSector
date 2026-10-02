package exiledsector.ui.inspect;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Style;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.SkillTreeBonusSummary;
import exiledsector.skills.SkillTreeBonusSummary.Summary;
import exiledsector.skills.SkillType;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;

public final class ShipTreeSummaryRenderer {

    private static final float LINE_PAD = 3f;
    private static final float SECTION_PAD = 10f;
    private static final String BULLET = "    - ";

    private ShipTreeSummaryRenderer() {
    }

    public static void render(TooltipMakerAPI info, FleetMemberAPI member, ShipTreeLookup.ShipTree tree, float pad) {
        render(info, tree, member.getHullSpec().getHullSize(), pad, Integer.MAX_VALUE);
    }

    public static void render(TooltipMakerAPI info, ShipTreeLookup.ShipTree tree, HullSize hullSize, float pad, int maxBonusLines) {
        I18n.forGameText(() -> renderSummary(info, tree, hullSize, pad, maxBonusLines));
    }

    private static void renderSummary(TooltipMakerAPI info, ShipTreeLookup.ShipTree tree, HullSize hullSize, float pad,
                                      int maxBonusLines) {
        Summary summary = SkillTreeBonusSummary.of(tree.data(), hullSize);

        List<String> parts = new ArrayList<>();
        parts.add(Translation.msg("summary.level").arg("level", summary.level()).text());
        if (tree.layoutName() != null) {
            parts.add(Translation.msg("summary.build").arg("layout", tree.layoutName()).text());
        }
        if (summary.root() != null) {
            parts.add(Translation.msg("summary.start").arg("root", summary.root().getDisplayName()).text());
        }
        parts.add(Translation.msg("summary.nodes").count(summary.nodeCount()).text());
        info.addPara("%s", pad, Misc.getHighlightColor(), String.join(Translation.text("summary.separator"), parts));

        if (summary.notables().isEmpty()) {
            VanillaText.addPara(info, Translation.styled("summary.noNotables"), LINE_PAD, Misc.getGrayColor());
        } else {
            List<StyledText> names = new ArrayList<>();
            for (SkillType notable : summary.notables()) {
                names.add(StyledText.styled(notable.getDisplayName(), Style.HIGHLIGHT));
            }
            VanillaText.addPara(info, Translation.msg("summary.notables").arg("names", Translation.list(names)).styled(), LINE_PAD,
                    Misc.getTextColor());
        }

        if (!summary.bonuses().isEmpty()) {
            VanillaText.addPara(info, Translation.styled("summary.bonuses"), SECTION_PAD, Misc.getTextColor());
            List<DescriptionLine> bonuses = summary.bonuses();
            int shown = Math.min(bonuses.size(), maxBonusLines);
            for (DescriptionLine line : bonuses.subList(0, shown)) {
                VanillaText.addPara(info, StyledText.of(BULLET).append(line.display()), LINE_PAD, Misc.getTextColor());
            }
            if (shown < bonuses.size()) {
                VanillaText.addPara(info, Translation.msg("summary.more").count(bonuses.size() - shown).styled(), LINE_PAD,
                        Misc.getGrayColor());
            }
        }
    }
}
