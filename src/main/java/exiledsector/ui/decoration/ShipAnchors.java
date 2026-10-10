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
    static final float ARMOR_RAY_FORWARD = 0.35f;
    static final float ARMOR_EDGE_INSET = 0.85f;
    static final int PULL_IN_STEPS = 10;
    private static final float PARALLEL_TOLERANCE = 1e-6f;

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
        anchors.put(SocketType.ARMOR_PLATING, new Anchor(frontExtent * 0.3f, halfBeam * 0.85f));
        anchors.put(SocketType.PHASE_COIL, new Anchor(-rearExtent * 0.05f, -halfBeam * 0.35f));
        anchors.put(SocketType.ENGINE_ROOM, engineAnchor(visuals, rearExtent));
        anchors.put(SocketType.WEAPON_MOUNT, weaponAnchor(weaponSlots, frontExtent));
        anchors.put(SocketType.FLIGHT_DECK, launchBayAnchor(weaponSlots, halfBeam));
        anchors.put(SocketType.SHIELD_GENERATOR, shieldAnchor(hullSpec, frontExtent, halfBeam));
        List<Anchor> outline = visuals.outline();
        if (outline.size() >= 3) {
            anchors.put(SocketType.ARMOR_PLATING, armorAnchor(outline, anchors.get(SocketType.ARMOR_PLATING)));
            if (contains(outline, CENTRE)) {
                anchors.replaceAll((socketType, anchor) -> socketType == SocketType.ARMOR_PLATING ? anchor : pullInside(outline, anchor));
            }
        }
        return new ShipAnchors(anchors);
    }

    static Anchor armorAnchor(List<Anchor> outline, Anchor fallback) {
        Anchor armorEdge = rayExit(outline, ARMOR_RAY_FORWARD, 1f);
        return armorEdge == null ? fallback : scaled(armorEdge, ARMOR_EDGE_INSET);
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

    static boolean contains(List<Anchor> outline, Anchor point) {
        boolean inside = false;
        for (int i = 0, j = outline.size() - 1; i < outline.size(); j = i++) {
            Anchor current = outline.get(i);
            Anchor previous = outline.get(j);
            boolean straddles = (current.left() > point.left()) != (previous.left() > point.left());
            if (straddles && point.forward() < (previous.forward() - current.forward()) * (point.left() - current.left())
                    / (previous.left() - current.left()) + current.forward()) {
                inside = !inside;
            }
        }
        return inside;
    }

    static Anchor rayExit(List<Anchor> outline, float directionForward, float directionLeft) {
        float furthest = -1f;
        for (int i = 0, j = outline.size() - 1; i < outline.size(); j = i++) {
            Anchor start = outline.get(j);
            float edgeForward = outline.get(i).forward() - start.forward();
            float edgeLeft = outline.get(i).left() - start.left();
            float denominator = directionForward * edgeLeft - directionLeft * edgeForward;
            if (Math.abs(denominator) < PARALLEL_TOLERANCE) {
                continue;
            }
            float rayDistance = (start.forward() * edgeLeft - start.left() * edgeForward) / denominator;
            float edgeShare = (start.forward() * directionLeft - start.left() * directionForward) / denominator;
            if (rayDistance > 0f && edgeShare >= 0f && edgeShare <= 1f) {
                furthest = Math.max(furthest, rayDistance);
            }
        }
        return furthest > 0f ? new Anchor(directionForward * furthest, directionLeft * furthest) : null;
    }

    static Anchor pullInside(List<Anchor> outline, Anchor anchor) {
        for (int step = 0; step < PULL_IN_STEPS; step++) {
            Anchor candidate = scaled(anchor, 1f - step / (float) PULL_IN_STEPS);
            if (contains(outline, candidate)) {
                return candidate;
            }
        }
        return CENTRE;
    }

    private static Anchor scaled(Anchor anchor, float share) {
        return new Anchor(anchor.forward() * share, anchor.left() * share);
    }

    private static Anchor anchorAt(Vector2f location) {
        return new Anchor(location.x, location.y);
    }
}
