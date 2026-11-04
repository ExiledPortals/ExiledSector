package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledsector.skills.skilleffect.SkillEffectSupport;

final class ArmorDecay implements AdvanceableListener {

    private final ShipAPI decayingShip;
    private float[][] pendingArmor;
    private float[][] stripRate;
    private int minX;
    private int maxX = -1;
    private int minY;
    private int maxY = -1;

    ArmorDecay(ShipAPI decayingShip) {
        this.decayingShip = decayingShip;
    }

    static ArmorDecay on(ShipAPI target) {
        return SkillEffectSupport.ensureListener(target, ArmorDecay.class, ArmorDecay::new);
    }

    void queue(ArmorGridAPI grid, int cellX, int cellY, float armorDamage, float seconds) {
        float[][] cells = grid.getGrid();
        if (pendingArmor == null || pendingArmor.length != cells.length) {
            pendingArmor = new float[cells.length][cells.length == 0 ? 0 : cells[0].length];
            stripRate = new float[pendingArmor.length][pendingArmor.length == 0 ? 0 : pendingArmor[0].length];
            maxX = -1;
        }
        if (!(armorDamage > 0f) || !(seconds > 0f) || cellX < 0 || cellY < 0 || cellX >= pendingArmor.length
                || cellY >= pendingArmor[cellX].length) {
            return;
        }
        pendingArmor[cellX][cellY] += armorDamage;
        stripRate[cellX][cellY] += armorDamage / seconds;
        widenBounds(cellX, cellY);
    }

    private void widenBounds(int x, int y) {
        if (maxX < minX) {
            minX = x;
            maxX = x;
            minY = y;
            maxY = y;
            return;
        }
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
    }

    @Override
    public void advance(float amount) {
        if (maxX < minX) {
            return;
        }
        ArmorGridAPI grid = decayingShip.getArmorGrid();
        boolean pending = false;
        boolean stripped = false;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                float cellPending = pendingArmor[x][y];
                if (cellPending <= 0f) {
                    continue;
                }
                float strip = Math.min(cellPending, stripRate[x][y] * amount);
                pendingArmor[x][y] = cellPending - strip;
                if (pendingArmor[x][y] <= 0f) {
                    pendingArmor[x][y] = 0f;
                    stripRate[x][y] = 0f;
                } else {
                    pending = true;
                }
                stripped |= grid != null && stripCell(grid, x, y, strip);
            }
        }
        if (stripped) {
            decayingShip.syncWithArmorGridState();
        }
        if (!pending) {
            maxX = -1;
        }
    }

    private static boolean stripCell(ArmorGridAPI grid, int x, int y, float amount) {
        float armor = grid.getArmorValue(x, y);
        if (amount <= 0f || armor <= 0f) {
            return false;
        }
        grid.setArmorValue(x, y, Math.max(0f, armor - amount));
        return true;
    }
}
