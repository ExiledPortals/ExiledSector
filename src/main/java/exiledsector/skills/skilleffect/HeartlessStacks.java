package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;
import java.util.Random;

final class HeartlessStacks {

    static final String ARMOR_RESTORE_PERCENT_KEY = "exiledSector_heartlessArmorRestorePercent";
    static final String ARMOR_PERCENT_PER_STACK_KEY = "exiledSector_heartlessArmorPercentPerStack";
    static final String MOBILITY_PENALTY_PERCENT_PER_STACK_KEY = "exiledSector_heartlessMobilityPenaltyPercentPerStack";
    static final String RADIATION_EMP_KEY = "exiledSector_heartlessRadiationEmp";
    static final String MAX_STACKS_KEY = "exiledSector_heartlessMaxStacks";
    static final int DEFAULT_MAX_STACKS = 5;
    private static final float ARC_THICKNESS = 12f;
    private static final Color ARC_FRINGE = new Color(120, 200, 255, 200);
    private static final String MOD_ID_PREFIX = "exiledSector_heartless_";

    private final ShipAPI ownerShip;
    private final String modId;
    private final Random random;
    private int stackCount;

    HeartlessStacks(ShipAPI ownerShip) {
        this(ownerShip, new Random());
    }

    HeartlessStacks(ShipAPI ownerShip, Random random) {
        this.ownerShip = ownerShip;
        this.modId = MOD_ID_PREFIX + ownerShip.getId();
        this.random = random;
    }

    static HeartlessStacks of(ShipAPI ownerShip) {
        List<HeartlessStacks> existing = ownerShip.getListeners(HeartlessStacks.class);
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
        }
        HeartlessStacks created = new HeartlessStacks(ownerShip);
        ownerShip.addListener(created);
        return created;
    }

    int stacks() {
        return stackCount;
    }

    void gain() {
        if (stackCount >= maxStacks() || !ownerShip.isAlive() || ownerShip.isHulk()) {
            return;
        }
        stackCount++;
        MutableShipStatsAPI stats = ownerShip.getMutableStats();
        float armorPercentPerStack = magnitude(ARMOR_PERCENT_PER_STACK_KEY);
        float restorePercent = magnitude(ARMOR_RESTORE_PERCENT_KEY);
        if (armorPercentPerStack > 0f || restorePercent > 0f) {
            addArmorToEveryCell(armorPercentPerStack);
            restoreArmor(restorePercent, armorPercentPerStack);
            ownerShip.syncWithArmorGridState();
        }
        float mobilityPenalty = magnitude(MOBILITY_PENALTY_PERCENT_PER_STACK_KEY);
        if (mobilityPenalty > 0f) {
            stats.getMaxSpeed().modifyPercent(modId, -mobilityPenalty * stackCount);
            Maneuverability.modifyPercent(stats, modId, -mobilityPenalty * stackCount);
        }
        radiationBurst(magnitude(RADIATION_EMP_KEY));
    }

    int maxStacks() {
        return Math.max(0, DEFAULT_MAX_STACKS + Math.round(magnitude(MAX_STACKS_KEY)));
    }

    private float magnitude(String key) {
        return ownerShip.getMutableStats().getDynamic().getValue(key, 0f);
    }

    private void addArmorToEveryCell(float percent) {
        if (percent <= 0f) {
            return;
        }
        ArmorGridAPI grid = ownerShip.getArmorGrid();
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
        ArmorGridAPI grid = ownerShip.getArmorGrid();
        float max = grid.getMaxArmorInCell();
        float ceiling = max * (1f + Math.max(0f, percentPerStack) * stackCount / 100f);
        float restored = max * percent / 100f;
        float[][] cells = grid.getGrid();
        for (int x = 0; x < cells.length; x++) {
            for (int y = 0; y < cells[x].length; y++) {
                float current = grid.getArmorValue(x, y);
                grid.setArmorValue(x, y, Math.max(current, Math.min(ceiling, current + restored)));
            }
        }
    }

    private void radiationBurst(float emp) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (emp <= 0f || engine == null) {
            return;
        }
        WeaponAPI weapon = pick(ownerShip.getAllWeapons().stream()
                .filter(w -> !w.isDecorative() && !w.isDisabled() && !w.isPermanentlyDisabled()).toList());
        if (weapon != null) {
            irradiate(engine, weapon.getLocation(), emp);
        }
        ShipEngineAPI shipEngine = pick(ownerShip.getEngineController().getShipEngines().stream()
                .filter(e -> !e.isSystemActivated() && !e.isDisabled() && !e.isPermanentlyDisabled()).toList());
        if (shipEngine != null) {
            irradiate(engine, shipEngine.getLocation(), emp);
        }
    }

    private <T> T pick(List<T> candidates) {
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private void irradiate(CombatEngineAPI engine, Vector2f point, float emp) {
        Vector2f target = new Vector2f(point);
        engine.applyDamage(ownerShip, target, 0f, DamageType.ENERGY, emp, true, false, ownerShip);
        engine.spawnEmpArcVisual(new Vector2f(ownerShip.getLocation()), ownerShip, target, ownerShip, ARC_THICKNESS, ARC_FRINGE, Color.WHITE);
    }
}
