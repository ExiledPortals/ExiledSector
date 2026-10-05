package exiledsector.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.layout.SkillNodeDecoration;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.skilleffect.FighterSkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillNodeTest {

    private MockedStatic<Global> globalMock;

    private static String description(SkillType type) {
        return SkillNode.describeTypeLines(type, null).stream().map(DescriptionLine::plain).collect(Collectors.joining("\n\n"));
    }

    @BeforeEach
    void setUp() {
        SkillTree.clearTypes();
        globalMock = Mockito.mockStatic(Global.class);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
        SkillTree.clearTypes();
    }

    @Test
    void descriptionListsTheHullSizesANodeIsRestrictedTo() {
        SkillType escort = new SkillType.Builder("escort", "Escort", "a.png", SkillTier.SMALL)
                .requiredHullSizes(List.of(HullSize.CRUISER, HullSize.DESTROYER)).build();
        SkillType frigateOnly = new SkillType.Builder("frigate_only", "Frigate Only", "a.png", SkillTier.SMALL)
                .requiredHullSizes(List.of(HullSize.FRIGATE)).build();

        assertEquals("Restricted to hull sizes: Destroyer, Cruiser.", description(escort));
        assertEquals("Restricted to hull size: Frigate.", description(frigateOnly));
    }

    @Test
    void descriptionDelegatesToTheTypesEffect() {
        SkillType type = new SkillType.Builder("hull", "Hull", "graphics/hullmods/reinforced_bulkheads.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .build();
        SkillNode node = new SkillNode("hull_1", type, List.of(), 0f, 0f);

        assertEquals("Increases hull points by 10%.", description(node.getType()));
    }

    @Test
    void descriptionStatesHowLongATemporaryAfterDeploymentNodeLasts() {
        SkillType type = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .temporaryAfterDeploymentSeconds(60f)
                .build();

        assertEquals("Increases hull points by 10%.\n\nThese effects only last for the first 60 seconds after the ship is deployed.",
                description(type));
    }

    @Test
    void descriptionJoinsMultipleEffectsOnSeparateLines() {
        SkillType type = new SkillType.Builder("heavyarmor", "Heavy Armor", "graphics/icons/notable_hullmods/heavy_armor.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 15f), new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 5f)))
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .build();
        SkillNode node = new SkillNode("heavyarmor_1", type, List.of(), 0f, 0f);

        assertEquals("Increases armor by 15%.\n\nIncreases hull points by 5%.", description(node.getType()));
    }

    @Test
    void descriptionPutsDeallocationWarningsLastRegardlessOfEffectOrder() {
        SkillType type = new SkillType.Builder("converted_hangar", "Converted Hangar", "graphics/icons/notable_hullmods/converted_hangar.png", SkillTier.KEYSTONE)
                .effects(List.of(new SkillTypeEffect(FighterSkillEffect.FIGHTER_BAYS_FLAT, 1f), new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 5f)))
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .build();
        SkillNode node = new SkillNode("converted_hangar_1", type, List.of(), 0f, 0f);

        assertEquals(
                "Increases number of fighter bays by 1.\n\nIncreases hull points by 5%.\n\nCannot be unallocated without at least 1 empty fighter bay.",
                description(node.getType()));
    }

    @Test
    void aSocketsLoreIsItsOnlyTextSoNothingSpoilsWhatCanBeInstalled() {
        SkillType type = new SkillType.Builder("socket", "Socket", "", SkillTier.SOCKET)
                .descriptionOverride("An empty room.")
                .build();

        NodeDescription description = SkillNode.describeType(type, null);

        assertEquals(List.of("An empty room."), description.effects().stream().map(DescriptionLine::plain).toList());
        assertTrue(description.details().isEmpty());
    }

    @Test
    void descriptionIsEmptyForATypeWithNoEffectsAndNoOverride() {
        SkillType type = new SkillType.Builder("cosmetic", "Cosmetic", "graphics/hullmods/flux_coil_adjunct.png", SkillTier.SMALL)
                .effects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .build();
        SkillNode node = new SkillNode("cosmetic_1", type, List.of(), 0f, 0f);

        assertEquals("", description(node.getType()));
    }

    @Test
    void descriptionOverrideIsFollowedByEffectLines() {
        SkillType type = new SkillType.Builder("hull", "Hull", "graphics/hullmods/reinforced_bulkheads.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .vanillaHullModId(null)
                .descriptionOverride("Custom flavor text.")
                .todo(null)
                .build();
        SkillNode node = new SkillNode("hull_1", type, List.of(), 0f, 0f);

        assertEquals("Custom flavor text.\n\nIncreases hull points by 10%.", description(node.getType()));
    }

    @Test
    void descriptionForAVanillaPassthroughTypeWithNoEffectsYetIsJustTheExclusivityLine() {
        SettingsAPI settings = mock(SettingsAPI.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getDisplayName()).thenReturn("Escort Package");
        when(settings.getHullModSpec("escort_package")).thenReturn(spec);

        SkillType type = new SkillType.Builder("escort_package", "Escort Package", "graphics/icons/notable_hullmods/escort_package.png", SkillTier.NOTABLE)
                .effects(List.of())
                .vanillaHullModId("escort_package")
                .descriptionOverride(null)
                .todo("Needs a real mechanic")
                .build();
        SkillNode node = new SkillNode("escort_package_1", type, List.of(), 0f, 0f);

        assertEquals("Mutually exclusive with hullmod:\n• Escort Package", description(node.getType()));
    }

    @Test
    void descriptionAppendsExclusivityLineAfterEffectsAndWarnings() {
        SettingsAPI settings = mock(SettingsAPI.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI armoredCladding = mock(HullModSpecAPI.class);
        when(armoredCladding.getDisplayName()).thenReturn("Armored Cladding");
        when(settings.getHullModSpec("armoredcladding")).thenReturn(armoredCladding);

        SkillType type = new SkillType.Builder("heavyarmor", "Heavy Armor", "graphics/icons/notable_hullmods/heavy_armor.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 15f)))
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of("armoredcladding"))
                .build();
        SkillNode node = new SkillNode("heavyarmor_1", type, List.of(), 0f, 0f);

        assertEquals("Increases armor by 15%.\n\nMutually exclusive with hullmod:\n• Armored Cladding", description(node.getType()));
    }

    @Test
    void aHullModFromAModThatIsNotInstalledIsLeftOutOfTheExclusivityLine() {
        SettingsAPI settings = mock(SettingsAPI.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI escortPackage = mock(HullModSpecAPI.class);
        when(escortPackage.getDisplayName()).thenReturn("Escort Package");
        when(settings.getHullModSpec("escort_package")).thenReturn(escortPackage);
        when(settings.getHullModSpec("unknown_hullmod")).thenReturn(null);

        SkillType type = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of("unknown_hullmod", "escort_package"))
                .build();
        SkillNode node = new SkillNode("hull_1", type, List.of(), 0f, 0f);

        assertEquals("Mutually exclusive with hullmod:\n• Escort Package", description(node.getType()));
    }

    @Test
    void descriptionIncludesSkillTypeExclusivityLine() {
        SkillType other = new SkillType.Builder("adaptiveshields", "Shield Conversion - Omni", "b.png", SkillTier.KEYSTONE)
                .effects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .build();
        SkillTree.registerType(other);

        SkillType type = new SkillType.Builder("frontemitter", "Shield Conversion - Front", "a.png", SkillTier.NOTABLE)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of())
                .exclusiveSkillTypeIds(List.of("adaptiveshields"))
                .build();
        SkillNode node = new SkillNode("frontemitter_1", type, List.of(), 0f, 0f);

        assertEquals("Mutually exclusive with node:\n• Shield Conversion - Omni", description(node.getType()));
    }

    @Test
    void descriptionListsANodeThatDeclaresTheExclusivityOnlyOnItsOwnSide() {
        SkillTree.registerType(new SkillType.Builder("adaptiveshields", "Shield Conversion - Omni", "a.png", SkillTier.NOTABLE)
                .exclusiveSkillTypeIds(List.of("frontemitter"))
                .build());
        SkillType type = new SkillType.Builder("frontemitter", "Shield Conversion - Front", "a.png", SkillTier.NOTABLE).build();
        SkillNode node = new SkillNode("frontemitter_1", type, List.of(), 0f, 0f);

        assertEquals("Mutually exclusive with node:\n• Shield Conversion - Omni", description(node.getType()));
    }

    @Test
    void descriptionFallsBackToRawIdWhenExclusiveSkillTypeIsUnknown() {
        SkillType type = new SkillType.Builder("frontemitter", "Shield Conversion - Front", "a.png", SkillTier.NOTABLE)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of())
                .exclusiveSkillTypeIds(List.of("unknown_type"))
                .build();
        SkillNode node = new SkillNode("frontemitter_1", type, List.of(), 0f, 0f);

        assertEquals("Mutually exclusive with node:\n• unknown_type", description(node.getType()));
    }

    @Test
    void descriptionCombinesHullModAndSkillTypeExclusivityAsSeparateLines() {
        SettingsAPI settings = mock(SettingsAPI.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getDisplayName()).thenReturn("Escort Package");
        when(settings.getHullModSpec("escort_package")).thenReturn(spec);

        SkillType other = new SkillType.Builder("adaptiveshields", "Shield Conversion - Omni", "b.png", SkillTier.KEYSTONE)
                .effects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .build();
        SkillTree.registerType(other);

        SkillType type = new SkillType.Builder("frontemitter", "Shield Conversion - Front", "a.png", SkillTier.NOTABLE)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of("escort_package"))
                .exclusiveSkillTypeIds(List.of("adaptiveshields"))
                .build();
        SkillNode node = new SkillNode("frontemitter_1", type, List.of(), 0f, 0f);

        assertEquals("Mutually exclusive with hullmod:\n• Escort Package\n\nMutually exclusive with node:\n• Shield Conversion - Omni", description(node.getType()));
    }

    @Test
    void descriptionPluralisesTheLabelWhenExclusiveWithSeveralHullMods() {
        SettingsAPI settings = mock(SettingsAPI.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        for (String[] hullMod : new String[][]{{"ground_support", "Ground Support"}, {"advanced_ground_support", "Advanced Ground Support"}}) {
            HullModSpecAPI spec = mock(HullModSpecAPI.class);
            when(spec.getDisplayName()).thenReturn(hullMod[1]);
            when(settings.getHullModSpec(hullMod[0])).thenReturn(spec);
        }
        SkillType type = new SkillType.Builder("ground_support", "Ground Support", "a.png", SkillTier.NOTABLE)
                .exclusiveHullModIds(List.of("ground_support", "advanced_ground_support"))
                .build();

        assertEquals("Mutually exclusive with hullmods:\n• Ground Support\n• Advanced Ground Support",
                description(type));
    }

    @Test
    void descriptionListsAHullModAndANodeSharingANameSeparately() {
        SettingsAPI settings = mock(SettingsAPI.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI spec = mock(HullModSpecAPI.class);
        when(spec.getDisplayName()).thenReturn("Shield Shunt");
        when(settings.getHullModSpec("shield_shunt")).thenReturn(spec);

        SkillType other = new SkillType.Builder("shield_shunt", "Shield Shunt", "b.png", SkillTier.NOTABLE)
                .effects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .build();
        SkillTree.registerType(other);

        SkillType type = new SkillType.Builder("frontemitter", "Shield Conversion - Front", "a.png", SkillTier.NOTABLE)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .optionalOptionIds(List.of())
                .exclusiveHullModIds(List.of("shield_shunt"))
                .exclusiveSkillTypeIds(List.of("shield_shunt"))
                .build();
        SkillNode node = new SkillNode("frontemitter_1", type, List.of(), 0f, 0f);

        assertEquals("Mutually exclusive with hullmod:\n• Shield Shunt\n\nMutually exclusive with node:\n• Shield Shunt", description(node.getType()));
    }

    @Test
    void resolveEffectiveTypeReturnsItsOwnTypeWhenNotOptional() {
        SkillType type = new SkillType.Builder("hull", "Hull", "graphics/hullmods/reinforced_bulkheads.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .build();
        SkillNode node = new SkillNode("hull_1", type, List.of(), 0f, 0f);

        assertSame(type, node.resolveEffectiveType(new ShipSkillData()));
    }

    @Test
    void resolveEffectiveTypeReturnsThePlaceholderWhenOptionalAndUnselected() {
        SkillType placeholder = new SkillType.Builder("slot", "Optional Skill", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .optionalOptionIds(List.of("hull", "armor"))
                .build();
        SkillNode node = new SkillNode("slot_1", placeholder, List.of(), 0f, 0f);

        assertSame(placeholder, node.resolveEffectiveType(new ShipSkillData()));
    }

    @Test
    void resolveEffectiveTypeReturnsTheSelectedOptionsTypeOnceChosen() {
        SkillType placeholder = new SkillType.Builder("slot", "Optional Skill", "a.png", SkillTier.SMALL)
                .effects(List.of())
                .hullSizeEffects(List.of())
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .optionalOptionIds(List.of("hull"))
                .build();
        SkillType hullOption = new SkillType.Builder("hull", "Hull", "graphics/hullmods/reinforced_bulkheads.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f)))
                .vanillaHullModId(null)
                .descriptionOverride(null)
                .todo(null)
                .build();
        SkillTree.registerType(hullOption);
        SkillNode node = new SkillNode("slot_1", placeholder, List.of(), 0f, 0f);
        ShipSkillData data = new ShipSkillData();
        data.selectOption(node, hullOption, 1);

        assertSame(hullOption, node.resolveEffectiveType(data));
    }

    private static SkillType taggedType(String id, List<String> optionIds, List<String> tags) {
        return new SkillType.Builder(id, id, "a.png", SkillTier.SMALL)
                .optionalOptionIds(optionIds)
                .tags(tags)
                .build();
    }

    private static SkillNode taggedNode(SkillType type, List<String> tags) {
        return new SkillNode(type.getId() + "_1", type, List.of(), 0f, 0f, SkillNodeDecoration.NONE, tags);
    }

    @Test
    void nodeTagsDefaultToEmptyWhenNotGivenOrNull() {
        SkillType type = taggedType("hull", List.of(), List.of());

        assertEquals(List.of(), new SkillNode("hull_1", type, List.of(), 0f, 0f).getTags());
        assertEquals(List.of(), taggedNode(type, null).getTags());
    }

    @Test
    void effectiveTagsListNodeTagsThenTypeTagsWithoutDuplicates() {
        SkillType type = taggedType("hull", List.of(), List.of("hull", "req_shields"));
        SkillNode node = taggedNode(type, List.of("hegemony", "hull"));

        assertEquals(List.of("hegemony", "hull", "req_shields"), List.copyOf(node.effectiveTags(new ShipSkillData())));
    }

    @Test
    void effectiveTagsIncludeTheChosenOptionsTagsOnceSelected() {
        SkillType hullOption = taggedType("hull", List.of(), List.of("hull", "req_ballistic"));
        SkillTree.registerType(hullOption);
        SkillType placeholder = taggedType("slot", List.of("hull"), List.of("hull"));
        SkillNode node = taggedNode(placeholder, List.of("pirate"));
        ShipSkillData data = new ShipSkillData();
        data.selectOption(node, hullOption, 1);

        assertEquals(List.of("pirate", "hull", "req_ballistic"), List.copyOf(node.effectiveTags(data)));
    }

    @Test
    void effectiveTagsOfAnUnselectedOptionalNodeAreJustTheNodeAndPlaceholderTags() {
        SkillType hullOption = taggedType("hull", List.of(), List.of("req_ballistic"));
        SkillTree.registerType(hullOption);
        SkillType placeholder = taggedType("slot", List.of("hull"), List.of("hull"));
        SkillNode node = taggedNode(placeholder, List.of("pirate"));

        assertEquals(List.of("pirate", "hull"), List.copyOf(node.effectiveTags(new ShipSkillData())));
        assertEquals(List.of("pirate", "hull"), List.copyOf(node.effectiveTags((ShipSkillData) null)));
    }

    @Test
    void effectiveTagsForAKnownOptionIncludeThatOptionsTags() {
        SkillType hullOption = taggedType("hull", List.of(), List.of("req_ballistic"));
        SkillType placeholder = taggedType("slot", List.of("hull"), List.of("hull"));
        SkillNode node = taggedNode(placeholder, List.of("pirate"));

        assertEquals(List.of("pirate", "hull", "req_ballistic"), List.copyOf(node.effectiveTags(hullOption)));
        assertEquals(List.of("pirate", "hull"), List.copyOf(node.effectiveTags((SkillType) null)));
    }

    @Test
    void effectiveTagsCannotBeModified() {
        SkillNode node = taggedNode(taggedType("hull", List.of(), List.of("hull")), List.of("core"));
        Set<String> tags = node.effectiveTags((SkillType) null);

        assertThrows(UnsupportedOperationException.class, () -> tags.add("shield"));
    }

    @Test
    void anOptionalNodeOnlyResolvesToAChosenOptionItStillOffers() {
        SkillType hull = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL).effects(List.of()).build();
        SkillType armor = new SkillType.Builder("armor", "Armor", "a.png", SkillTier.SMALL).effects(List.of()).build();
        SkillTree.registerType(hull);
        SkillTree.registerType(armor);
        SkillType slot = new SkillType.Builder("slot", "Slot", "a.png", SkillTier.SMALL).effects(List.of()).optionalOptionIds(List.of("hull")).build();
        SkillNode listed = new SkillNode("listed", slot, List.of(), 0f, 0f);
        SkillNode unlisted = new SkillNode("unlisted", slot, List.of(), 0f, 0f);
        ShipSkillData data = new ShipSkillData();
        data.selectOption(listed, hull, 0);
        data.selectOption(unlisted, armor, 0);

        assertSame(hull, listed.resolveEffectiveType(data));
        assertSame(slot, unlisted.resolveEffectiveType(data));
    }

    @Test
    void effectsAndRestrictionsAreDescribedSeparately() {
        SettingsAPI settings = mock(SettingsAPI.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI armoredCladding = mock(HullModSpecAPI.class);
        when(armoredCladding.getDisplayName()).thenReturn("Armored Cladding");
        when(settings.getHullModSpec("armoredcladding")).thenReturn(armoredCladding);

        SkillType type = new SkillType.Builder("heavyarmor", "Heavy Armor", "a.png", SkillTier.NOTABLE)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.ARMOR_PERCENT, 15f)))
                .requiredHullSizes(List.of(HullSize.FRIGATE))
                .exclusiveHullModIds(List.of("armoredcladding"))
                .build();

        NodeDescription description = SkillNode.describeType(type, null);

        assertEquals(List.of("Increases armor by 15%."), description.effects().stream().map(DescriptionLine::plain).toList());
        assertEquals(List.of("Restricted to hull size: Frigate.", "Mutually exclusive with hullmod:\n• Armored Cladding"),
                description.details().stream().map(DescriptionLine::plain).toList());
    }

    @Test
    void aNodePlacingAPhantomCopyStillNamesTheRealHullModItExcludes() {
        SettingsAPI settings = mock(SettingsAPI.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        HullModSpecAPI navRelay = mock(HullModSpecAPI.class);
        when(navRelay.getDisplayName()).thenReturn("Nav Relay");
        when(settings.getHullModSpec("nav_relay")).thenReturn(navRelay);
        PhantomHullModStatus.markActive("nav_relay");
        try {
            SkillType type = new SkillType.Builder("nav_relay", "Nav Relay", "a.png", SkillTier.NOTABLE)
                    .phantomHullModIds(List.of("nav_relay"))
                    .build();

            assertEquals("Mutually exclusive with hullmod:\n• Nav Relay", description(type));
        } finally {
            PhantomHullModStatus.clear();
        }
    }
}
