package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import java.util.ArrayList;
import java.util.List;

public final class HullFramework {

    private final String id;
    private final HullSize hullSize;
    private final List<String> socketTypeIds;

    HullFramework(String id, HullSize hullSize, List<String> socketTypeIds) {
        this.id = id;
        this.hullSize = hullSize;
        this.socketTypeIds = new ArrayList<>(socketTypeIds);
    }

    String id() {
        return id;
    }

    HullSize hullSize() {
        return hullSize;
    }

    List<String> socketTypeIds() {
        return socketTypeIds == null ? List.of() : List.copyOf(socketTypeIds);
    }
}
