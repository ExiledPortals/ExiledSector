package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import exiledsector.domain.ExiledSectorDomain;
import exiledsector.effects.ShipEffectSource;
import exiledsector.i18n.Translation;
import exiledsector.skills.SocketedItem;
import exiledsector.skills.skilleffect.special.SlotItemEffect;
import exiledsector.loot.LootSource;
import exiledsector.loot.MarketStock;
import exiledsector.skills.ShipSections;
import exiledsector.ui.ExiledSectorSettings;
import exiledsector.ui.HullItemsRefitButton;
import exiledsector.ui.cargo.ItemIconLooks;
import exiledsector.ui.inspect.ShipSummaryPart;
import exiledsector.ui.refit.RefitButtonColumn;
import exiledsector.ui.section.SectionIconLooks;
import exiledsector.ui.section.SectionShipSummary;
import lunalib.lunaSettings.LunaSettings.SettingsCreator;

import java.util.List;

import static exiledsector.ExiledSector.MOD_ID;

public final class HullUpgradesDomain implements ExiledSectorDomain {

    public HullUpgradesDomain() {
        ShipSection.all().forEach(section -> SlotItemEffect.register(SocketedItem.HULL_ITEM, section.id(), section::displayName));
        ItemStore.absorbStacksWith(RefitSchematic.STACK_ABSORBER);
        ItemStore.absorbStacksWith(MasterSchematic.STACK_ABSORBER);
    }

    @Override
    public boolean enabled() {
        return HullUpgradesSetting.enabled();
    }

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
        return List.of(HullItemsRefitButton.COLUMN_BUTTON);
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
    public List<MarketStock> marketStocks() {
        return List.of(HullUpgradeLootSource.INSTANCE);
    }

    @Override
    public void onApplicationLoad() {
        HullUpgradePoints.install();
        MasterSchematic.requiredWhen(HullUpgradesSetting::masterSchematicsRequired);
        HullItemsRefitButton.addButton();
    }

    @Override
    public void registerSettings() {
        String itemsTab = ExiledSectorSettings.itemsTab();
        HullUpgradesSetting.SWITCH.addSettings("exiledSector_hullUpgradesHeader", "settings.hullUpgrades.header",
                "settings.hullItems.hullUpgrades.name", "settings.hullItems.hullUpgrades.tooltip", itemsTab);
        SettingsCreator.addInt(MOD_ID, HullUpgradesSetting.UNLOCK_LEVEL_FIELD_ID,
                Translation.text("settings.hullItems.dropLevel.name"), Translation.text("settings.hullItems.dropLevel.tooltip"),
                HullUpgradesSetting.DEFAULT_UNLOCK_LEVEL, HullUpgradesSetting.MIN_UNLOCK_LEVEL, HullUpgradesSetting.MAX_UNLOCK_LEVEL, itemsTab);
        SettingsCreator.addRadio(MOD_ID, HullUpgradesSetting.POINTS_FIELD_ID,
                Translation.text("settings.hullItems.hullUpgradePoints.name"), Translation.text("settings.hullItems.hullUpgradePoints.tooltip"),
                HullUpgradesSetting.REFIT_SCHEMATICS, String.join(",", HullUpgradesSetting.POINTS_OPTIONS), itemsTab);
        SettingsCreator.addBoolean(MOD_ID, HullUpgradesSetting.MASTER_SCHEMATICS_FIELD_ID,
                Translation.text("settings.hullItems.masterSchematics.name"), Translation.text("settings.hullItems.masterSchematics.tooltip"),
                HullUpgradesSetting.DEFAULT_MASTER_SCHEMATICS_REQUIRED, itemsTab);
        SettingsCreator.addInt(MOD_ID, BattleItemDrops.DROP_PERCENT_FIELD_ID,
                Translation.text("settings.hullItems.battleItems.name"), Translation.text("settings.hullItems.battleItems.tooltip"),
                BattleItemDrops.DEFAULT_DROP_PERCENT, 0, BattleItemDrops.MAX_DROP_PERCENT, itemsTab);
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
        HullUpgradeMigration.runOnSave(savedCargoes);
        HullUpgradePoints.markChanged();
        MasterSchematicActivation.sync(Global.getSector());
    }
}
