package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.ModSettingsListener;
import exiledsector.domain.Domain;
import exiledsector.i18n.Translation;
import exiledsector.skills.ShipSections;
import exiledsector.ui.ExiledSectorSettings;
import exiledsector.ui.HullUpgradesRefitButton;
import lunalib.lunaSettings.LunaSettings.SettingsCreator;

import java.util.List;

import static exiledsector.ExiledSectorModPlugin.MOD_ID;

public final class HullUpgradesDomain implements Domain {

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
        ModSettingsListener.ensureRegistered(HullUpgradesSetting.SettingsListener.class, HullUpgradesSetting.SettingsListener::new);
    }

    @Override
    public void reloadSettings() {
        HullUpgradesSetting.reloadSettings();
    }

    @Override
    public void loadItems() {
        MarketHullItems.load();
    }

    @Override
    public void onGameLoad(boolean newGame, List<CargoAPI> savedCargoes) {
        ShipSections.clearCache();
    }

    @Override
    public void afterLootListeners() {
        SocketTypeMigration.run();
    }
}
