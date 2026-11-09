package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.loading.VariantSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ModuleVariants {

    private ModuleVariants() {
    }

    public static ShipVariantAPI variant(VariantSource source) {
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getSource()).thenReturn(source);
        when(variant.getHullSize()).thenReturn(HullSize.CRUISER);
        RealVariantTags.attach(variant);
        Set<String> permaMods = new LinkedHashSet<>();
        when(variant.getPermaMods()).thenReturn(permaMods);
        doAnswer(invocation -> permaMods.add(invocation.getArgument(0))).when(variant).addPermaMod(anyString());
        doAnswer(invocation -> permaMods.remove(invocation.getArgument(0))).when(variant).removePermaMod(anyString());
        when(variant.getStationModules()).thenReturn(new LinkedHashMap<>());
        return variant;
    }

    public static ShipVariantAPI parent(VariantSource source, Map<String, ShipVariantAPI> modulesBySlot) {
        ShipVariantAPI parent = variant(source);
        Map<String, ShipVariantAPI> moduleVariants = new LinkedHashMap<>(modulesBySlot);
        Map<String, String> stationModules = new LinkedHashMap<>();
        modulesBySlot.keySet().forEach(slotId -> stationModules.put(slotId, slotId + "_variant"));
        when(parent.getStationModules()).thenReturn(stationModules);
        when(parent.getModuleSlots()).thenAnswer(invocation -> new ArrayList<>(stationModules.keySet()));
        when(parent.getModuleVariant(anyString())).thenAnswer(invocation -> moduleVariants.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> moduleVariants.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(parent).setModuleVariant(anyString(), any());
        return parent;
    }

    public static ShipVariantAPI linkedModule(String parentMemberId) {
        ShipVariantAPI module = variant(VariantSource.REFIT);
        module.addTag(ModuleLinks.PARENT_TAG_PREFIX + parentMemberId);
        module.addPermaMod(ModuleHullMod.ID);
        return module;
    }
}
