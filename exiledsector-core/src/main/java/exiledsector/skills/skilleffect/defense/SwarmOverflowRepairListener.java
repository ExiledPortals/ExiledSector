package exiledsector.skills.skilleffect.defense;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import exiledsector.skills.skilleffect.ShipCombatListener;
import exiledsector.skills.skilleffect.shipsystem.FragmentSwarms;

import java.util.Map;

final class SwarmOverflowRepairListener extends ShipCombatListener implements AdvanceableListener {

    static final String REPAIR_PERCENT_KEY = "exiledSector_swarmOverflowArmorRepairPercent";
    static final float BATTLE_CAP_PERCENT = 10f;
    static final String REPAIRED_KEY_PREFIX = "exiledSector_swarmOverflowArmorRepaired_";
    private static final float CHECK_SECONDS = 1f;

    private final IntervalUtil overflowCheck = new IntervalUtil(CHECK_SECONDS, CHECK_SECONDS);
    private float overflowFragments;

    SwarmOverflowRepairListener(ShipAPI ownerShip) {
        super(ownerShip);
    }

    @Override
    public void advance(float amount) {
        overflowCheck.advance(amount);
        if (!overflowCheck.intervalElapsed()) {
            return;
        }
        RoilingSwarmEffect swarm = FragmentSwarms.swarmOf(ownerShip);
        if (!FragmentSwarms.isFull(swarm) || !ownerIsAliveNotHulk()) {
            overflowFragments = 0f;
            return;
        }
        overflowFragments += CHECK_SECONDS * swarm.getParams().memberRespawnRate;
        int fragments = (int) overflowFragments;
        if (fragments > 0) {
            overflowFragments -= fragments;
            repair(fragments);
        }
    }

    private void repair(int fragments) {
        ArmorGridAPI grid = ownerShip.getArmorGrid();
        CombatEngineAPI engine = Global.getCombatEngine();
        float percent = magnitude(REPAIR_PERCENT_KEY);
        if (grid == null || engine == null || percent <= 0f) {
            return;
        }
        Map<String, Object> customData = engine.getCustomData();
        String repairedKey = repairedKey(ownerShip);
        float repaired = customData.get(repairedKey) instanceof Float earlier ? earlier : 0f;
        float totalArmor = ArmorCells.total(grid);
        float restored = restoreMostDamaged(grid, fragments, totalArmor * percent / 100f, totalArmor * BATTLE_CAP_PERCENT / 100f - repaired);
        if (restored > 0f) {
            customData.put(repairedKey, repaired + restored);
            ownerShip.syncWithArmorGridState();
        }
    }

    static String repairedKey(ShipAPI ship) {
        String memberId = ship.getFleetMemberId();
        return REPAIRED_KEY_PREFIX + (memberId != null ? memberId : ship.getId());
    }

    static float restoreMostDamaged(ArmorGridAPI grid, int fragments, float perFragment, float budget) {
        float maxArmorInCell = grid.getMaxArmorInCell();
        float restored = 0f;
        for (int fragment = 0; fragment < fragments && restored < budget; fragment++) {
            int[] cell = ArmorCells.mostDamaged(grid);
            float current = grid.getArmorValue(cell[0], cell[1]);
            float repair = Math.min(Math.min(perFragment, maxArmorInCell - current), budget - restored);
            if (!(repair > 0f)) {
                break;
            }
            grid.setArmorValue(cell[0], cell[1], current + repair);
            restored += repair;
        }
        return restored;
    }
}
