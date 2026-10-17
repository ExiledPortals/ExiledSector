package exiledsector.ui.inspect;

import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.NumberText;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.hullitems.HullItemImplicits;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.hullitems.PoolEntry;
import exiledsector.hullitems.RolledEffect;
import exiledsector.hullitems.HullItemDefinition;
import exiledsector.hullitems.HullItemRarity;
import exiledsector.hullitems.HullItemRollOdds;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.Map;

public class HullItemCodexEntry extends BoxedCodexEntry {

    private static final float LINE_PAD = 3f;
    private static final float ICON_SIZE = 64f;
    private static final float CHANCE_COLUMN_WIDTH = 60f;
    private static final float COLUMN_GAP = 10f;

    private final HullItemDefinition definition;

    public HullItemCodexEntry(HullItemDefinition definition) {
        super(HullItemCodex.entryId(definition.id()), I18n.forGameText(definition::displayName), definition.icon(), definition);
        this.definition = definition;
    }

    @Override
    public void createTitleForList(TooltipMakerAPI info, float width, ListMode mode) {
        I18n.forGameText(() -> {
            VanillaText.plainPara(info, definition.displayName(), 0f, Misc.getBasePlayerColor());
            VanillaText.plainPara(info, subtitle(), 0f, Misc.getGrayColor());
        });
    }

    private String subtitle() {
        String socketName = definition.kind().displayName();
        return definition.unique() ? Translation.msg("codex.socketables.uniqueSubtitle").arg("socket", socketName).text() : socketName;
    }

    @Override
    public boolean isVisible() {
        return HullItemCodex.isRevealed(definition);
    }

    @Override
    public ModSpecAPI getSourceMod() {
        return HullItemCodex.sourceMod();
    }

    @Override
    protected void writeDetail(TooltipMakerAPI info, float textWidth) {
        I18n.forGameText(() -> writeDefinition(info, textWidth));
    }

    private void writeDefinition(TooltipMakerAPI info, float textWidth) {
        TooltipMakerAPI header = info.beginImageWithText(definition.icon(), ICON_SIZE);
        header.setTitleOrbitronLarge();
        header.addTitle(definition.displayName(), definition.unique() ? HullItemRarity.UNIQUE.color() : Misc.getBasePlayerColor());
        VanillaText.addPara(header, Translation.msg("codex.socketables.socket").arg("socket", definition.kind().displayName()).styled(),
                LINE_PAD, Misc.getTextColor());
        info.addImageWithText(0f);
        StyledText description = definition.descriptionText();
        if (description != null) {
            VanillaText.addPara(info, description, PAD, Misc.getGrayColor());
        }
        writeImplicits(info);
        if (definition.unique()) {
            writeUnique(info);
        } else {
            writeAffixes(info, textWidth);
        }
    }

    private void writeImplicits(TooltipMakerAPI info) {
        List<Modifier> implicits = definition.kind().implicits();
        if (implicits.isEmpty()) {
            return;
        }
        info.addSectionHeading(Translation.text("hullItem.tooltip.implicit"), Alignment.MID, PAD);
        float linePad = PAD;
        for (Modifier implicit : implicits) {
            StyledText effectText = effectText(implicit.line());
            if (effectText != null) {
                VanillaText.addPara(info, effectText, linePad, Misc.getTextColor());
                linePad = LINE_PAD;
            }
        }
    }

    private void writeUnique(TooltipMakerAPI info) {
        info.addSectionHeading(Translation.text("codex.socketables.effects"), Alignment.MID, PAD);
        float linePad = PAD;
        for (PoolEntry entry : definition.pool()) {
            StyledText effectText = effectText(entry);
            if (effectText != null) {
                VanillaText.addPara(info, effectText, linePad, Misc.getTextColor());
                linePad = LINE_PAD;
            }
        }
    }

    private void writeAffixes(TooltipMakerAPI info, float textWidth) {
        Map<String, Double> weightShares = HullItemRollOdds.weightShares(definition);
        writePool(info, "codex.socketables.prefixes", definition.prefixes(), weightShares, textWidth);
        writePool(info, "codex.socketables.suffixes", definition.suffixes(), weightShares, textWidth);
    }

    private static void writePool(TooltipMakerAPI info, String headingKey, List<PoolEntry> pool, Map<String, Double> weightShares,
                                  float textWidth) {
        if (pool.isEmpty()) {
            return;
        }
        info.addSectionHeading(Translation.text(headingKey), Alignment.MID, PAD);
        float effectColumnWidth = textWidth - CHANCE_COLUMN_WIDTH - COLUMN_GAP;
        float linePad = PAD;
        for (PoolEntry entry : pool) {
            StyledText effectText = effectText(entry);
            if (effectText == null) {
                continue;
            }
            String chance = percentText(weightShares.getOrDefault(entry.effectName(), 0.0));
            TooltipMakerAPI chanceCell = info.beginSubTooltip(CHANCE_COLUMN_WIDTH);
            VanillaText.addPara(chanceCell, Translation.msg("codex.socketables.chance").arg("chance", chance).styled(), 0f, Misc.getTextColor())
                    .setAlignment(Alignment.RMID);
            info.endSubTooltip();
            TooltipMakerAPI effectCell = info.beginSubTooltip(effectColumnWidth);
            VanillaText.addPara(effectCell, effectText, 0f, Misc.getTextColor());
            info.endSubTooltip();
            info.addCustom(chanceCell, linePad);
            info.addCustomDoNotSetPosition(effectCell).getPosition().rightOfTop(chanceCell, COLUMN_GAP);
            float extraEffectHeight = effectCell.getPosition().getHeight() - chanceCell.getPosition().getHeight();
            if (extraEffectHeight > 0f) {
                info.addSpacer(extraEffectHeight);
            }
            linePad = LINE_PAD;
        }
    }

    private static StyledText effectText(PoolEntry entry) {
        return effectText(entry.line());
    }

    private static StyledText effectText(ModifierLine line) {
        return new RolledEffect(line.effectName(), line.boundFarthestFromZero()).rangeDescription(line);
    }

    static String percentText(double chance) {
        if (chance <= 0.0) {
            return "0";
        }
        if (chance >= 1.0 - 1e-9) {
            return "100";
        }
        double hundredths = Math.min(99.9, Math.max(0.01, Math.round(chance * 10000.0) / 100.0));
        double shown = hundredths < 1.0 ? hundredths : Math.round(hundredths * 10.0) / 10.0;
        return NumberText.format((float) shown);
    }
}
