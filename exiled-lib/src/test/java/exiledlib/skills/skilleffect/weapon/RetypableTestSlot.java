package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class RetypableTestSlot implements WeaponSlotAPI {

    private final String slotId;
    private final boolean builtIn;
    private WeaponType weaponType;

    public RetypableTestSlot(String slotId, WeaponType weaponType, boolean builtIn) {
        this.slotId = slotId;
        this.weaponType = weaponType;
        this.builtIn = builtIn;
    }

    public RetypableTestSlot copy() {
        return new RetypableTestSlot(slotId, weaponType, builtIn);
    }

    public void setWeaponType(WeaponType weaponType) {
        this.weaponType = weaponType;
    }

    @Override
    public WeaponType getWeaponType() {
        return weaponType;
    }

    @Override
    public String getId() {
        return slotId;
    }

    @Override
    public boolean isBuiltIn() {
        return builtIn;
    }

    @Override
    public boolean isHardpoint() {
        return false;
    }

    @Override
    public boolean isTurret() {
        return true;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isSystemSlot() {
        return false;
    }

    @Override
    public boolean isDecorative() {
        return false;
    }

    @Override
    public WeaponSize getSlotSize() {
        return WeaponSize.MEDIUM;
    }

    @Override
    public float getArc() {
        return 0f;
    }

    @Override
    public void setArc(float arc) {
    }

    @Override
    public float getAngle() {
        return 0f;
    }

    @Override
    public Vector2f computePosition(CombatEntityAPI ship) {
        return new Vector2f();
    }

    @Override
    public boolean isStationModule() {
        return false;
    }

    @Override
    public boolean weaponFits(WeaponSpecAPI weapon) {
        return false;
    }

    @Override
    public void setAngle(float angle) {
    }

    @Override
    public Vector2f getLocation() {
        return new Vector2f();
    }

    @Override
    public float getRenderOrderMod() {
        return 0f;
    }

    @Override
    public void setRenderOrderMod(float renderOrderMod) {
    }

    @Override
    public float computeMidArcAngle(ShipAPI ship) {
        return 0f;
    }

    @Override
    public List<Vector2f> getLaunchPointOffsets() {
        return List.of();
    }

    @Override
    public boolean isWeaponSlot() {
        return true;
    }
}
