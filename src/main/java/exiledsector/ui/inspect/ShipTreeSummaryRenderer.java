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
import exiledsector.socketables.Socketable;
import exiledsector.socketables.SocketableStore;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;

public final class ShipTreeSummaryRenderer {

    private static final float LINE_PAD = 3f;
    private static final float SECTION_PAD = 10f;
    private static final String BULLET = "    - ";

    private ShipTreeSummaryRenderer() {
    }

    public static void render(TooltipMakerAPI info, FleetMemberAPI member, ShipTreeLookup.ShipTree shipTree, float pad) {
        render(info, shipTree, member.getHullSpec().getHullSize(), pad, Integer.MAX_VALUE);
    }

    public static void render(TooltipMakerAPI info, ShipTreeLookup.ShipTree shipTree, HullSize hullSize, float pad, int maxBonusLines) {
        I18n.forGameText(() -> renderSummary(info, shipTree, hullSize, pad, maxBonusLines));
    }

    private static void renderSummary(TooltipMakerAPI info, ShipTreeLookup.ShipTree shipTree, HullSize hullSize, float pad,
                                      int maxBonusLines) {
        Summary bonusSummary = SkillTreeBonusSummary.of(shipTree.skillData(), hullSize, shipTree.bonusScale());

        List<String> headerParts = new ArrayList<>();
        headerParts.add(Translation.msg("summary.level").arg("level", bonusSummary.level()).text());
        if (!shipTree.buildThemes().isEmpty()) {
            headerParts.add(Translation.msg("summary.build").arg("layout", NpcBuildLabel.name(shipTree.buildThemes())).text());
        }
        if (bonusSummary.root() != null) {
            headerParts.add(Translation.msg("summary.start").arg("root", bonusSummary.root().getDisplayName()).text());
        }
        headerParts.add(Translation.msg("summary.nodes").count(bonusSummary.nodeCount()).text());
        if (shipTree.bonusScale() > 1f) {
            headerParts.add(Translation.msg("summary.bonusScale").arg("scale", shipTree.bonusScale()).text());
        }
        info.addPara("%s", pad, Misc.getHighlightColor(), String.join(Translation.text("summary.separator"), headerParts));

        if (bonusSummary.notables().isEmpty()) {
            VanillaText.addPara(info, Translation.styled("summary.noNotables"), LINE_PAD, Misc.getGrayColor());
        } else {
            List<StyledText> notableNames = new ArrayList<>();
            for (SkillType notable : bonusSummary.notables()) {
                notableNames.add(StyledText.styled(notable.getDisplayName(), Style.HIGHLIGHT));
            }
            VanillaText.addPara(info, Translation.msg("summary.notables").arg("names", Translation.list(notableNames)).styled(), LINE_PAD,
                    Misc.getTextColor());
        }

        List<StyledText> socketedNames = new ArrayList<>();
        for (String socketableId : shipTree.skillData().getSocketedItems().values()) {
            Socketable socketable = SocketableStore.lookup(socketableId);
            if (socketable != null) {
                socketedNames.add(StyledText.styled(socketable.name(), Style.HIGHLIGHT));
            }
        }
        if (!socketedNames.isEmpty()) {
            VanillaText.addPara(info, Translation.msg("summary.sockets").arg("names", Translation.list(socketedNames)).styled(), LINE_PAD,
                    Misc.getTextColor());
        }

        if (!bonusSummary.bonuses().isEmpty()) {
            VanillaText.addPara(info, Translation.styled("summary.bonuses"), SECTION_PAD, Misc.getTextColor());
            List<DescriptionLine> bonuses = bonusSummary.bonuses();
            int shownBonusCount = Math.min(bonuses.size(), maxBonusLines);
            for (DescriptionLine line : bonuses.subList(0, shownBonusCount)) {
                VanillaText.addPara(info, StyledText.of(BULLET).append(line.display()), LINE_PAD, Misc.getTextColor());
            }
            if (shownBonusCount < bonuses.size()) {
                VanillaText.addPara(info, Translation.msg("summary.more").count(bonuses.size() - shownBonusCount).styled(), LINE_PAD,
                        Misc.getGrayColor());
            }
        }
    }
}
