package exiledsector.ui.cargo;

import java.awt.Color;

public record CargoIconLook(Color color, float glow, boolean framed, boolean rings, boolean ticks, boolean sparkles, boolean arcs,
                            boolean heartbeat, boolean prismatic, Color badgeColor, SwarmMotion swarm) {

    public CargoIconLook withBadge(Color badge) {
        return new CargoIconLook(color, glow, framed, rings, ticks, sparkles, arcs, heartbeat, prismatic, badge, swarm);
    }

    public CargoIconLook withSwarm(SwarmMotion motion) {
        return new CargoIconLook(color, glow, framed, rings, ticks, sparkles, arcs, heartbeat, prismatic, badgeColor, motion);
    }

    public CargoIconLook unframed() {
        return new CargoIconLook(color, glow, false, rings, ticks, sparkles, arcs, heartbeat, prismatic, badgeColor, swarm);
    }

    public static CargoIconLook plain(Color color) {
        return new CargoIconLook(color, 0f, false, false, false, false, false, false, false, null, SwarmMotion.NONE);
    }

    public static CargoIconLook outlined(Color color) {
        return new CargoIconLook(color, 0f, true, false, false, false, false, false, false, null, SwarmMotion.NONE);
    }

    public static CargoIconLook glowing(Color color, float glow) {
        return new CargoIconLook(color, glow, true, false, false, false, false, false, false, null, SwarmMotion.NONE);
    }

    public static CargoIconLook ringed(Color color, float glow) {
        return new CargoIconLook(color, glow, true, true, false, true, false, false, false, null, SwarmMotion.NONE);
    }

    public static CargoIconLook charged(Color color, float glow) {
        return new CargoIconLook(color, glow, true, true, true, true, true, true, false, null, SwarmMotion.NONE);
    }

    public static CargoIconLook refracting(Color color, float glow) {
        return new CargoIconLook(color, glow, true, true, true, true, true, true, true, null, SwarmMotion.NONE);
    }
}
