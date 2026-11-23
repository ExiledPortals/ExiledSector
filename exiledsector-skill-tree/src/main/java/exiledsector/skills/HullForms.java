package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipVariantAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class HullForms {

    private record HullForm(Set<String> hullModIds, String typeId) {
    }

    private static final Map<String, HullForm> FORMS_BY_BASE_TYPE_ID = Map.of(
            "adaptive_entropy_projector", new HullForm(Set.of("rat_abyssal_grid", "rat_raphaels_grace"), "abyssal_entropy_projector"));
    private static final Set<String> FORM_HULL_MOD_IDS = formHullModIds();
    private static final Map<ShipSkillData, Set<String>> FORM_HULL_MODS_BY_SHIP = Collections.synchronizedMap(new WeakHashMap<>());

    private HullForms() {
    }

    private static Set<String> formHullModIds() {
        Set<String> hullModIds = new LinkedHashSet<>();
        FORMS_BY_BASE_TYPE_ID.values().forEach(form -> hullModIds.addAll(form.hullModIds()));
        return Set.copyOf(hullModIds);
    }

    public static Set<String> bindHull(ShipSkillData data, ShipVariantAPI variant) {
        if (data == null || variant == null) {
            return Set.of();
        }
        Set<String> present = null;
        for (String hullModId : FORM_HULL_MOD_IDS) {
            if (variant.hasHullMod(hullModId)) {
                if (present == null) {
                    present = new HashSet<>();
                }
                present.add(hullModId);
            }
        }
        if (present == null) {
            FORM_HULL_MODS_BY_SHIP.remove(data);
            return Set.of();
        }
        Set<String> bound = Set.copyOf(present);
        FORM_HULL_MODS_BY_SHIP.put(data, bound);
        return bound;
    }

    public static SkillType formOf(SkillType type, ShipSkillData data) {
        HullForm form = type == null ? null : FORMS_BY_BASE_TYPE_ID.get(type.getId());
        if (form == null || data == null) {
            return type;
        }
        Set<String> hullModIds = FORM_HULL_MODS_BY_SHIP.get(data);
        if (hullModIds == null || Collections.disjoint(hullModIds, form.hullModIds())) {
            return type;
        }
        SkillType formType = SkillTree.getType(form.typeId());
        return formType != null ? formType : type;
    }

    public static List<SkillType> withForms(SkillType type) {
        List<SkillType> types = new ArrayList<>();
        types.add(type);
        HullForm form = type == null ? null : FORMS_BY_BASE_TYPE_ID.get(type.getId());
        SkillType formType = form == null ? null : SkillTree.getType(form.typeId());
        if (formType != null) {
            types.add(formType);
        }
        return types;
    }
}
