package exiledsector.socketables;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.i18n.Translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class HullFramework {

    private final String id;
    private final HullSize hullSize;
    private final SocketableRarity rarity;
    private final List<String> socketTypeIds;
    private final long seed;

    HullFramework(String id, HullFrameworkData data) {
        this.id = id;
        this.hullSize = data.hullSize();
        this.rarity = data.rarity();
        this.socketTypeIds = new ArrayList<>(data.socketTypes().stream().map(SocketType::id).toList());
        this.seed = data.seed();
    }

    public String id() {
        return id;
    }

    public HullSize hullSize() {
        return hullSize;
    }

    public SocketableRarity rarity() {
        return rarity;
    }

    public long seed() {
        return seed;
    }

    public List<SocketType> socketTypes() {
        List<SocketType> socketTypes = new ArrayList<>(socketTypeIds.size());
        for (String socketTypeId : socketTypeIds) {
            SocketType socketType = SocketType.byIdOrNull(socketTypeId);
            if (socketType != null && socketType.isFramework()) {
                socketTypes.add(socketType);
            }
        }
        return Collections.unmodifiableList(socketTypes);
    }

    public int slotCount() {
        return socketTypeIds.size();
    }

    public SocketType socketType(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= socketTypeIds.size()) {
            return null;
        }
        SocketType socketType = SocketType.byIdOrNull(socketTypeIds.get(slotIndex));
        return socketType != null && socketType.isFramework() ? socketType : null;
    }

    public HullFrameworkData data() {
        return new HullFrameworkData(hullSize, rarity, socketTypes(), seed);
    }

    public String name() {
        return Translation.msg("framework.name")
                .arg("rarity", Translation.text("ui.socketStorage.rarity." + rarity.name().toLowerCase(Locale.ROOT)))
                .arg("size", Translation.text("hullSize." + hullSize.name())).text();
    }

    public String iconPath() {
        return HullFrameworkData.iconPath(hullSize, rarity);
    }
}
