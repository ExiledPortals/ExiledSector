package exiledsector.socketables;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class SocketableStore {

    static final String DATA_KEY = "exiledSector_socketables";
    private static final String ID_PREFIX = "socketable_";
    private static final Logger LOG = Logger.getLogger(SocketableStore.class);

    private final List<Socketable> owned = new ArrayList<>();
    private long nextId = 1;

    public static SocketableStore get() {
        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        return (SocketableStore) persistentData.computeIfAbsent(DATA_KEY, key -> new SocketableStore());
    }

    public List<Socketable> owned() {
        return Collections.unmodifiableList(owned);
    }

    public Socketable add(SocketableDefinition definition, long seed) {
        Socketable socketable = definition.kind().create(ID_PREFIX + nextId++, definition.id(), seed,
                SocketableRoller.roll(definition, seed));
        owned.add(socketable);
        return socketable;
    }

    public int absorbFrom(CargoAPI cargo) {
        int moved = 0;
        for (CargoStackAPI stack : cargo.getStacksCopy()) {
            SocketableItemData item = SocketableItemData.of(stack.getSpecialDataIfSpecial());
            if (item == null) {
                continue;
            }
            SocketableDefinition definition = item.definition();
            if (definition == null) {
                LOG.warn("Leaving a socketable in cargo: its definition \"" + item.definitionId() + "\" is not loaded");
                continue;
            }
            int count = Math.round(stack.getSize());
            for (int i = 0; i < count; i++) {
                add(definition, item.seed());
            }
            cargo.removeStack(stack);
            moved += count;
        }
        return moved;
    }
}
