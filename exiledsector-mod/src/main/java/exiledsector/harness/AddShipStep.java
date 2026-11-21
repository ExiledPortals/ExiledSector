package exiledsector.harness;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.VariantSource;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AddShipStep implements HarnessStep {

    record Fitting(boolean strip, Map<String, String> weaponsBySlot, List<String> wingIds, List<String> hullModIds, Integer fluxVents,
                   Integer fluxCapacitors) {

        static final Fitting STOCK = new Fitting(false, Map.of(), List.of(), List.of(), null, null);

        Fitting {
            weaponsBySlot = Map.copyOf(weaponsBySlot);
            wingIds = List.copyOf(wingIds);
            hullModIds = List.copyOf(hullModIds);
        }

        static Fitting parse(JSONObject step) throws JSONException {
            JSONObject weapons = step.optJSONObject("weapons");
            Map<String, String> weaponsBySlot = new LinkedHashMap<>();
            String[] slotIds = weapons == null ? null : JSONObject.getNames(weapons);
            for (int index = 0; slotIds != null && index < slotIds.length; index++) {
                weaponsBySlot.put(slotIds[index], weapons.getString(slotIds[index]));
            }
            return new Fitting(step.optBoolean("strip", false), weaponsBySlot, HarnessScenario.strings(step.optJSONArray("wings")),
                    HarnessScenario.strings(step.optJSONArray("hullMods")), step.has("vents") ? step.getInt("vents") : null,
                    step.has("capacitors") ? step.getInt("capacitors") : null);
        }

        boolean customisesVariant() {
            return strip || !weaponsBySlot.isEmpty() || !wingIds.isEmpty() || !hullModIds.isEmpty() || fluxVents != null || fluxCapacitors != null;
        }
    }

    record Loadout(String shipName, Fitting fitting, boolean flagship, boolean replaceFleet) {

        static final Loadout STOCK = new Loadout(null, Fitting.STOCK, false, false);

        static Loadout parse(JSONObject step) throws JSONException {
            String shipName = step.optString("name", "");
            return new Loadout(shipName.isEmpty() ? null : shipName, Fitting.parse(step), step.optBoolean("flagship", false),
                    step.optBoolean("replaceFleet", false));
        }
    }

    private final String variantId;
    private final int count;
    private final Loadout loadout;

    AddShipStep(String variantId, int count) {
        this(variantId, count, Loadout.STOCK);
    }

    AddShipStep(String variantId, int count, Loadout loadout) {
        if (count < 1) {
            throw new IllegalArgumentException("addShip needs a count of at least 1, not " + count);
        }
        if (count > 1 && (loadout.flagship() || loadout.shipName() != null)) {
            throw new IllegalArgumentException("addShip with a name or as the flagship adds one ship, not " + count);
        }
        this.variantId = variantId;
        this.count = count;
        this.loadout = loadout;
    }

    @Override
    public String name() {
        return "addShip " + count + " " + variantId + (loadout.shipName() == null ? "" : " as " + loadout.shipName())
                + (loadout.flagship() ? " (flagship)" : "");
    }

    @Override
    public StepOutcome advance(StepContext context) {
        if (Global.getCurrentState() != GameState.CAMPAIGN) {
            return StepOutcome.failed("addShip only works in the campaign");
        }
        if (!Global.getSettings().doesVariantExist(variantId)) {
            return StepOutcome.failed("no ship variant \"" + variantId + "\"");
        }
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        FleetDataAPI fleetData = playerFleet.getFleetData();
        List<FleetMemberAPI> previousMembers = fleetData.getMembersListCopy();
        List<String> added = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        FleetMemberAPI lastAdded = null;
        for (int index = 0; index < count; index++) {
            FleetMemberAPI member = loadout.fitting().customisesVariant()
                    ? Global.getFactory().createFleetMember(FleetMemberType.SHIP, customVariant(skipped))
                    : Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
            member.setShipName(loadout.shipName() == null ? playerFleet.getFaction().pickRandomShipName() : loadout.shipName());
            fleetData.addFleetMember(member);
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
            added.add(member.getShipName() + " (" + member.getHullId() + ")");
            lastAdded = member;
        }
        if (loadout.flagship()) {
            makeFlagship(playerFleet, lastAdded);
        }
        int removed = 0;
        if (loadout.replaceFleet()) {
            for (FleetMemberAPI previous : previousMembers) {
                fleetData.removeFleetMember(previous);
                removed++;
            }
        }
        fleetData.setSyncNeeded();
        return StepOutcome.passed("added " + String.join(", ", added) + " to the player fleet"
                + (loadout.flagship() ? " as the flagship" : "")
                + (removed > 0 ? ", removed the " + removed + " ships it had" : "")
                + (skipped.isEmpty() ? "" : "; skipped unknown " + String.join(", ", skipped)));
    }

    private ShipVariantAPI customVariant(List<String> skipped) {
        SettingsAPI settings = Global.getSettings();
        Fitting fitting = loadout.fitting();
        ShipVariantAPI variant = settings.getVariant(variantId).clone();
        variant.setSource(VariantSource.REFIT);
        if (fitting.strip()) {
            strip(variant);
        }
        if (!fitting.weaponsBySlot().isEmpty()) {
            clearWeapons(variant);
            fitting.weaponsBySlot().forEach((slotId, weaponId) -> {
                if (variant.getSlot(slotId) != null && weaponExists(settings, weaponId)) {
                    variant.addWeapon(slotId, weaponId);
                } else {
                    skipped.add("weapon " + weaponId + " in " + slotId);
                }
            });
            variant.autoGenerateWeaponGroups();
        }
        if (!fitting.wingIds().isEmpty()) {
            for (int bay = 0; bay < variant.getHullSpec().getFighterBays(); bay++) {
                String wingId = bay < fitting.wingIds().size() ? fitting.wingIds().get(bay) : null;
                boolean known = wingId != null && wingExists(settings, wingId);
                if (wingId != null && !known) {
                    skipped.add("wing " + wingId);
                }
                variant.setWingId(bay, known ? wingId : null);
            }
        }
        for (String hullModId : fitting.hullModIds()) {
            if (settings.getHullModSpec(hullModId) != null) {
                variant.addMod(hullModId);
            } else {
                skipped.add("hull mod " + hullModId);
            }
        }
        if (fitting.fluxVents() != null) {
            variant.setNumFluxVents(fitting.fluxVents());
        }
        if (fitting.fluxCapacitors() != null) {
            variant.setNumFluxCapacitors(fitting.fluxCapacitors());
        }
        return variant;
    }

    private static void strip(ShipVariantAPI variant) {
        clearWeapons(variant);
        for (int bay = 0; bay < variant.getHullSpec().getFighterBays(); bay++) {
            variant.setWingId(bay, null);
        }
        variant.clearHullMods();
        variant.setNumFluxVents(0);
        variant.setNumFluxCapacitors(0);
    }

    private static void clearWeapons(ShipVariantAPI variant) {
        for (String slotId : new ArrayList<>(variant.getFittedWeaponSlots())) {
            variant.clearSlot(slotId);
        }
        variant.getWeaponGroups().clear();
    }

    private static void makeFlagship(CampaignFleetAPI playerFleet, FleetMemberAPI member) {
        FleetMemberAPI previousFlagship = playerFleet.getFlagship();
        if (previousFlagship != null && previousFlagship != member && previousFlagship.getCaptain() == playerFleet.getCommander()) {
            previousFlagship.setCaptain(null);
        }
        playerFleet.getFleetData().setFlagship(member);
        member.setCaptain(playerFleet.getCommander());
    }

    @SuppressWarnings("java:S1166") // the game throws for unknown weapon ids, which this step reports as skipped
    private static boolean weaponExists(SettingsAPI settings, String weaponId) {
        try {
            return settings.getWeaponSpec(weaponId) != null;
        } catch (RuntimeException unknownWeapon) {
            return false;
        }
    }

    @SuppressWarnings("java:S1166") // the game throws for unknown wing ids, which this step reports as skipped
    private static boolean wingExists(SettingsAPI settings, String wingId) {
        try {
            return settings.getFighterWingSpec(wingId) != null;
        } catch (RuntimeException unknownWing) {
            return false;
        }
    }
}
