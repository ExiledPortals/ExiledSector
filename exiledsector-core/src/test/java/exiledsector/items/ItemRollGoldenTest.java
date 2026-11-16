package exiledsector.items;

import exiledsector.modifiers.ModifierSlot;
import exiledsector.modifiers.TierRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemRollGoldenTest {

    private static final int SEEDS = 48;
    private static final String CHART_WITH_MINIMUM_TIER = "minimum_tier_chart";
    private static final int CHART_MINIMUM_TIER = 5;
    private static final AffixLayout.Limits HULL_DROP_LIMITS = new AffixLayout.Limits(2, 2, AffixLayout.MAX_PER_SIDE, List.of(0.64f));
    private static final AffixLayout.Limits CHART_LIMITS = new AffixLayout.Limits(2, 6, 3, List.of(0.2f, 0.25f, 0.2f, 0.15f, 0.1f));
    private static final List<Integer> TIERS = List.of(1, 4, 5, 9);
    private static final List<Float> RARITIES = List.of(0f, 0.5f, 1f, 2f, 4f);
    private static final List<Integer> MODIFIER_COUNTS = List.of(1, 2, 3, 4, 5);

    private static final List<PoolEntry> PREFIXES = List.of(
            PoolEntry.of(ModifierSlot.PREFIX, "FLUX_CAPACITY_MULT", 4f, 6f, 1f),
            PoolEntry.of(ModifierSlot.PREFIX, "FLUX_DISSIPATION_MULT", 4f, 6f, 2f),
            PoolEntry.of(ModifierSlot.PREFIX, "BEAM_WEAPON_DAMAGE_PERCENT", 10f, 15f, 1f).withTiers(TierRange.atLeast(5)),
            PoolEntry.of(ModifierSlot.PREFIX, "ARMOR_FLAT", 20f, 40f, 3f));
    private static final List<PoolEntry> SUFFIXES = List.of(
            PoolEntry.of(ModifierSlot.SUFFIX, "HULL_MULT", 4f, 6f, 1f),
            PoolEntry.of(ModifierSlot.SUFFIX, "ARMOR_PERCENT", 6f, 9f, 1.5f),
            PoolEntry.of(ModifierSlot.SUFFIX, "SHIELD_DAMAGE_TAKEN_MULT", -15f, -10f, 1f).withTiers(TierRange.between(1, 4)),
            PoolEntry.of(ModifierSlot.SUFFIX, "HULL_PERCENT", 5f, 10f, 2f));

    private static final ItemDefinition SUBROUTINE = new ItemDefinition("golden_chip", ItemKind.SUBROUTINE, "Chip", ItemDefinition.FALLBACK_ICON, 1f,
            false, "", PREFIXES, SUFFIXES, "");
    private static final ItemDefinition HULL_ITEM = new ItemDefinition("golden_plate", ItemKind.ARMOR_PLATING, "Plate", ItemDefinition.FALLBACK_ICON,
            1f, false, "", PREFIXES, SUFFIXES, "");
    private static final ItemDefinition UNIQUE = new ItemDefinition("golden_relic", ItemKind.REACTOR, "Relic", ItemDefinition.FALLBACK_ICON, 1f,
            true, "", PREFIXES.subList(0, 2), SUFFIXES.subList(0, 2), "");
    private static final ItemDefinition KEEPS_ONE = new ItemDefinition("golden_one_of", ItemKind.BRIDGE, "One Of", ItemDefinition.FALLBACK_ICON, 1f,
            true, "", PREFIXES, SUFFIXES, "").withMechanic(ItemDefinition.ONE_OF_MECHANIC);
    private static final ItemDefinition CHART = chart("golden_chart");
    private static final ItemDefinition CHART_FROM_TIER_FIVE = chart(CHART_WITH_MINIMUM_TIER);
    private static final List<ItemDefinition> DEFINITIONS = List.of(SUBROUTINE, HULL_ITEM, UNIQUE, KEEPS_ONE, CHART, CHART_FROM_TIER_FIVE);

    private interface ModifierRoll {
        List<RolledModifier> roll(ItemDefinition definition, long seed);
    }

    private interface DataRoll {
        ItemData roll(ItemDefinition definition, long seed);
    }

    private static ItemDefinition chart(String definitionId) {
        return new ItemDefinition(definitionId, null, "Chart", ItemDefinition.FALLBACK_ICON, 1f, false, "", PREFIXES, SUFFIXES, "",
                ItemType.STAR_CHART, null, List.of(), ItemDefinition.NO_SOCKET_LIMIT);
    }

    @BeforeEach
    void registerTypes() {
        ItemTypes.register(new BasicItemTypeSupport(ItemType.HULL_ITEM, Item.class, "golden.HullItem", UnresolvedItem::new, null) {
            @Override
            public AffixLayout.Limits dropLimits() {
                return HULL_DROP_LIMITS;
            }
        });
        ItemTypes.register(new BasicItemTypeSupport(ItemType.STAR_CHART, Item.class, "golden.StarChart", UnresolvedItem::new, null) {
            @Override
            public boolean tiered() {
                return true;
            }

            @Override
            public AffixLayout.Limits affixLimits() {
                return CHART_LIMITS;
            }

            @Override
            public int lowestTier(String definitionId) {
                return CHART_WITH_MINIMUM_TIER.equals(definitionId) ? CHART_MINIMUM_TIER : ItemTraits.LOWEST_TIER;
            }

            @Override
            public int rolledTier(String definitionId, int tier) {
                return Math.max(lowestTier(definitionId), tier);
            }

            @Override
            public ItemTraits traitsAtTier(ItemDefinition definition, long seed, int tier) {
                return new ItemTraits(tier, 0, null, List.of(RolledModifier.single(ModifierSlot.IMPLICIT, new RolledLine("HULL_MULT", seed % 7))));
            }
        });
        ItemNames.useTypeWords(ItemType.SUBROUTINE, new ItemNames.NameWords(ItemNames.Style.CODENAME, List.of("Iron", "Ash", "Vane"),
                List.of("Wake", "Spire", "Gale"), List.of("Mk I", "Mk II")));
        ItemNames.useTypeWords(ItemType.HULL_ITEM, new ItemNames.NameWords(ItemNames.Style.PRODUCT, List.of("Kite", "Lance"),
                List.of("Works", "Yards"), List.of("A", "B", "C")));
        ItemDefinitions.register(DEFINITIONS);
    }

    @AfterEach
    void restore() {
        ItemTypes.unregister(ItemType.HULL_ITEM);
        ItemTypes.unregister(ItemType.STAR_CHART);
        ItemNames.clear();
        ItemDefinitions.clear();
    }

    @Test
    void aPlainRollStaysSeedIdentical() {
        assertEquals("P=FLUX_DISSIPATION_MULT:5;P=ARMOR_FLAT:35;S=HULL_PERCENT:7;S=ARMOR_PERCENT:6", modifiers(ItemRoller.roll(SUBROUTINE, 7L)));
        assertEquals("35fae2fdec0fd2100ca2105b", digestOfModifiers((definition, seed) -> ItemRoller.roll(definition, seed)));
    }

    @Test
    void aRollAtARawTierStaysSeedIdentical() {
        StringBuilder rolls = new StringBuilder();
        for (int tier : TIERS) {
            rolls.append(modifierRolls((definition, seed) -> ItemRoller.rollResolved(definition, seed, ItemRoll.atTier(tier))));
        }
        assertEquals("7d564176578e141dbb64740a", digest(rolls));
    }

    @Test
    void aRollAtATierStaysSeedIdentical() {
        StringBuilder rolls = new StringBuilder();
        for (int tier : TIERS) {
            rolls.append(modifierRolls((definition, seed) -> ItemRoller.roll(definition, seed, ItemRoll.atTier(tier))));
        }
        assertEquals("2d8fc6cc9fcf5d441f6ad55c", digest(rolls));
    }

    @Test
    void aRollAtARarityWithDropLimitsStaysSeedIdentical() {
        StringBuilder rolls = new StringBuilder();
        for (float rarity : RARITIES) {
            rolls.append(modifierRolls((definition, seed) -> ItemRoller.roll(definition, seed, ItemRoll.dropAtRarity(rarity))));
        }
        assertEquals("47306a99ecc22a6564ba6d56", digest(rolls));
    }

    @Test
    void aRollAtATierAndRarityWithAffixLimitsStaysSeedIdentical() {
        StringBuilder rolls = new StringBuilder();
        for (int tier : TIERS) {
            for (float rarity : RARITIES) {
                rolls.append(modifierRolls((definition, seed) -> ItemRoller.roll(definition, seed, ItemRoll.atTier(tier).withRarity(rarity))));
            }
        }
        assertEquals("0fb677aafffb12a05debfdb9", digest(rolls));
    }

    @Test
    void aRollWithAModifierCountStaysSeedIdentical() {
        StringBuilder rolls = new StringBuilder();
        for (int modifierCount : MODIFIER_COUNTS) {
            rolls.append(modifierRolls((definition, seed) -> ItemRoller.roll(definition, seed, ItemRoll.withModifierCount(modifierCount))));
        }
        assertEquals("b17e56208f98b20dd730b4c7", digest(rolls));
    }

    @Test
    void aRollPastTheLimitsStaysSeedIdentical() {
        StringBuilder rolls = new StringBuilder();
        for (int tier : TIERS) {
            for (int modifierCount : List.of(4, 6)) {
                for (int maxPerSide : List.of(2, 3)) {
                    rolls.append(modifierRolls((definition, seed) -> ItemRoller.rollPastLimits(definition, seed, tier, modifierCount,
                            maxPerSide)));
                }
            }
        }
        assertEquals("f9b17485eb52ad0a70f0716c", digest(rolls));
    }

    @Test
    void aPlainItemStaysSeedIdentical() {
        assertEquals("golden_plate#7>P=FLUX_DISSIPATION_MULT:5;P=ARMOR_FLAT:35;S=HULL_PERCENT:7;S=ARMOR_PERCENT:6"
                + " | FrozenName[null, null, Kite, Works, null, null, product] | none", item(ItemData.rolled(HULL_ITEM, 7L)));
        assertEquals("minimum_tier_chart#7>P=FLUX_DISSIPATION_MULT:5;P=ARMOR_FLAT:35;P=BEAM_WEAPON_DAMAGE_PERCENT:12;S=ARMOR_PERCENT:6;S=HULL_PERCENT:5;S=HULL_MULT:6"
                + " | null | tier 5 quantity 0 corruption null implicits I=HULL_MULT:0", item(ItemData.rolled(CHART_FROM_TIER_FIVE, 7L)));
        assertEquals("77b95d7d258e36fe5902726b", digest(itemRolls((definition, seed) -> ItemData.rolled(definition, seed))));
    }

    @Test
    void anItemAtARarityWithDropLimitsStaysSeedIdentical() {
        StringBuilder rolls = new StringBuilder();
        for (float rarity : RARITIES) {
            rolls.append(itemRolls((definition, seed) -> ItemData.rolled(definition, seed, ItemRoll.dropAtRarity(rarity))));
        }
        assertEquals("0e959616f2e1253dd4a5ae5a", digest(rolls));
    }

    @Test
    void anItemWithAModifierCountStaysSeedIdentical() {
        assertEquals("golden_plate#7>P=FLUX_DISSIPATION_MULT:5;S=HULL_PERCENT:8"
                + " | FrozenName[FLUX_DISSIPATION_MULT, HULL_PERCENT, null, null, null, null] | none", item(ItemData.rolled(HULL_ITEM, 7L, ItemRoll.withModifierCount(2))));
        assertEquals("golden_chart#7>P=FLUX_DISSIPATION_MULT:5;P=ARMOR_FLAT:35;P=BEAM_WEAPON_DAMAGE_PERCENT:12;S=ARMOR_PERCENT:6;S=HULL_PERCENT:5;S=HULL_MULT:6"
                + " | null | none", item(ItemData.rolled(CHART, 7L, ItemRoll.withModifierCount(2))));
        StringBuilder rolls = new StringBuilder();
        for (int modifierCount : MODIFIER_COUNTS) {
            rolls.append(itemRolls((definition, seed) -> ItemData.rolled(definition, seed, ItemRoll.withModifierCount(modifierCount))));
        }
        assertEquals("d97e0257300f4f54bcdb6f3e", digest(rolls));
    }

    @Test
    void anItemAtATierStaysSeedIdentical() {
        assertEquals("minimum_tier_chart#7>P=FLUX_DISSIPATION_MULT:5;P=ARMOR_FLAT:35;P=BEAM_WEAPON_DAMAGE_PERCENT:12;S=ARMOR_PERCENT:6;S=HULL_PERCENT:5;S=HULL_MULT:6"
                + " | null | tier 5 quantity 0 corruption null implicits I=HULL_MULT:0", item(ItemData.rolled(CHART_FROM_TIER_FIVE, 7L, ItemRoll.atTier(1))));
        StringBuilder rolls = new StringBuilder();
        for (int tier : TIERS) {
            rolls.append(itemRolls((definition, seed) -> ItemData.rolled(definition, seed, ItemRoll.atTier(tier))));
        }
        assertEquals("3a6f4db279144215ca8da8b7", digest(rolls));
    }

    @Test
    void anItemAtATierAndRarityWithAffixLimitsStaysSeedIdentical() {
        assertEquals("golden_plate#7>P=FLUX_DISSIPATION_MULT:5;P=ARMOR_FLAT:35;S=HULL_PERCENT:7;S=ARMOR_PERCENT:6"
                + " | FrozenName[null, null, Kite, Works, null, null, product] | none", item(ItemData.rolled(HULL_ITEM, 7L, ItemRoll.atTier(5).withRarity(4f))));
        StringBuilder rolls = new StringBuilder();
        for (int tier : TIERS) {
            for (float rarity : RARITIES) {
                rolls.append(itemRolls((definition, seed) -> ItemData.rolled(definition, seed, ItemRoll.atTier(tier).withRarity(rarity))));
            }
        }
        assertEquals("8316832d35763f7d4d872357", digest(rolls));
    }

    @Test
    void aSavedItemWithoutModifiersRerollsThemFromItsSeed() {
        StringBuilder rolls = new StringBuilder();
        rolls.append(itemRolls((definition, seed) -> ItemData.of(new ItemData(definition.id(), seed).preview())));
        for (int tier : TIERS) {
            rolls.append(itemRolls((definition, seed) -> ItemData.of(new ItemData(definition.id(), seed, null, null, ItemTraits.ofTier(tier)).preview())));
        }
        assertEquals("360817f5889405eaf55a57ab", digest(rolls));
    }

    @Test
    void aSavedItemRerollsTheModifiersItWasRolledWith() {
        for (ItemDefinition definition : DEFINITIONS) {
            for (long seed = 0; seed < SEEDS; seed++) {
                ItemData plain = ItemData.rolled(definition, seed);
                assertEquals(modifiers(plain.explicits()), modifiers(new ItemData(definition.id(), seed).preview().explicits()), definition.id() + " " + seed);
                for (int tier : TIERS) {
                    ItemData tiered = ItemData.rolled(definition, seed, ItemRoll.atTier(tier));
                    if (tiered.traits() != null) {
                        List<RolledModifier> rerolled = new ItemData(definition.id(), seed, null, null, ItemTraits.ofTier(tiered.traits().tier())).preview().explicits();
                        assertEquals(modifiers(tiered.explicits()), modifiers(rerolled), definition.id() + " " + seed + " tier " + tier);
                    }
                }
            }
        }
    }

    private static StringBuilder modifierRolls(ModifierRoll roll) {
        StringBuilder rolls = new StringBuilder();
        for (ItemDefinition definition : DEFINITIONS) {
            for (long seed = 0; seed < SEEDS; seed++) {
                rolls.append(definition.id()).append('#').append(seed).append('>').append(modifiers(roll.roll(definition, seed))).append('\n');
            }
        }
        return rolls;
    }

    private static StringBuilder itemRolls(DataRoll roll) {
        StringBuilder rolls = new StringBuilder();
        for (ItemDefinition definition : DEFINITIONS) {
            for (long seed = 0; seed < SEEDS; seed++) {
                rolls.append(item(roll.roll(definition, seed))).append('\n');
            }
        }
        return rolls;
    }

    private static String digestOfModifiers(ModifierRoll roll) {
        return digest(modifierRolls(roll));
    }

    private static String modifiers(List<RolledModifier> modifiers) {
        return modifiers == null ? "null" : ItemCodec.modifiers(modifiers);
    }

    private static String item(ItemData item) {
        ItemTraits traits = item.traits();
        String traitsText = traits == null ? "none" : "tier " + traits.tier() + " quantity " + traits.quantity() + " corruption " + traits.corruption()
                + " implicits " + modifiers(traits.implicits()) + (traits.mirrored() ? " mirrored" : "");
        return item.definitionId() + "#" + item.seed() + ">" + modifiers(item.explicits()) + " | " + item.frozenName() + " | " + traitsText;
    }

    private static String digest(CharSequence rolls) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(rolls.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
