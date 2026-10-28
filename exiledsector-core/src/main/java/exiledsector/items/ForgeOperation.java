package exiledsector.items;

import java.util.List;
import java.util.Locale;
import java.util.Random;

public enum ForgeOperation {
    GRAFT {
        @Override
        public List<ForgeChoice> choices(Item item) {
            return ItemForging.graftParts(item).stream().map(ForgeChoice::part).toList();
        }

        @Override
        boolean run(Item item, ForgeChoice choice, Random random) {
            return ItemForging.graft(item, choice.part(), random) != null;
        }
    },
    EXCISE {
        @Override
        public List<ForgeChoice> choices(Item item) {
            return ItemForging.excisable(item).stream().map(ForgeChoice::modifier).toList();
        }

        @Override
        boolean run(Item item, ForgeChoice choice, Random random) {
            return ItemForging.excise(item, choice.modifier());
        }
    },
    PERFECT {
        @Override
        public List<ForgeChoice> choices(Item item) {
            return ItemForging.perfectible(item).stream().map(ForgeChoice::modifier).toList();
        }

        @Override
        boolean run(Item item, ForgeChoice choice, Random random) {
            return ItemForging.perfect(item, choice.modifier());
        }
    },
    ANCHOR_REROLL {
        @Override
        public List<ForgeChoice> choices(Item item) {
            return ItemForging.anchors(item).stream().map(ForgeChoice::modifier).toList();
        }

        @Override
        boolean run(Item item, ForgeChoice choice, Random random) {
            return ItemForging.anchorReroll(item, choice.modifier(), random);
        }
    },
    RETEMPER {
        @Override
        public List<ForgeChoice> choices(Item item) {
            return ItemForging.canRetemper(item) ? List.of(ForgeChoice.WHOLE_ITEM) : List.of();
        }

        @Override
        boolean run(Item item, ForgeChoice choice, Random random) {
            return ItemForging.retemper(item, random);
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

    public abstract List<ForgeChoice> choices(Item item);

    abstract boolean run(Item item, ForgeChoice choice, Random random);

    public boolean picksAChoice() {
        return this != RETEMPER;
    }

    public UseVerdict verdict(Item item) {
        if (item == null) {
            return UseVerdict.refused(REFUSAL_PREFIX + "loadFirst");
        }
        if (!ItemCrafting.takesCurrency(item.type())) {
            return UseVerdict.refused(REFUSAL_PREFIX + "itemType");
        }
        if (item.mirrored()) {
            return UseVerdict.refused(REFUSAL_PREFIX + "mirrored");
        }
        if (item.corrupted()) {
            return UseVerdict.refused(REFUSAL_PREFIX + "corrupted");
        }
        if (ItemCrafting.isLockedFromCurrency(item.definition())) {
            return UseVerdict.refused(REFUSAL_PREFIX + "uniqueSubroutine");
        }
        return choices(item).isEmpty() ? UseVerdict.refused(REFUSAL_PREFIX + id()) : UseVerdict.ALLOWED;
    }

    public boolean apply(Item item, ForgeChoice choice, Random random) {
        return verdict(item).allowed() && choice != null && choices(item).contains(choice) && run(item, choice, random);
    }
}
