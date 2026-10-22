package exiledsector.ui.section;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import exiledlib.i18n.Style;
import exiledlib.i18n.StyledText;
import exiledlib.i18n.Translation;
import exiledsector.items.HullUpgradeEffectSource;
import exiledsector.items.Item;
import exiledlib.skills.EffectContribution;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
import exiledsector.ui.inspect.ShipSummaryPart;

import java.util.ArrayList;
import java.util.List;

public final class SectionShipSummary implements ShipSummaryPart {

    public static final SectionShipSummary INSTANCE = new SectionShipSummary();

    private SectionShipSummary() {
    }

    @Override
    public List<EffectContribution> contributions(ShipSkillData shipData, ShipVariantAPI variant, HullSize hullSize) {
        return HullUpgradeEffectSource.contributions(shipData, sections(shipData, variant), hullSize);
    }

    @Override
    public StyledText line(ShipSkillData shipData, ShipVariantAPI variant) {
        int hullUpgradePoints = shipData.getHullUpgradePoints();
        if (hullUpgradePoints <= 0) {
            return null;
        }
        List<StyledText> socketTexts = new ArrayList<>();
        for (ShipSections.UnlockedSection slot : sections(shipData, variant)) {
            socketTexts.add(sectionText(slot));
        }
        StyledText sockets = socketTexts.isEmpty() ? Translation.styled("summary.frameworkNone") : Translation.list(socketTexts);
        return Translation.msg("summary.framework").arg("used", shipData.getUnlockedSocketTypeIds().size())
                .arg("points", hullUpgradePoints).arg("sockets", sockets).styled();
    }

    @Override
    public List<Item> installedItems(ShipSkillData shipData, ShipVariantAPI variant) {
        List<Item> installed = new ArrayList<>();
        for (ShipSections.UnlockedSection slot : sections(shipData, variant)) {
            if (slot.item() != null) {
                installed.add(slot.item());
            }
        }
        return installed;
    }

    private static List<ShipSections.UnlockedSection> sections(ShipSkillData shipData, ShipVariantAPI variant) {
        return variant == null ? List.of() : ShipSections.forVariant(shipData, variant);
    }

    private static StyledText sectionText(ShipSections.UnlockedSection slot) {
        if (!slot.active()) {
            return Translation.msg("summary.frameworkSocket.inactive").arg("type", slot.section().displayName()).styled();
        }
        if (slot.item() == null) {
            return StyledText.of(slot.section().displayName());
        }
        return Translation.msg("summary.frameworkSocket.filled").arg("type", slot.section().displayName())
                .arg("item", StyledText.styled(slot.item().name(), Style.HIGHLIGHT)).styled();
    }
}
