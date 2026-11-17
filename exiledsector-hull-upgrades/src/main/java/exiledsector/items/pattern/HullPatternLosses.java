package exiledsector.items.pattern;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.Misc;
import exiledsector.i18n.I18n;
import exiledsector.i18n.Translation;
import exiledsector.items.Item;
import exiledsector.items.ItemStore;
import exiledsector.items.hull.HullUpgradesSetting;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
import exiledsector.ui.CampaignMessages;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public final class HullPatternLosses {

    private HullPatternLosses() {
    }

    public static void patternShipLeft(HullPattern pattern) {
        HullPatternRules.delete(pattern);
        String hullName = HullTypes.displayName(pattern.hullType());
        message(() -> Translation.msg("pattern.message.removed").arg("hull", hullName).text());
    }

    public static Item followerDestroyed(HullPattern pattern, String followerId, ShipSkillData followerData, ItemStore store, Random random) {
        Item lostItem = HullUpgradesSetting.enabled() ? destroyClonedItem(pattern, followerId, followerData, store, random) : null;
        HullPatternRules.unlink(pattern, followerId);
        return lostItem;
    }

    static Item destroyClonedItem(HullPattern pattern, String followerId, ShipSkillData followerData, ItemStore store, Random random) {
        HullPatternClones.Source source = followerData == null ? null : HullPatternClones.activeSource(pattern);
        if (source == null) {
            return null;
        }
        List<ShipSections.UnlockedSection> clonedSections = ShipSections.of(followerData, null).stream()
                .filter(unlocked -> unlocked.cloned() && unlocked.appliesItem()).toList();
        if (clonedSections.isEmpty()) {
            return null;
        }
        ShipSections.UnlockedSection lost = clonedSections.get(random.nextInt(clonedSections.size()));
        Item lostItem = lost.item();
        source.patternData().removeFromSection(lost.section().id());
        store.remove(lostItem);
        FleetMemberAPI patternMember = HullPatternActivity.activePatternMember(pattern);
        if (patternMember != null) {
            patternMember.setStatUpdateNeeded(true);
        }
        HullPatternActivity.markFollowersForStatUpdate(pattern);
        HullPatterns.markChanged();
        String patternShipName = patternMember == null ? HullTypes.displayName(pattern.hullType()) : patternMember.getShipName();
        String followerName = String.valueOf(pattern.followerName(followerId));
        message(() -> Translation.msg("pattern.message.itemLost").arg("item", lostItem.name()).arg("ship", patternShipName)
                .arg("follower", followerName).text());
        return lostItem;
    }

    private static void message(Supplier<String> text) {
        if (CampaignMessages.available()) {
            CampaignMessages.plain(I18n.forGameText(text), Misc.getNegativeHighlightColor());
        }
    }
}
