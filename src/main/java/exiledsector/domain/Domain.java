package exiledsector.domain;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.thoughtworks.xstream.XStream;
import exiledsector.compat.CompatTarget;

import java.util.List;

public interface Domain {

    default void onApplicationLoad() {
    }

    default void registerSettings() {
    }

    default void reloadSettings() {
    }

    default void loadData() {
    }

    default void loadItems() {
    }

    default void addCodexEntries() {
    }

    default void configureXStream(XStream xstream) {
    }

    default void onGameLoad(boolean newGame, List<CargoAPI> savedCargoes) {
    }

    default void afterSaveRepair() {
    }

    default void afterLootListeners() {
    }

    default void afterSocketCustody() {
    }

    default List<CompatTarget> compatTargets() {
        return List.of();
    }
}
