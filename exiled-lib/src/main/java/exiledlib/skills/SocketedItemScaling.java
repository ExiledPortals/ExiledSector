package exiledlib.skills;

import exiledlib.i18n.StyledText;
import exiledlib.skills.skilleffect.EffectBacking;
import exiledlib.skills.skilleffect.EffectText;
import exiledlib.skills.skilleffect.SkillEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SocketedItemScaling(Scope scope, String slotId) implements EffectBacking {

    public enum Scope {
        ALL, TARGETED, SLOT, DRAWBACKS
    }

    private record Percents(float all, float drawbacks, Map<String, Float> targeted, Map<String, Float> slotScoped) {

        boolean none() {
            return all == 0f && drawbacks == 0f && targeted.isEmpty() && slotScoped.isEmpty();
        }

        float forSlot(String slotId) {
            return all + targeted.getOrDefault(slotId, 0f) + slotScoped.getOrDefault(slotId, 0f);
        }
    }

    public SocketedItemScaling(Scope scope) {
        this(scope, null);
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
        float all = 0f;
        float drawbacks = 0f;
        Map<String, Float> targeted = new HashMap<>();
        Map<String, Float> slotScoped = new HashMap<>();
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
                    case ALL -> all += effect.magnitude();
                    case DRAWBACKS -> drawbacks += effect.magnitude();
                    case SLOT -> slotScoped.merge(scaling.slotId(), effect.magnitude(), Float::sum);
                    case TARGETED -> {
                        if (targetSlotId != null) {
                            targeted.merge(targetSlotId, effect.magnitude(), Float::sum);
                        }
                    }
                }
            }
        }
        return new Percents(all, drawbacks, targeted, slotScoped);
    }

    private static EffectContribution scaled(EffectContribution contribution, SocketedItem item, Percents percents) {
        float itemPercent = percents.forSlot(item.slotId());
        List<SkillTypeEffect> effects = new ArrayList<>(contribution.effects().size());
        for (SkillTypeEffect effect : contribution.effects()) {
            if (effect.effect().socketedItemScaling() != null) {
                effects.add(effect);
                continue;
            }
            float percent = itemPercent + (isDrawback(effect) ? percents.drawbacks() : 0f);
            effects.add(new SkillTypeEffect(effect.effect(), effect.magnitude() * Math.max(0f, 1f + percent / 100f)));
        }
        return contribution.withEffects(Collections.unmodifiableList(effects));
    }

    static boolean isDrawback(SkillTypeEffect effect) {
        return effect.effect().lowerIsBetter() ? effect.magnitude() > 0f : effect.magnitude() < 0f;
    }
}
