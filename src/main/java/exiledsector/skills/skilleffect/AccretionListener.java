package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

final class AccretionListener implements AdvanceableListener {

    static final String ARMOR_RESTORE_PERCENT_KEY = "exiledSector_accretionArmorRestorePercent";
    static final String ARMOR_PERCENT_PER_STACK_KEY = "exiledSector_accretionArmorPercentPerStack";
    static final String MOBILITY_PENALTY_PERCENT_PER_STACK_KEY = "exiledSector_accretionMobilityPenaltyPercentPerStack";
    static final String RADIATION_DISABLES_KEY = "exiledSector_accretionRadiationDisables";
    static final float RANGE = 1500f;
    static final int MAX_STACKS = 5;
    private static final float CHECK_SECONDS = 0.25f;
    private static final float SEARCH_MARGIN = 500f;
    private static final float ARC_THICKNESS = 12f;
    private static final Color ARC_FRINGE = new Color(120, 200, 255, 200);
    private static final String MOD_ID_PREFIX = "exiledSector_accretion_";

    private final ShipAPI ship;
    private final String modId;
    private final IntervalUtil interval = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private final Random random;
    private Set<ShipAPI> aliveNearby = new HashSet<>();
    private int stacks;

    AccretionListener(ShipAPI ship) {
        this(ship, new Random());
    }

    AccretionListener(ShipAPI ship, Random random) {
        this.ship = ship;
        this.modId = MOD_ID_PREFIX + ship.getId();
        this.random = random;
    }

    @Override
    public void advance(float amount) {
        if (!ship.isAlive() || ship.isHulk()) {
            return;
        }
        interval.advance(amount);
        if (!interval.intervalElapsed()) {
            return;
        }
        int wrecks = 0;
        Set<ShipAPI> stillAlive = new HashSet<>();
        for (ShipAPI other : CombatQueries.shipsNear(ship.getLocation(), RANGE + SEARCH_MARGIN, this::counts)) {
            if (other.isAlive() && !other.isHulk()) {
                stillAlive.add(other);
            } else if (isWreck(other) && aliveNearby.contains(other)
                    && CombatQueries.withinRadius(other.getLocation(), ship.getLocation(), RANGE)) {
                wrecks++;
            }
        }
        aliveNearby = stillAlive;
        for (int i = 0; i < wrecks; i++) {
            accrete();
        }
    }

    private boolean counts(ShipAPI other) {
        return other != ship && !other.isFighter() && other.getParentStation() == null;
    }

    private static boolean isWreck(ShipAPI other) {
        return other.isHulk() || other.getHitpoints() <= 0f;
    }

    private void accrete() {
        MutableShipStatsAPI stats = ship.getMutableStats();
        if (stacks < MAX_STACKS) {
            stacks++;
            addArmorToEveryCell(stats.getDynamic().getValue(ARMOR_PERCENT_PER_STACK_KEY, 0f));
            applyMobilityPenalty(stats);
            radiationBurst(Math.round(stats.getDynamic().getValue(RADIATION_DISABLES_KEY, 0f)));
        }
        restoreArmor(stats.getDynamic().getValue(ARMOR_RESTORE_PERCENT_KEY, 0f),
                stats.getDynamic().getValue(ARMOR_PERCENT_PER_STACK_KEY, 0f));
        ship.syncWithArmorGridState();
    }

    private void addArmorToEveryCell(float percent) {
        if (percent <= 0f) {
            return;
        }
        ArmorGridAPI grid = ship.getArmorGrid();
        float added = grid.getMaxArmorInCell() * percent / 100f;
        float[][] cells = grid.getGrid();
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                grid.setArmorValue(x, y, grid.getArmorValue(x, y) + added);
            }
        }
    }

    private void restoreArmor(float percent, float percentPerStack) {
        if (percent <= 0f) {
            return;
        }
        ArmorGridAPI grid = ship.getArmorGrid();
        float max = grid.getMaxArmorInCell();
        float ceiling = max * (1f + percentPerStack * stacks / 100f);
        float restored = max * percent / 100f;
        float[][] cells = grid.getGrid();
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                float current = grid.getArmorValue(x, y);
                grid.setArmorValue(x, y, Math.max(current, Math.min(ceiling, current + restored)));
            }
        }
    }

    private void applyMobilityPenalty(MutableShipStatsAPI stats) {
        float percent = -stats.getDynamic().getValue(MOBILITY_PENALTY_PERCENT_PER_STACK_KEY, 0f) * stacks;
        stats.getMaxSpeed().modifyPercent(modId, percent);
        stats.getMaxTurnRate().modifyPercent(modId, percent);
    }

    private void radiationBurst(int count) {
        for (int i = 0; i < count; i++) {
            WeaponAPI weapon = pick(ship.getAllWeapons().stream()
                    .filter(w -> !w.isDecorative() && !w.isDisabled() && !w.isPermanentlyDisabled()).toList());
            if (weapon != null) {
                weapon.disable();
                arcTo(weapon.getLocation());
            }
            ShipEngineAPI engine = pick(ship.getEngineController().getShipEngines().stream()
                    .filter(e -> !e.isSystemActivated() && !e.isDisabled() && !e.isPermanentlyDisabled()).toList());
            if (engine != null) {
                engine.disable();
                arcTo(engine.getLocation());
            }
        }
    }

    private <T> T pick(List<T> candidates) {
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private void arcTo(Vector2f point) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            engine.spawnEmpArcVisual(new Vector2f(ship.getLocation()), ship, new Vector2f(point), ship, ARC_THICKNESS, ARC_FRINGE, Color.WHITE);
        }
    }
}
