package exiledlib.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import exiledlib.skills.skilleffect.render.FallbackSupport;
import org.apache.log4j.Logger;

import java.util.function.Consumer;

abstract class WeaponDroneShell implements AdvanceableListener {

    protected final ShipAPI droneShip;
    private boolean removed;
    private boolean markersMirrored;

    protected WeaponDroneShell(ShipAPI droneShip) {
        this.droneShip = droneShip;
    }

    static <T> T buildOrMarkUnsupported(FallbackSupport.ThrowingSupplier<T> build, WeaponAPI weapon, String weaponId,
            Consumer<WeaponAPI> markUnsupported, Logger log, String effectName, String fallbackBehaviour) {
        T built = FallbackSupport.getOrFallback(build, null, log, effectName + " could not build a drone for weapon " + weaponId
                + "; that weapon " + fallbackBehaviour + " for the rest of the session");
        if (built == null) {
            markUnsupported.accept(weapon);
        }
        return built;
    }

    void markSourceMount(WeaponAPI weapon) {
        droneShip.setCustomData(WeaponDroneFactory.SOURCE_MOUNT_KEY, WeaponDroneFactory.mountOf(weapon));
    }

    abstract ShipAPI firingShip();

    abstract void advanceState(float amount);

    abstract void forget();

    boolean isRemoved() {
        return removed;
    }

    boolean isInPlay() {
        return !removed && Global.getCombatEngine().isEntityInPlay(droneShip);
    }

    @Override
    public void advance(float amount) {
        if (removed) {
            return;
        }
        if (!markersMirrored) {
            WeaponDroneFactory.mirrorMarkerHullMods(firingShip(), droneShip);
            markersMirrored = true;
        }
        advanceState(amount);
    }

    void remove() {
        removed = true;
        Global.getCombatEngine().removeEntity(droneShip);
        forget();
    }
}
