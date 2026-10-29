package exiledsector.skills;

import exiledsector.i18n.StyledText;
import exiledsector.skills.skilleffect.EffectBacking;
import exiledsector.skills.skilleffect.EffectText;
import exiledsector.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SocketedItemScaling(Scope scope, String category, String slotId) implements EffectBacking {

    public enum Scope {
        ALL, TARGETED, SLOT, DRAWBACKS
    }

    private record SlotKey(String category, String slotId) {
    }

    private record Percents(Map<String, Float> all, Map<String, Float> drawbacks, Map<SlotKey, Float> targeted, Map<SlotKey, Float> slotScoped) {

        boolean none() {
            return all.isEmpty() && drawbacks.isEmpty() && targeted.isEmpty() && slotScoped.isEmpty();
        }

        float forItem(SocketedItem item) {
            SlotKey key = new SlotKey(item.category(), item.slotId());
            return all.getOrDefault(item.category(), 0f) + targeted.getOrDefault(key, 0f) + slotScoped.getOrDefault(key, 0f);
        }

        float drawbacksFor(SocketedItem item) {
            return drawbacks.getOrDefault(item.category(), 0f);
        }
    }

    public SocketedItemScaling(Scope scope, String category) {
        this(scope, category, null);
    }

    @Override
    public boolean supportsTemporaryGating() {
        return false;
    }

    @Override
    public boolean lowerIsBetter() {
        return scope == Scope.DRAWBACKS;
    }

    @Override
    public StyledText description(SkillEffect effect, float magnitude) {
        return EffectText.signed(effect, magnitude).styled();
    }

    public static List<EffectContribution> withScaledItems(List<EffectContribution> contributions) {
        Percents percents = percents(contributions);
        if (percents.none()) {
            return contributions;
        }
        List<EffectContribution> scaled = new ArrayList<>(contributions.size());
        for (EffectContribution contribution : contributions) {
            scaled.add(contribution.origin() instanceof SocketedItem item ? scaled(contribution, item, percents) : contribution);
        }
        return scaled;
    }

    private static Percents percents(List<EffectContribution> contributions) {
        Map<String, Float> all = new HashMap<>();
        Map<String, Float> drawbacks = new HashMap<>();
        Map<SlotKey, Float> targeted = new HashMap<>();
        Map<SlotKey, Float> slotScoped = new HashMap<>();
        for (EffectContribution contribution : contributions) {
            if (!contribution.permanent()) {
                continue;
            }
            String targetSlotId = contribution.origin() instanceof SocketedItem item ? item.targetSlotId() : null;
            for (SkillTypeEffect effect : contribution.effects()) {
                SocketedItemScaling scaling = effect.effect().socketedItemScaling();
                if (scaling == null) {
                    continue;
                }
                switch (scaling.scope()) {
                    case ALL -> all.merge(scaling.category(), effect.magnitude(), Float::sum);
                    case DRAWBACKS -> drawbacks.merge(scaling.category(), effect.magnitude(), Float::sum);
                    case SLOT -> slotScoped.merge(new SlotKey(scaling.category(), scaling.slotId()), effect.magnitude(), Float::sum);
                    case TARGETED -> {
                        if (targetSlotId != null) {
                            targeted.merge(new SlotKey(scaling.category(), targetSlotId), effect.magnitude(), Float::sum);
                        }
                    }
                }
            }
        }
        return new Percents(all, drawbacks, targeted, slotScoped);
    }

    private static EffectContribution scaled(EffectContribution contribution, SocketedItem item, Percents percents) {
        float itemPercent = percents.forItem(item);
        float drawbackPercent = percents.drawbacksFor(item);
        if (itemPercent == 0f && drawbackPercent == 0f) {
            return contribution;
        }
        List<SkillTypeEffect> effects = new ArrayList<>(contribution.effects().size());
        for (SkillTypeEffect effect : contribution.effects()) {
            if (effect.effect().socketedItemScaling() != null) {
                effects.add(effect);
                continue;
            }
            float percent = itemPercent + (isDrawback(effect) ? drawbackPercent : 0f);
            effects.add(new SkillTypeEffect(effect.effect(), effect.magnitude() * Math.max(0f, 1f + percent / 100f)));
        }
        return contribution.withEffects(Collections.unmodifiableList(effects));
    }

    static boolean isDrawback(SkillTypeEffect effect) {
        return effect.effect().lowerIsBetter() ? effect.magnitude() > 0f : effect.magnitude() < 0f;
    }
}
