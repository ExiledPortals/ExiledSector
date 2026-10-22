package exiledsector.ui.cargo;

import exiledsector.items.CraftingCurrency;
import exiledsector.items.CraftingWard;
import exiledsector.items.ItemDisassembly;
import exiledsector.items.MaterialTier;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CargoIconFxTest {

    @Test
    void hashesStayInTheUnitRangeAndRepeatForTheSameInputs() {
        for (int seed = -50; seed < 50; seed++) {
            float value = CargoIconFx.hash(seed, seed * 3, seed * 7);
            assertTrue(value >= 0f && value < 1f);
            assertEquals(value, CargoIconFx.hash(seed, seed * 3, seed * 7));
        }
        assertTrue(CargoIconFx.hash(1, 2, 3) != CargoIconFx.hash(1, 2, 4));
    }

    @Test
    void theHeartbeatBeatsTwiceThenRests() {
        assertTrue(CargoIconFx.heartbeat(0.12f) > 0.99f);
        assertTrue(CargoIconFx.heartbeat(0.42f) > 0.69f);
        assertTrue(CargoIconFx.heartbeat(0.27f) < 0.2f);
        assertTrue(CargoIconFx.heartbeat(1.5f) < 0.01f);
        assertEquals(CargoIconFx.heartbeat(0.12f), CargoIconFx.heartbeat(0.12f + 2.6f), 1e-4f);
    }

    @Test
    void thePerimeterWalksTheSquareEdgesInOrder() {
        assertEquals(-1f, CargoIconFx.perimeterX(0f));
        assertEquals(-1f, CargoIconFx.perimeterY(0f));
        assertEquals(1f, CargoIconFx.perimeterX(1f));
        assertEquals(-1f, CargoIconFx.perimeterY(1f));
        assertEquals(1f, CargoIconFx.perimeterX(2f));
        assertEquals(1f, CargoIconFx.perimeterY(2f));
        assertEquals(-1f, CargoIconFx.perimeterX(3f));
        assertEquals(1f, CargoIconFx.perimeterY(3f));
        assertEquals(CargoIconFx.perimeterX(0.5f), CargoIconFx.perimeterX(4.5f), 1e-5f);
        assertEquals(CargoIconFx.perimeterY(-0.5f), CargoIconFx.perimeterY(3.5f), 1e-5f);
    }

    @Test
    void looksGetLouderWithEachTier() {
        Color color = Color.CYAN;
        CargoIconLook basic = CargoIconLooks.forMaterial(MaterialTier.BASIC, color);
        CargoIconLook minor = CargoIconLooks.forMaterial(MaterialTier.MINOR, color);
        CargoIconLook major = CargoIconLooks.forMaterial(MaterialTier.MAJOR, color);
        CargoIconLook exotic = CargoIconLooks.forMaterial(MaterialTier.EXOTIC, color);
        CargoIconLook mythic = CargoIconLooks.forMaterial(MaterialTier.MYTHIC, color);

        assertFalse(CargoIconLooks.celebratesDrop(basic));
        assertTrue(CargoIconLooks.celebratesDrop(minor));
        assertTrue(minor.glow() < major.glow() && major.glow() < exotic.glow() && exotic.glow() < mythic.glow());
        assertFalse(minor.rings());
        assertTrue(major.rings() && !major.arcs());
        assertTrue(exotic.arcs() && exotic.heartbeat() && !exotic.prismatic());
        assertTrue(mythic.prismatic());
    }

    @Test
    void eachMaterialGetsItsOwnSwarmMotion() {
        assertEquals(SwarmMotion.SLURRY, CargoIconLooks.swarmFor(ItemDisassembly.PARTS_ITEM_ID));
        assertEquals(SwarmMotion.ORBIT, CargoIconLooks.swarmFor(CraftingCurrency.AUGMENTATION.itemId()));
        assertEquals(SwarmMotion.FERAL, CargoIconLooks.swarmFor(CraftingCurrency.CORRUPTION.itemId()));
        assertEquals(SwarmMotion.REPLICATING, CargoIconLooks.swarmFor(CraftingCurrency.REPLICATION.itemId()));
        assertEquals(SwarmMotion.CONTAINED, CargoIconLooks.swarmFor(CraftingWard.PREFIX.itemId()));
        assertEquals(SwarmMotion.NONE, CargoIconLook.outlined(Color.WHITE).swarm(), "hull items have no swarm");
    }

    @Test
    void busierAndFasterSwarmsAtHigherTiers() {
        Color color = Color.CYAN;
        CargoIconLook minor = CargoIconLooks.forMaterial(MaterialTier.MINOR, color);
        CargoIconLook major = CargoIconLooks.forMaterial(MaterialTier.MAJOR, color);
        CargoIconLook exotic = CargoIconLooks.forMaterial(MaterialTier.EXOTIC, color);
        CargoIconLook mythic = CargoIconLooks.forMaterial(MaterialTier.MYTHIC, color);

        assertTrue(CargoIconFx.speckCount(minor) < CargoIconFx.speckCount(major));
        assertTrue(CargoIconFx.speckCount(major) < CargoIconFx.speckCount(exotic));
        assertTrue(CargoIconFx.speckCount(exotic) < CargoIconFx.speckCount(mythic));
        assertTrue(CargoIconFx.swarmSpeed(minor) < CargoIconFx.swarmSpeed(mythic));
    }

    @Test
    void theAnimationSettingFallsBackToFull() {
        assertEquals(ItemAnimationSetting.Mode.CALM, ItemAnimationSetting.parse("Calm"));
        assertEquals(ItemAnimationSetting.Mode.OFF, ItemAnimationSetting.parse("off"));
        assertEquals(ItemAnimationSetting.Mode.FULL, ItemAnimationSetting.parse("Full"));
        assertEquals(ItemAnimationSetting.Mode.FULL, ItemAnimationSetting.parse(null));
    }

    @Test
    void dropAnnouncementsUseTheRarestMaterialFound() {
        assertNull(MaterialDropAnnouncer.bestTier(Map.of()));
        assertEquals(MaterialTier.BASIC, MaterialDropAnnouncer.bestTier(Map.of(ItemDisassembly.PARTS_ITEM_ID, 4)));
        assertEquals(MaterialTier.EXOTIC, MaterialDropAnnouncer.bestTier(Map.of(ItemDisassembly.PARTS_ITEM_ID, 4,
                CraftingCurrency.CORRUPTION.itemId(), 1, CraftingCurrency.AUGMENTATION.itemId(), 1)));
        assertTrue(MaterialTier.MYTHIC.announced());
        assertFalse(MaterialTier.MAJOR.announced());
    }
}
