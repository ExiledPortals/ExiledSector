package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import exiledsector.i18n.StyledText;
import exiledsector.i18n.Translation;
import exiledsector.skills.SocketedItemScaling;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public final class SlotItemEffect implements SkillEffect {

    static final String NAME_SUFFIX = "_ITEM_EFFECT_PERCENT";
    private static final String DESCRIPTION_KEY = "effect.SLOT_ITEM_EFFECT_PERCENT.";
    private static final Map<String, SlotItemEffect> BY_NAME = new LinkedHashMap<>();

    private final String name;
    private final Supplier<String> slotName;
    private final SocketedItemScaling scaling;

    private SlotItemEffect(String category, String slotId, Supplier<String> slotName) {
        this.name = slotId.toUpperCase(Locale.ROOT) + NAME_SUFFIX;
        this.slotName = slotName;
        this.scaling = new SocketedItemScaling(SocketedItemScaling.Scope.SLOT, category, slotId);
    }

    public static synchronized SlotItemEffect register(String category, String slotId, Supplier<String> slotName) {
        SlotItemEffect effect = new SlotItemEffect(category, slotId, slotName);
        return BY_NAME.computeIfAbsent(effect.name, name -> effect);
    }

    public static synchronized List<SlotItemEffect> all() {
        return List.copyOf(new ArrayList<>(BY_NAME.values()));
    }

    public static synchronized SlotItemEffect byName(String name) {
        return name == null ? null : BY_NAME.get(name);
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String modId, float magnitude) {
    }

    @Override
    public SocketedItemScaling socketedItemScaling() {
        return scaling;
    }

    @Override
    public StyledText description(float magnitude) {
        return Translation.msg(DESCRIPTION_KEY + (magnitude >= 0 ? "more" : "less"))
                .arg("value", Math.abs(magnitude)).arg("slot", slotName.get()).styled();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
