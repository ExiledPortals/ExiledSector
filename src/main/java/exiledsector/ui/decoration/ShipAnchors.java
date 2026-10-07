package exiledsector.ui.decoration;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import exiledsector.socketables.SocketType;
import org.lwjgl.util.vector.Vector2f;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ShipAnchors {

    public record Anchor(float forward, float left) {
    }

    static final Anchor CENTRE = new Anchor(0f, 0f);

    private final Map<SocketType, Anchor> anchorsByType;

    private ShipAnchors(Map<SocketType, Anchor> anchorsByType) {
        this.anchorsByType = anchorsByType;
    }

    public Anchor forSocket(SocketType socketType) {
        return anchorsByType.getOrDefault(socketType, CENTRE);
    }

    static ShipAnchors of(ShipHullSpecAPI hullSpec, FleetHullVisuals visuals, float spriteWidth, float spriteHeight) {
        float centreY = visuals.hasCenter() ? visuals.centerY() : spriteHeight / 2f;
        float frontExtent = Math.max(1f, spriteHeight - centreY);
        float rearExtent = Math.max(1f, centreY);
        float halfBeam = Math.max(1f, spriteWidth / 2f);
        List<WeaponSlotAPI> weaponSlots = hullSpec == null ? List.of() : hullSpec.getAllWeaponSlotsCopy();
        Map<SocketType, Anchor> anchors = new EnumMap<>(SocketType.class);
        anchors.put(SocketType.BRIDGE, new Anchor(frontExtent * 0.45f, 0f));
        anchors.put(SocketType.CREW_QUARTERS, new Anchor(frontExtent * 0.1f, halfBeam * 0.4f));
        anchors.put(SocketType.REACTOR, new Anchor(-rearExtent * 0.25f, 0f));
        anchors.put(SocketType.PHASE_COIL, new Anchor(-rearExtent * 0.05f, -halfBeam * 0.35f));
        anchors.put(SocketType.ENGINE_ROOM, engineAnchor(visuals, rearExtent));
        anchors.put(SocketType.WEAPON_MOUNT, weaponAnchor(weaponSlots, frontExtent));
        anchors.put(SocketType.FLIGHT_DECK, launchBayAnchor(weaponSlots, halfBeam));
        anchors.put(SocketType.SHIELD_GENERATOR, shieldAnchor(hullSpec, frontExtent, halfBeam));
        return new ShipAnchors(anchors);
    }

    private static Anchor engineAnchor(FleetHullVisuals visuals, float rearExtent) {
        List<FleetHullVisuals.EngineFlame> flames = visuals.flames();
        if (flames.isEmpty()) {
            return new Anchor(-rearExtent * 0.8f, 0f);
        }
        float forwardTotal = 0f;
        float leftTotal = 0f;
        for (FleetHullVisuals.EngineFlame flame : flames) {
            forwardTotal += flame.forwardOffset();
            leftTotal += flame.leftOffset();
        }
        return new Anchor(forwardTotal / flames.size() * 0.9f, leftTotal / flames.size());
    }

    private static Anchor weaponAnchor(List<WeaponSlotAPI> weaponSlots, float frontExtent) {
        WeaponSlotAPI largestSlot = null;
        for (WeaponSlotAPI slot : weaponSlots) {
            if (isWeaponMount(slot) && (largestSlot == null || slot.getSlotSize().ordinal() > largestSlot.getSlotSize().ordinal()
                    || slot.getSlotSize() == largestSlot.getSlotSize() && slot.getLocation().x > largestSlot.getLocation().x)) {
                largestSlot = slot;
            }
        }
        return largestSlot == null ? new Anchor(frontExtent * 0.6f, 0f) : anchorAt(largestSlot.getLocation());
    }

    private static boolean isWeaponMount(WeaponSlotAPI slot) {
        return slot.getLocation() != null && slot.getSlotSize() != null && !slot.isDecorative() && !slot.isSystemSlot()
                && !slot.isStationModule() && slot.getWeaponType() != WeaponType.LAUNCH_BAY;
    }

    private static Anchor launchBayAnchor(List<WeaponSlotAPI> weaponSlots, float halfBeam) {
        float forwardTotal = 0f;
        float leftTotal = 0f;
        int bayCount = 0;
        for (WeaponSlotAPI slot : weaponSlots) {
            if (slot.getWeaponType() == WeaponType.LAUNCH_BAY && slot.getLocation() != null) {
                forwardTotal += slot.getLocation().x;
                leftTotal += slot.getLocation().y;
                bayCount++;
            }
        }
        return bayCount == 0 ? new Anchor(0f, -halfBeam * 0.45f) : new Anchor(forwardTotal / bayCount, leftTotal / bayCount);
    }

    private static Anchor shieldAnchor(ShipHullSpecAPI hullSpec, float frontExtent, float halfBeam) {
        ShipHullSpecAPI.ShieldSpecAPI shieldSpec = hullSpec == null ? null : hullSpec.getShieldSpec();
        if (shieldSpec != null && (shieldSpec.getCenterX() != 0f || shieldSpec.getCenterY() != 0f)) {
            return new Anchor(shieldSpec.getCenterX(), shieldSpec.getCenterY());
        }
        return new Anchor(frontExtent * 0.15f, -halfBeam * 0.2f);
    }

    private static Anchor anchorAt(Vector2f location) {
        return new Anchor(location.x, location.y);
    }
}
