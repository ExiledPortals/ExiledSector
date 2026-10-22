package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledlib.ModSettingsListener;
import exiledlib.items.ItemTypeSupport;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.effects.ShipEffectSource;
import exiledlib.i18n.Translation;
import exiledsector.loot.LootSource;
import exiledsector.skills.ShipSections;
import exiledsector.ui.ExiledSectorSettings;
import exiledsector.ui.HullUpgradesRefitButton;
import exiledsector.ui.cargo.ItemIconLooks;
import exiledsector.ui.inspect.ShipSummaryPart;
import exiledsector.ui.refit.RefitButtonColumn;
import exiledsector.ui.section.SectionIconLooks;
import exiledsector.ui.section.SectionShipSummary;
import lunalib.lunaSettings.LunaSettings.SettingsCreator;

import java.util.List;

import static exiledsector.ExiledSector.MOD_ID;

public final class HullUpgradesDomain implements ExiledSectorDomain {

    @Override
    public List<ItemTypeSupport> itemTypes() {
        return List.of(HullItem.SUPPORT);
    }

    @Override
    public List<ItemDataRules> itemDataRules() {
        return List.of(HullItemDataRules.INSTANCE);
    }

    @Override
    public List<ItemIconLooks> itemIconLooks() {
        return List.of(SectionIconLooks.INSTANCE);
    }

    @Override
    public List<RefitButtonColumn.ButtonSpec> refitButtons() {
        return List.of(HullUpgradesRefitButton.COLUMN_BUTTON);
    }

    @Override
    public List<ShipCustody> shipCustodies() {
        return List.of(SectionCustody.INSTANCE);
    }

    @Override
    public List<ShipEffectSource> shipEffectSources() {
        return List.of(HullUpgradeEffectSource.INSTANCE);
    }

    @Override
    public List<ShipSummaryPart> shipSummaryParts() {
        return List.of(SectionShipSummary.INSTANCE);
    }

    @Override
    public List<LootSource> lootSources() {
        return List.of(HullUpgradeLootSource.INSTANCE);
    }

    @Override
    public void onApplicationLoad() {
        HullUpgradesRefitButton.addButton();
    }

    @Override
    public void registerSettings() {
        SettingsCreator.addHeader(MOD_ID, "exiledSector_hullUpgradesHeader", Translation.text("settings.hullUpgrades.header"), ExiledSectorSettings.MAIN_TAB);
        SettingsCreator.addBoolean(MOD_ID, HullUpgradesSetting.ENABLED_FIELD_ID,
                Translation.text("settings.socketStorage.hullUpgrades.name"), Translation.text("settings.socketStorage.hullUpgrades.tooltip"),
                HullUpgradesSetting.DEFAULT_ENABLED, ExiledSectorSettings.MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, HullUpgradesSetting.UNLOCK_LEVEL_FIELD_ID,
                Translation.text("settings.socketStorage.hullUpgradesLevel.name"), Translation.text("settings.socketStorage.hullUpgradesLevel.tooltip"),
                HullUpgradesSetting.DEFAULT_UNLOCK_LEVEL, HullUpgradesSetting.MIN_UNLOCK_LEVEL, HullUpgradesSetting.MAX_UNLOCK_LEVEL,
                ExiledSectorSettings.MAIN_TAB);
        SettingsCreator.addInt(MOD_ID, BattleItemDrops.DROP_PERCENT_FIELD_ID,
                Translation.text("settings.socketStorage.battleItems.name"), Translation.text("settings.socketStorage.battleItems.tooltip"),
                BattleItemDrops.DEFAULT_DROP_PERCENT, 0, BattleItemDrops.MAX_DROP_PERCENT, ExiledSectorSettings.MAIN_TAB);
        ModSettingsListener.ensureRegistered(HullUpgradesSetting.SettingsListener.class, HullUpgradesSetting.SettingsListener::new);
    }

    @Override
    public void reloadSettings() {
        HullUpgradesSetting.reloadSettings();
    }

    @Override
    public void loadItems() {
        MarketHullItems.load();
        BattleItemDrops.load();
    }

    @Override
    public void onGameLoad(boolean newGame, List<CargoAPI> savedCargoes) {
        ShipSections.clearCache();
    }
}
