package exiledsector.items.crafting;

import exiledsector.items.Item;
import exiledsector.items.ItemTypes;
import exiledsector.items.UseVerdict;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public enum ForgeOperation {
    GRAFT {
        @Override
        public List<ForgeChoice> choices(Item item, ForgeInfusions.Source infusion) {
            return ItemForging.graftParts(item).stream().map(ForgeChoice::part).toList();
        }

        @Override
        boolean run(Item item, ForgeChoice choice, ForgeInfusions.Source infusion, Random random) {
            return ItemForging.graft(item, choice.part(), random) != null;
        }
    },
    EXCISE {
        @Override
        public List<ForgeChoice> choices(Item item, ForgeInfusions.Source infusion) {
            return ItemForging.excisable(item).stream().map(ForgeChoice::modifier).toList();
        }

        @Override
        boolean run(Item item, ForgeChoice choice, ForgeInfusions.Source infusion, Random random) {
            return ItemForging.excise(item, choice.modifier());
        }
    },
    PERFECT {
        @Override
        public List<ForgeChoice> choices(Item item, ForgeInfusions.Source infusion) {
            return ItemForging.perfectible(item).stream().map(ForgeChoice::modifier).toList();
        }

        @Override
        boolean run(Item item, ForgeChoice choice, ForgeInfusions.Source infusion, Random random) {
            return ItemForging.perfect(item, choice.modifier());
        }
    },
    INFUSE {
        @Override
        public List<ForgeChoice> choices(Item item, ForgeInfusions.Source infusion) {
            return ForgeInfusions.choices(item, infusion);
        }

        @Override
        String refusalReason(Item item, ForgeInfusions.Source infusion) {
            return ForgeInfusions.refusalReason(this, item, infusion);
        }

        @Override
        boolean run(Item item, ForgeChoice choice, ForgeInfusions.Source infusion, Random random) {
            return ForgeInfusions.infuse(item, infusion, random);
        }
    },
    BIOME_INFUSE {
        @Override
        public List<ForgeChoice> choices(Item item, ForgeInfusions.Source infusion) {
            return ForgeInfusions.choices(item, infusion);
        }

        @Override
        String refusalReason(Item item, ForgeInfusions.Source infusion) {
            return ForgeInfusions.refusalReason(this, item, infusion);
        }

        @Override
        boolean run(Item item, ForgeChoice choice, ForgeInfusions.Source infusion, Random random) {
            return ForgeInfusions.infuse(item, infusion, random);
        }
    };

    static final String REFUSAL_PREFIX = "ui.forge.refused.";

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ForgeOperation byIdOrNull(String id) {
        for (ForgeOperation operation : values()) {
            if (operation.id().equals(id)) {
                return operation;
            }
        }
        return null;
    }

    public abstract List<ForgeChoice> choices(Item item, ForgeInfusions.Source infusion);

    public List<ForgeChoice> choices(Item item) {
        return choices(item, ForgeInfusions.Source.NONE);
    }

    abstract boolean run(Item item, ForgeChoice choice, ForgeInfusions.Source infusion, Random random);

    String refusalReason(Item item, ForgeInfusions.Source infusion) {
        return choices(item, infusion).isEmpty() ? id() : null;
    }

    public boolean infuses() {
        return this == INFUSE || this == BIOME_INFUSE;
    }

    public boolean picksAChoice() {
        return !infuses();
    }

    public UseVerdict verdict(Item item) {
        return verdict(item, ForgeInfusions.Source.NONE);
    }

    public UseVerdict verdict(Item item, ForgeInfusions.Source infusion) {
        if (item == null) {
            return UseVerdict.refused(REFUSAL_PREFIX + "loadFirst");
        }
        if (!ItemTypes.takesCraftingItem(item.type())) {
            return UseVerdict.refused(REFUSAL_PREFIX + "itemType");
        }
        if (item.mirrored()) {
            return UseVerdict.refused(REFUSAL_PREFIX + "mirrored");
        }
        if (item.corrupted()) {
            return UseVerdict.refused(REFUSAL_PREFIX + "corrupted");
        }
        if (ItemCrafting.isLockedFromCraftingItem(item.definition())) {
            return UseVerdict.refused(REFUSAL_PREFIX + "uniqueSubroutine");
        }
        String reason = refusalReason(item, infusion);
        return reason == null ? UseVerdict.ALLOW : UseVerdict.refused(REFUSAL_PREFIX + reason);
    }

    public boolean apply(Item item, ForgeChoice choice, Random random) {
        return apply(item, choice, ForgeInfusions.Source.NONE, random);
    }

    public boolean apply(Item item, ForgeChoice choice, ForgeInfusions.Source infusion, Random random) {
        return verdict(item, infusion).allowed() && choice != null && choices(item, infusion).contains(choice) && run(item, choice, infusion, random);
    }
}
