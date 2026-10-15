package exiledsector.ui.inspect;

import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.impl.codex.CodexDialogAPI;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Message;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.socketables.PoolEntry;
import exiledsector.socketables.RolledEffect;
import exiledsector.socketables.SocketableDefinition;
import exiledsector.socketables.SocketableDrops;
import exiledsector.socketables.SocketableRarity;
import exiledsector.socketables.SocketableRoller;
import exiledsector.socketables.SocketableRollOdds;
import exiledsector.ui.VanillaText;

import java.util.List;
import java.util.Map;

public class SocketableCodexEntry extends CodexEntryV2 implements CustomUIPanelPlugin {

    private static final float RELATED_ENTRIES_WIDTH = 290f;
    private static final float BOX_HORIZONTAL_PAD = 30f;
    private static final float PAD = 10f;
    private static final float LINE_PAD = 3f;
    private static final float ICON_SIZE = 64f;

    private final SocketableDefinition definition;

    public SocketableCodexEntry(SocketableDefinition definition) {
        super(SocketableCodex.entryId(definition.id()), I18n.forGameText(definition::displayName), definition.icon(), definition);
        this.definition = definition;
    }

    @Override
    public void createTitleForList(TooltipMakerAPI info, float width, ListMode mode) {
        I18n.forGameText(() -> {
            info.addPara("%s", 0f, Misc.getBasePlayerColor(), Misc.getBasePlayerColor(), definition.displayName());
            info.addPara("%s", 0f, Misc.getGrayColor(), Misc.getGrayColor(), subtitle());
        });
    }

    private String subtitle() {
        String socketName = definition.kind().displayName();
        return definition.unique() ? Translation.msg("codex.socketables.uniqueSubtitle").arg("socket", socketName).text() : socketName;
    }

    @Override
    public ModSpecAPI getSourceMod() {
        return SocketableCodex.sourceMod();
    }

    @Override
    public boolean hasCustomDetailPanel() {
        return true;
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return this;
    }

    @Override
    public void createCustomDetail(CustomPanelAPI panel, UIPanelAPI relatedEntries, CodexDialogAPI codex) {
        float panelWidth = panel.getPosition().getWidth();
        float textWidth = panelWidth - RELATED_ENTRIES_WIDTH - PAD - BOX_HORIZONTAL_PAD + PAD;
        TooltipMakerAPI textElement = panel.createUIElement(textWidth, 0f, false);
        I18n.forGameText(() -> writeDetail(textElement));
        panel.updateUIElementSizeAndMakeItProcessInput(textElement);

        UIPanelAPI textBox = panel.wrapTooltipWithBox(textElement);
        panel.addComponent(textBox).inTL(0f, 0f);
        float panelHeight = textBox.getPosition().getHeight();
        if (relatedEntries != null) {
            panel.addComponent(relatedEntries).inTR(0f, 0f);
            panelHeight = Math.max(panelHeight, relatedEntries.getPosition().getHeight());
        }
        panel.getPosition().setSize(panelWidth, panelHeight);
    }

    private void writeDetail(TooltipMakerAPI info) {
        TooltipMakerAPI header = info.beginImageWithText(definition.icon(), ICON_SIZE);
        header.setTitleOrbitronLarge();
        header.addTitle(definition.displayName(), definition.unique() ? SocketableRarity.UNIQUE.color() : Misc.getBasePlayerColor());
        VanillaText.addPara(header, Translation.msg("codex.socketables.socket").arg("socket", definition.kind().displayName()).styled(),
                LINE_PAD, Misc.getTextColor());
        info.addImageWithText(0f);
        StyledText description = definition.descriptionText();
        if (description != null) {
            VanillaText.addPara(info, description, PAD, Misc.getGrayColor());
        }
        if (definition.unique()) {
            writeUnique(info);
        } else {
            writeAffixes(info);
        }
        if (definition.pool().stream().anyMatch(PoolEntry::scalesWithHullSize)) {
            VanillaText.addPara(info, Translation.styled("codex.socketables.hullValues"), PAD, Misc.getGrayColor());
        }
        String rollsKey = SocketableRoller.averageRolls() ? "codex.socketables.averageRolls" : "codex.socketables.randomRolls";
        VanillaText.addPara(info, Translation.styled(rollsKey), PAD, Misc.getGrayColor());
    }

    private void writeUnique(TooltipMakerAPI info) {
        info.addSectionHeading(Translation.text("codex.socketables.effects"), Alignment.MID, PAD);
        VanillaText.addPara(info, Translation.styled("codex.socketables.uniqueRolls"), PAD, Misc.getTextColor());
        float linePad = PAD;
        for (PoolEntry entry : definition.pool()) {
            StyledText effectText = effectText(entry);
            if (effectText != null) {
                VanillaText.addPara(info, effectText, linePad, Misc.getTextColor());
                linePad = LINE_PAD;
            }
        }
    }

    private void writeAffixes(TooltipMakerAPI info) {
        boolean synthesisable = SocketableDrops.canBeSynthesised(definition);
        info.addSectionHeading(Translation.text("codex.socketables.effectCount"), Alignment.MID, PAD);
        Message countMessage = Translation.msg("codex.socketables.effectCounts");
        for (int effectCount : SocketableRollOdds.EFFECT_COUNTS) {
            countMessage.arg("chance" + effectCount, percentText(SocketableRollOdds.effectCountChance(effectCount)));
        }
        VanillaText.addPara(info, countMessage.styled(), PAD, Misc.getTextColor());
        if (synthesisable) {
            VanillaText.addPara(info, Translation.styled("codex.socketables.synthesisedCount"), LINE_PAD, Misc.getTextColor());
        }
        Map<String, Double> foundChances = SocketableRollOdds.foundChances(definition);
        Map<String, Double> synthesisedChances = synthesisable ? SocketableRollOdds.synthesisedChances(definition) : Map.of();
        writePool(info, "codex.socketables.prefixes", definition.prefixes(), foundChances, synthesisedChances);
        writePool(info, "codex.socketables.suffixes", definition.suffixes(), foundChances, synthesisedChances);
    }

    private static void writePool(TooltipMakerAPI info, String headingKey, List<PoolEntry> pool, Map<String, Double> foundChances,
                                  Map<String, Double> synthesisedChances) {
        if (pool.isEmpty()) {
            return;
        }
        info.addSectionHeading(Translation.text(headingKey), Alignment.MID, PAD);
        float linePad = PAD;
        for (PoolEntry entry : pool) {
            StyledText effectText = effectText(entry);
            if (effectText == null) {
                continue;
            }
            String foundChance = percentText(foundChances.getOrDefault(entry.effectName(), 0.0));
            Double synthesisedChance = synthesisedChances.get(entry.effectName());
            Message line = synthesisedChance == null
                    ? Translation.msg("codex.socketables.effectChance").arg("chance", foundChance)
                    : Translation.msg("codex.socketables.effectChanceSynthesised").arg("chance", foundChance)
                            .arg("synthesised", percentText(synthesisedChance));
            VanillaText.addPara(info, line.arg("effect", effectText).styled(), linePad, Misc.getTextColor());
            linePad = LINE_PAD;
        }
    }

    private static StyledText effectText(PoolEntry entry) {
        return new RolledEffect(entry.effectName(), entry.boundFarthestFromZero()).rangeDescription(entry);
    }

    static String percentText(double chance) {
        if (chance <= 0.0) {
            return "0";
        }
        if (chance < 0.0005) {
            return Translation.text("codex.socketables.belowTenth");
        }
        if (chance >= 1.0 - 1e-9) {
            return "100";
        }
        double tenths = Math.min(99.9, Math.round(chance * 1000.0) / 10.0);
        return tenths == Math.rint(tenths) ? Long.toString((long) tenths) : Double.toString(tenths);
    }

    @Override
    public void destroyCustomDetail() {
    }

    @Override
    public void positionChanged(PositionAPI position) {
    }

    @Override
    public void renderBelow(float alphaMult) {
    }

    @Override
    public void render(float alphaMult) {
    }

    @Override
    public void advance(float amount) {
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
    }

    @Override
    public void buttonPressed(Object buttonId) {
    }
}
