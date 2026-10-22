package exiledsector.ui.inspect;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.effects.ModuleShareSetting;
import exiledsector.effects.SkillTreeHullMod;
import exiledlib.i18n.I18n;
import exiledlib.i18n.Style;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledlib.skills.DescriptionLine;
import exiledlib.skills.DiminishedStat;
import exiledlib.skills.DiminishingReturns;
import exiledlib.skills.EffectContribution;
import exiledlib.skills.ShipFacts;
import exiledsector.skills.SkillTreeBonusSummary;
import exiledsector.skills.SkillTreeBonusSummary.Summary;
import exiledsector.skills.SkillType;
import exiledlib.items.Item;
import exiledlib.items.ItemStore;
import exiledlib.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;

public final class ShipTreeSummaryRenderer {

    private static final float LINE_PAD = 3f;
    private static final float SECTION_PAD = 10f;
    private static final String BULLET = "    - ";
    private static final float HULL_MOD_TOOLTIP_PAD = 10f;
    private static final int MAX_HULL_MOD_BONUS_LINES = 25;

    private ShipTreeSummaryRenderer() {
    }

    public static void renderForCoreHullMod(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship) {
        ShipTreeLookup.ShipTree shipTree = ShipTreeLookup.forShip(SkillTreeHullMod.memberOf(ship), ship.getVariant());
        if (shipTree == null) {
            I18n.forGameText(() -> VanillaText.addPara(tooltip, Translation.styled("hullmod.exiledSector_core.noTree"), HULL_MOD_TOOLTIP_PAD,
                    Misc.getGrayColor()));
            return;
        }
        boolean hasModuleSlots = ship.getHullSpec() != null && ShipFacts.hasModuleSlots(ship.getHullSpec());
        render(tooltip, shipTree, hullSize, hasModuleSlots, HULL_MOD_TOOLTIP_PAD, MAX_HULL_MOD_BONUS_LINES);
    }

    public static void render(TooltipMakerAPI info, FleetMemberAPI member, ShipTreeLookup.ShipTree shipTree, float pad) {
        render(info, shipTree, member.getHullSpec().getHullSize(), ShipFacts.hasModuleSlots(member.getHullSpec()), pad, Integer.MAX_VALUE);
    }

    public static void render(TooltipMakerAPI info, ShipTreeLookup.ShipTree shipTree, HullSize hullSize, boolean hasModuleSlots, float pad,
                              int maxBonusLines) {
        I18n.forGameText(() -> renderSummary(info, shipTree, hullSize, hasModuleSlots, pad, maxBonusLines));
    }

    private static void renderSummary(TooltipMakerAPI info, ShipTreeLookup.ShipTree shipTree, HullSize hullSize, boolean hasModuleSlots,
                                      float pad, int maxBonusLines) {
        Summary bonusSummary = SkillTreeBonusSummary.of(shipTree.skillData(), hullSize, shipTree.bonusScale(),
                partContributions(shipTree, hullSize),
                shipTree.statBaseline(), DiminishingReturns.current());

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
        VanillaText.plainPara(info, String.join(Translation.text("summary.separator"), headerParts), pad, Misc.getHighlightColor());

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
        for (String itemId : shipTree.skillData().getSocketedItems().values()) {
            Item item = ItemStore.lookup(itemId);
            if (item != null) {
                socketedNames.add(StyledText.styled(item.name(), Style.HIGHLIGHT));
            }
        }
        if (!socketedNames.isEmpty()) {
            VanillaText.addPara(info, Translation.msg("summary.sockets").arg("names", Translation.list(socketedNames)).styled(), LINE_PAD,
                    Misc.getTextColor());
        }
        renderPartLines(info, shipTree);
        renderModuleShare(info, hasModuleSlots);

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
        if (!bonusSummary.diminishedStats().isEmpty()) {
            List<StyledText> statNames = new ArrayList<>();
            for (DiminishedStat stat : bonusSummary.diminishedStats()) {
                statNames.add(Translation.styled(stat.translationKey()));
            }
            VanillaText.addPara(info, Translation.msg("summary.diminished").arg("stats", Translation.list(statNames)).styled(), LINE_PAD,
                    Misc.getGrayColor());
        }
    }

    private static void renderModuleShare(TooltipMakerAPI info, boolean hasModuleSlots) {
        if (!hasModuleSlots) {
            return;
        }
        int moduleSharePercent = ModuleShareSetting.percent();
        if (moduleSharePercent > 0) {
            VanillaText.addPara(info, Translation.msg("summary.moduleShare").arg("percent", moduleSharePercent).styled(), LINE_PAD,
                    Misc.getTextColor());
        }
    }

    private static List<EffectContribution> partContributions(ShipTreeLookup.ShipTree shipTree, HullSize hullSize) {
        List<EffectContribution> contributions = new ArrayList<>();
        for (ShipSummaryPart part : ShipSummaryParts.all()) {
            contributions.addAll(part.contributions(shipTree.skillData(), shipTree.variant(), hullSize));
        }
        return contributions;
    }

    private static void renderPartLines(TooltipMakerAPI info, ShipTreeLookup.ShipTree shipTree) {
        for (ShipSummaryPart part : ShipSummaryParts.all()) {
            StyledText line = part.line(shipTree.skillData(), shipTree.variant());
            if (line != null) {
                VanillaText.addPara(info, line, LINE_PAD, Misc.getTextColor());
            }
        }
    }
}
