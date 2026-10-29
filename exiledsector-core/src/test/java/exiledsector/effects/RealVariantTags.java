package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipVariantAPI;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public final class RealVariantTags {

    private RealVariantTags() {
    }

    public static Set<String> attach(ShipVariantAPI variant) {
        Set<String> tags = new LinkedHashSet<>();
        when(variant.getTags()).thenReturn(tags);
        doAnswer(invocation -> tags.add(invocation.getArgument(0))).when(variant).addTag(anyString());
        doAnswer(invocation -> tags.remove(invocation.getArgument(0))).when(variant).removeTag(anyString());
        return tags;
    }
}
