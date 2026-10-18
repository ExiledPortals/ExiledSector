package exiledsector.ui.inspect;

import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.NumberText;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.items.ItemImplicits;
import exiledsector.modifiers.Modifier;
import exiledsector.modifiers.ModifierLine;
import exiledsector.items.PoolEntry;
import exiledsector.items.RolledEffect;
import exiledsector.items.ItemDefinition;
import exiledsector.items.ItemEffects;
import exiledsector.items.ItemRarity;
import exiledsector.items.ItemRollOdds;
import exiledsector.ui.VanillaText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemCodexEntry extends BoxedCodexEntry {

    private static final float LINE_PAD = 3f;
    private static final float ICON_SIZE = 64f;
    private static final float CHANCE_COLUMN_WIDTH = 60f;
    private static final float COLUMN_GAP = 10f;

    private final ItemDefinition definition;

    public ItemCodexEntry(ItemDefinition definition) {
        super(ItemCodex.entryId(definition.id()), I18n.forGameText(definition::displayName), definition.icon(), definition);
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
        return ItemCodex.isRevealed(definition);
    }

    @Override
    public ModSpecAPI getSourceMod() {
        return ItemCodex.sourceMod();
    }

    @Override
    protected void writeDetail(TooltipMakerAPI info, float textWidth) {
        I18n.forGameText(() -> writeDefinition(info, textWidth));
    }

    private void writeDefinition(TooltipMakerAPI info, float textWidth) {
        TooltipMakerAPI header = info.beginImageWithText(definition.icon(), ICON_SIZE);
        header.setTitleOrbitronLarge();
        header.addTitle(definition.displayName(), definition.unique() ? ItemRarity.UNIQUE.color() : Misc.getBasePlayerColor());
        String installsInKey = definition.kind().fitsSection() ? "codex.hullItems.section" : "codex.socketables.socket";
        VanillaText.addPara(header, Translation.msg(installsInKey).arg("socket", definition.kind().displayName()).styled(), LINE_PAD, Misc.getTextColor());
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
            for (StyledText effectText : effectTexts(implicit)) {
                VanillaText.addPara(info, effectText, linePad, Misc.getTextColor());
                linePad = LINE_PAD;
            }
        }
    }

    private void writeUnique(TooltipMakerAPI info) {
        info.addSectionHeading(Translation.text("codex.socketables.effects"), Alignment.MID, PAD);
        float linePad = PAD;
        for (PoolEntry entry : definition.pool()) {
            for (StyledText effectText : effectTexts(entry.modifier())) {
                VanillaText.addPara(info, effectText, linePad, Misc.getTextColor());
                linePad = LINE_PAD;
            }
        }
    }

    private void writeAffixes(TooltipMakerAPI info, float textWidth) {
        Map<String, Double> weightShares = ItemRollOdds.weightShares(definition);
        writePool(info, "codex.socketables.prefixes", definition.prefixes(), weightShares, textWidth);
        writePool(info, "codex.socketables.suffixes", definition.suffixes(), weightShares, textWidth);
    }

    private void writePool(TooltipMakerAPI info, String headingKey, List<PoolEntry> pool, Map<String, Double> weightShares,
                           float textWidth) {
        if (pool.isEmpty()) {
            return;
        }
        info.addSectionHeading(Translation.text(headingKey), Alignment.MID, PAD);
        float effectColumnWidth = textWidth - CHANCE_COLUMN_WIDTH - COLUMN_GAP;
        float linePad = PAD;
        for (PoolEntry entry : pool) {
            List<StyledText> effectTexts = effectTexts(entry.modifier());
            if (effectTexts.isEmpty()) {
                continue;
            }
            String chance = percentText(weightShares.getOrDefault(entry.modifier().id(), 0.0));
            TooltipMakerAPI chanceCell = info.beginSubTooltip(CHANCE_COLUMN_WIDTH);
            VanillaText.addPara(chanceCell, Translation.msg("codex.socketables.chance").arg("chance", chance).styled(), 0f, Misc.getTextColor())
                    .setAlignment(Alignment.RMID);
            info.endSubTooltip();
            TooltipMakerAPI effectCell = info.beginSubTooltip(effectColumnWidth);
            float effectLinePad = 0f;
            for (StyledText effectText : effectTexts) {
                VanillaText.addPara(effectCell, effectText, effectLinePad, Misc.getTextColor());
                effectLinePad = LINE_PAD;
            }
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

    private List<StyledText> effectTexts(Modifier modifier) {
        List<StyledText> effectTexts = new ArrayList<>(modifier.lines().size());
        for (ModifierLine line : modifier.lines()) {
            StyledText effectText = effectText(line);
            if (effectText != null) {
                effectTexts.add(effectText);
            }
        }
        return effectTexts;
    }

    private StyledText effectText(ModifierLine line) {
        return new RolledEffect(line.effectName(), line.boundFarthestFromZero()).rangeDescription(ItemEffects.forType(definition.type()), line);
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
