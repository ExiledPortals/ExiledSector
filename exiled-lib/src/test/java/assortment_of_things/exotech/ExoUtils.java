package assortment_of_things.exotech;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

public final class ExoUtils {

    public static final ExoUtils INSTANCE = new ExoUtils();

    private ExoUtils() {
    }

    public ExoData getExoData() {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        ExoData data = (ExoData) memory.get("$rat_exo_data");
        if (data == null) {
            data = new ExoData();
            memory.set("$rat_exo_data", data);
        }
        return data;
    }
}
