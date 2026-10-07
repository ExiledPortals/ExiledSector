package exiledsector.effects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.ExiledSectorModPlugin;
import exiledsector.ModSettings;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import lunalib.lunaSettings.LunaSettingsListener;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class NpcBonusScale {

    public static final String START_PLAYER_LEVEL_FIELD_ID = "exiledSector_npcBonusScaleStartPlayerLevel";
    public static final String LOW_SHIP_LEVEL_FIELD_ID = "exiledSector_npcBonusScaleLowShipLevel";
    public static final String HIGH_SHIP_LEVEL_FIELD_ID = "exiledSector_npcBonusScaleHighShipLevel";
    public static final String MAX_MULTIPLIER_FIELD_ID = "exiledSector_npcBonusScaleMaxMultiplier";

    public static final int DEFAULT_START_PLAYER_LEVEL = 15;
    public static final int DEFAULT_LOW_SHIP_LEVEL = 15;
    public static final int DEFAULT_HIGH_SHIP_LEVEL = 50;
    public static final float DEFAULT_MAX_MULTIPLIER = 3f;

    private static final float MULTIPLIER_STEPS_PER_UNIT = 100f;
    private static final float REFRESH_DAYS = 1f;
    private static final AtomicReference<Snapshot> SNAPSHOT = new AtomicReference<>();

    private record Snapshot(float multiplier, long timestamp) {
    }

    record Config(int startPlayerLevel, int lowShipLevel, int highShipLevel, float maxMultiplier) {

        static Config current() {
            return new Config(ModSettings.intOr(START_PLAYER_LEVEL_FIELD_ID, DEFAULT_START_PLAYER_LEVEL),
                    ModSettings.intOr(LOW_SHIP_LEVEL_FIELD_ID, DEFAULT_LOW_SHIP_LEVEL),
                    ModSettings.intOr(HIGH_SHIP_LEVEL_FIELD_ID, DEFAULT_HIGH_SHIP_LEVEL),
                    ModSettings.floatOr(MAX_MULTIPLIER_FIELD_ID, DEFAULT_MAX_MULTIPLIER));
        }
    }

    public static final class SettingsListener implements LunaSettingsListener {

        @Override
        public void settingsChanged(String modId) {
            if (ExiledSectorModPlugin.MOD_ID.equals(modId)) {
                invalidate();
            }
        }
    }

    private NpcBonusScale() {
    }

    public static float current() {
        SectorAPI sector = Global.getSector();
        if (sector == null || sector.getClock() == null) {
            return 1f;
        }
        Snapshot snapshot = SNAPSHOT.get();
        if (snapshot != null && sector.getClock().getElapsedDaysSince(snapshot.timestamp()) < REFRESH_DAYS) {
            return snapshot.multiplier();
        }
        float multiplier = rounded(multiplier(sector, Config.current()));
        SNAPSHOT.set(new Snapshot(multiplier, sector.getClock().getTimestamp()));
        return multiplier;
    }

    public static void invalidate() {
        SNAPSHOT.set(null);
    }

    private static float multiplier(SectorAPI sector, Config config) {
        if (sector.getPlayerStats() == null || sector.getPlayerStats().getLevel() < config.startPlayerLevel()) {
            return 1f;
        }
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet == null || playerFleet.getFleetData() == null) {
            return 1f;
        }
        float averageLevel = averageCombatShipLevel(playerFleet.getFleetData().getMembersListCopy(), SkillTreeInstaller.currentLevelFloor());
        return multiplier(sector.getPlayerStats().getLevel(), averageLevel, config);
    }

    static float multiplier(int playerLevel, float averageShipLevel, Config config) {
        if (playerLevel < config.startPlayerLevel() || config.maxMultiplier() <= 1f) {
            return 1f;
        }
        int levelSpan = config.highShipLevel() - config.lowShipLevel();
        float progress = levelSpan <= 0
                ? (averageShipLevel >= config.highShipLevel() ? 1f : 0f)
                : (averageShipLevel - config.lowShipLevel()) / levelSpan;
        float clampedProgress = Math.max(0f, Math.min(1f, progress));
        return 1f + clampedProgress * (config.maxMultiplier() - 1f);
    }

    static float averageCombatShipLevel(List<FleetMemberAPI> members, int levelFloor) {
        int combatShips = 0;
        int levelTotal = 0;
        for (FleetMemberAPI member : members) {
            if (member.isFighterWing() || member.isCivilian() || member.isMothballed()) {
                continue;
            }
            ShipSkillData shipData = ShipSkillDataManager.find(member.getId());
            int storedLevel = shipData == null ? 0 : shipData.getLevel();
            levelTotal += Math.max(storedLevel, levelFloor);
            combatShips++;
        }
        return combatShips == 0 ? 0f : (float) levelTotal / combatShips;
    }

    private static float rounded(float multiplier) {
        return Math.round(multiplier * MULTIPLIER_STEPS_PER_UNIT) / MULTIPLIER_STEPS_PER_UNIT;
    }
}
