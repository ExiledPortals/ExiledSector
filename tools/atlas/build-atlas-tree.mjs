import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const TREE_PATH = resolve(repoRoot, 'data/skilltrees/atlas_tree.json');
const BODIES_PATH = resolve(repoRoot, 'data/skilltrees/atlas_bodies.json');

const ROOT_ID = 'atlas_root';
const STAR_RADIUS = 520;
const STAR_CLEARANCE = 260;
const HIGHWAY_HALF_WIDTH = 75;
const WHEEL_PAIR_CLEARANCE = 160;
const RING_NODE_COUNTS = [8, 12, 18, 20, 24];
const RING_HIGHWAYS = ['travel_boss_star_charts', 'travel_boss_star_charts', 'travel_catalysts', 'travel_catalysts', 'travel_chart_modifier_effect'];
const HIGHWAY_COLOUR = '#ffffff';
const HIGHWAY_ROCKS = Array.from({ length: 12 }, (_, index) => `graphics/asteroids/ring_asteroid${String(index).padStart(2, '0')}.png`);
const HIGHWAY_ROCK_SIZE = 16;
const ROOT_RAYS = 3;
const LINKS_INTO_RING = [0, 3, 4, 5, 6];
const SPOKE_CLEARANCE = HIGHWAY_HALF_WIDTH + 40;
const SPOKE_SPREAD = 30;
const SATELLITE_GAP = 170;
const WHEEL_CLEARANCE = 60;
const HIGHWAY_CLEARANCE = 40;
const BODY_CLEARANCE = 12;
const OUTER_SMALL_TURN = [35, 70];
const INNER_SMALL_TURN = [60, 110];
const SPIRAL_POINTS = 24;
const SPIRAL_HOOK = 150;
const BOW_SHARE = 0.12;
const BOW_POINTS = 16;
const LOWEST_ROOT_RAY_SHIFT = 2;
const SITE_LIGHT = '#ffbe5a';

const MOON_PLANETS = ['barren', 'barren2', 'barren3', 'rocky_ice', 'frozen1', 'rocky_metallic'];
const SITE_PLANETS = ['lava', 'frozen', 'frozen2', 'frozen3', 'cryovolcanic', 'barren', 'barren_castiron', 'barren2', 'barren3', 'rocky_metallic',
  'rocky_unstable', 'rocky_ice', 'barren-bombarded', 'toxic_cold', 'terran', 'terran-eccentric', 'jungle', 'water', 'tundra', 'desert', 'desert1',
  'barren-desert'];
const FREIGHTERS = ['buffalo', 'tarsus', 'hermes', 'phaeton', 'dram', 'mule', 'gemini', 'wayfarer'];
const PICKETS = ['wolf', 'lasher', 'hound', 'kite', 'vigilance', 'tempest', 'brawler', 'centurion', 'monitor', 'omen'];
const GUARDS_PER_DEPOT = 4;
const ESCORTS = ['lasher', 'wolf', 'hound', 'vigilance', 'centurion', 'brawler', 'enforcer', 'hammerhead', 'sunder', 'monitor'];
const DORMANT_HULLS = {
  remnant: { lead: ['brilliant', 'radiant', 'nova', 'apex'], rest: ['glimmer', 'lumen', 'fulgent', 'scintilla'] },
  threat: { lead: ['fabricator_unit', 'hive_unit', 'standoff_unit'], rest: ['skirmish_unit', 'assault_unit', 'overseer_unit'] },
  dweller: { lead: ['shrouded_maw', 'shrouded_maelstrom'], rest: ['shrouded_tendril', 'shrouded_eye'] },
  derelict: { lead: ['guardian', 'rampart', 'bastillon', 'berserker'], rest: ['picket', 'sentry', 'warden', 'defender'] },
};
const DORMANT_FACTION = { redacted_sleepers: 'remnant', threatening_sleepers: 'threat', veiled_sleepers: 'dweller' };
const POPULATED_SIZE = 150;
const PORT_SIZE = 30;
const CONVOYS_PER_PORT = 3;
const HAULER_FREIGHTERS = ['atlas', 'prometheus', 'colossus', 'starliner'];
const HAULER_ESCORTS = ['eagle', 'falcon', 'dominator', 'champion', 'heron', 'conquest'];
const PORT_STATIONS = ['graphics/stations/station_lowtech1.png', 'graphics/stations/station_lowtech2.png', 'graphics/stations/station_midline2.png',
  'graphics/stations/station_midline3.png', 'graphics/stations/station_hightech1.png', 'graphics/stations/station_hightech2.png'];
const POPULATED_WORLDS = [
  { planet: 'jungle', cityLights: ['volturn'] },
  { planet: 'terran', cityLights: ['sindria'] },
  { planet: 'water', cityLights: ['asharu'] },
  { planet: 'terran-eccentric', cityLights: ['volturn'] },
  { planet: 'rocky_metallic', cityLights: ['volturn', 'sindria', 'asharu'] },
];
const STORM_GIANT_TINT = '#ffc4ee';
const STORM_GIANT_SIZE = 150;
const REDACTED_FRIGATES = ['glimmer', 'lumen', 'glimmer', 'lumen', 'glimmer'];
const stormGiant = (planet, storm) => ({ kind: 'planet', planet, tint: STORM_GIANT_TINT, storm, size: STORM_GIANT_SIZE });
const STORM_NOTABLE_ART = {
  lightning_rod: () => stormGiant('gas_giant', 'lightning_rod'),
  supercell: () => stormGiant('gas_giant', 'supercell'),
  eye_of_the_storm: () => stormGiant('ice_giant', 'eye'),
  redacted_front: () => ({ kind: 'swarmed_planet', planet: 'gas_giant', tint: STORM_GIANT_TINT, hulls: REDACTED_FRIGATES, size: STORM_GIANT_SIZE }),
  captured_front: () => stormGiant('ice_giant', 'captured'),
  solar_front: () => ({ kind: 'planet', planet: 'star_yellow', storm: 'solar', size: 110 }),
  thunder_duel: () => stormGiant('gas_giant', 'duel'),
};
const FLARE_STAR_SIZE = 110;
const SUNSTRUCK_CONVOY = ['colossus', 'buffalo', 'tarsus', 'phaeton', 'buffalo'];
const flareStar = (planet, flare, size = FLARE_STAR_SIZE) => ({ kind: 'planet', planet, flare, size });
const FLARE_NOTABLE_ART = {
  solar_maximum: () => flareStar('star_blue_giant', 'often'),
  golden_hour: () => flareStar('star_orange', 'warm'),
  tempered_light: () => flareStar('star_red_dwarf', 'gentle', 95),
  perfect_light: () => flareStar('star_white', 'perfect'),
  sunstruck_convoy: () => ({ kind: 'crossed_planet', planet: 'star_yellow', flare: 'licking', hulls: SUNSTRUCK_CONVOY, size: 90, facing: 'travel' }),
};

function seededRandom(seed) {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let mixed = state;
    mixed = Math.imul(mixed ^ (mixed >>> 15), mixed | 1);
    mixed ^= mixed + Math.imul(mixed ^ (mixed >>> 7), mixed | 61);
    return ((mixed ^ (mixed >>> 14)) >>> 0) / 4294967296;
  };
}

const random = seededRandom(20261117);
const haulerRandom = seededRandom(20261120);
const pickHaulers = (options, low, high) => Array.from({ length: low + Math.floor(haulerRandom() * (high - low + 1)) },
  () => options[Math.floor(haulerRandom() * options.length)]);
const between = ([low, high]) => low + (high - low) * random();
const pick = (options) => options[Math.floor(random() * options.length)];
const pickSome = (options, low, high) => Array.from({ length: low + Math.floor(random() * (high - low + 1)) }, () => pick(options));
const round = (value) => Math.round(value * 10) / 10;
const toRadians = (degrees) => degrees * Math.PI / 180;
const angularGap = (first, second) => Math.abs(((first - second + 540) % 360) - 180);

function shuffled(options) {
  const copy = [...options];
  for (let index = copy.length - 1; index > 0; index--) {
    const swap = Math.floor(random() * (index + 1));
    [copy[index], copy[swap]] = [copy[swap], copy[index]];
  }
  return copy;
}

const sitePlanets = shuffled(SITE_PLANETS);
const nextSitePlanet = () => sitePlanets.shift();

const populatedWorlds = shuffled(POPULATED_WORLDS);
const populatedWorld = () => ({ kind: 'planet', ...(populatedWorlds.shift() ?? pick(POPULATED_WORLDS)), size: POPULATED_SIZE });
const portStations = shuffled(PORT_STATIONS);
let portsMade = 0;
const nextPortStation = () => portStations[portsMade++ % portStations.length];
const convoy = () => [...pickSome(FREIGHTERS, 5, 7), ...pickSome(ESCORTS, 2, 3)];
const hauler = () => [...pickHaulers(HAULER_FREIGHTERS, 3, 4), ...pickHaulers(HAULER_ESCORTS, 2, 3)];
const port = () => ({ kind: 'sprite', path: nextPortStation(), size: PORT_SIZE, facing: 'spin', spin: 0.15,
  convoys: Array.from({ length: CONVOYS_PER_PORT }, convoy), haulers: Array.from({ length: CONVOYS_PER_PORT }, hauler) });

const depotGuards = () => pickSome(PICKETS, GUARDS_PER_DEPOT, GUARDS_PER_DEPOT);

function dormantFleet(notableType) {
  const faction = DORMANT_FACTION[notableType] ?? 'derelict';
  const hulls = DORMANT_HULLS[faction];
  if (faction === 'dweller') {
    return { kind: 'shrouded', hulls: [pick(hulls.lead), ...pickSome(hulls.rest, 2, 3)], size: 140 };
  }
  return { kind: 'derelicts', hulls: [pick(hulls.lead), ...pickSome(hulls.rest, 4, 9)], size: 140 };
}

const ANOMALIES = {
  excavation_sites: {
    colour: '#ffaa50',
    chance: 'excavation_sites_chance',
    others: ['additional_site', 'inhibitor_quantity'],
    small: () => ({ kind: 'planet', planet: pick(MOON_PLANETS), size: 45 }),
    notable: () => ({ kind: 'planet', planet: nextSitePlanet(), size: 150, light: SITE_LIGHT }),
  },
  supply_convoy: {
    colour: '#78dc8c',
    chance: 'supply_convoy_chance',
    others: ['threat_feedstock_chance', 'shroud_feedstock_chance', 'phase_feedstock_chance', 'feedstock_duplication', 'additional_convoy_chance'],
    small: port,
    notable: populatedWorld,
  },
  relay_network: {
    colour: '#6ec8ff',
    chance: 'relay_network_chance',
    others: ['relay_reward_rarity'],
    small: () => ({ kind: 'sprite', path: 'graphics/ships/nav_buoy.png', size: 28, facing: 'spin' }),
    notable: () => ({ kind: 'sprite', path: pick(['graphics/ships/com_relay.png', 'graphics/ships/sensor_array.png']), size: 56, facing: 'spin', spin: 0.2 }),
  },
  spatial_breach: {
    colour: '#be78ff',
    chance: 'spatial_breach_chance',
    others: ['breach_cache_quantity', 'breach_cache_items', 'warped_chance'],
    small: () => ({ kind: 'breach', size: 34 }),
    notable: () => ({ kind: 'breach', size: 104 }),
  },
  anomalous_debris: {
    colour: '#ff6e5a',
    chance: 'anomalous_debris_chance',
    others: ['additional_debris', 'engram_quantity'],
    small: () => ({ kind: 'sprite', path: pick(['graphics/asteroids/asteroid1.png', 'graphics/asteroids/asteroid2.png', 'graphics/asteroids/asteroid3.png', 'graphics/asteroids/asteroid4.png']), size: 38, facing: 'spin' }),
    notable: (notableType) => dormantFleet(notableType),
  },
  listening_post: {
    colour: '#f0dc6e',
    chance: 'listening_post_chance',
    others: ['vault_quantity', 'slow_purge'],
    small: () => ({ kind: 'patrol', hulls: pickSome(PICKETS, 1, 2), size: 34 }),
    notable: () => ({ kind: 'outpost', path: pick(['graphics/stations/station_side01.png', 'graphics/stations/station_side02.png', 'graphics/stations/station_side03.png', 'graphics/stations/station_side04.png']), hulls: depotGuards(), size: 116, facing: 'spin' }),
  },
  storm_front: {
    colour: '#ff28d2',
    chance: 'storm_front_chance',
    others: ['storm_item_quantity', 'storm_front_size'],
    small: () => ({ kind: 'storm_cell', size: 40 }),
    notable: (notableType) => STORM_NOTABLE_ART[notableType]?.() ?? stormGiant('gas_giant', 'generic'),
  },
  stellar_flares: {
    colour: '#ffe4a0',
    chance: 'stellar_flares_chance',
    others: ['flare_hull_items'],
    small: () => flareStar('star_yellow', 'plain', 36),
    notable: (notableType) => FLARE_NOTABLE_ART[notableType]?.() ?? flareStar('star_yellow', 'plain'),
  },
};

const NOTABLE_ART = {
  mixed_manifest: () => ({ kind: 'sprite', path: 'graphics/stations/station_midline1.png', size: 184, facing: 'spin' }),
  incursion: () => ({ kind: 'sprite', path: 'graphics/stations/station_side00.png', size: 128, facing: 'spin', spin: 0.3 }),
  fortified_depot: () => ({ kind: 'station', hulls: ['station2', ...depotGuards()], size: 160 }),
  double_down: () => ({ kind: 'binary', paths: ['graphics/ships/com_relay.png', 'graphics/ships/sensor_array.png'], size: 80, facing: 'spin', spin: 0.2 }),
};

const HYBRIDS = {
  infested_bolt_holes: { anomalies: ['excavation_sites', 'anomalous_debris'], planet: 'toxic', colour: '#ff8c55' },
  torn_holds: { anomalies: ['supply_convoy', 'spatial_breach'], planet: 'lava_minor', colour: '#9aaaff' },
  bolt_hole_drop: { anomalies: ['supply_convoy', 'excavation_sites'], planet: 'arid', colour: '#c8c86e' },
  event_horizon: { anomalies: ['spatial_breach', 'spatial_breach'], planet: 'black_hole', colour: '#be78ff' },
  rude_awakening: { anomalies: ['spatial_breach', 'anomalous_debris'], planet: 'irradiated', colour: '#e070b0' },
  breached_graveyard: { anomalies: ['anomalous_debris', 'spatial_breach'], planet: 'barren_venuslike', colour: '#e070b0' },
};

const ANOMALY_OF = {
  settled_shafts: 'excavation_sites', twin_shafts: 'excavation_sites', sapper_teams: 'excavation_sites', open_bores: 'excavation_sites',
  widened_bores: 'excavation_sites', needle_bores: 'excavation_sites', deeper_bores: 'excavation_sites', pitch_black: 'excavation_sites',
  threat_embargo: 'supply_convoy', shroud_embargo: 'supply_convoy', phase_embargo: 'supply_convoy', supply_lines: 'supply_convoy',
  open_season: 'supply_convoy', mixed_manifest: 'supply_convoy',
  crowded_channel: 'relay_network', extra_relay: 'relay_network', shared_broadcast: 'relay_network', scar_collector: 'relay_network',
  early_echo: 'relay_network', full_array: 'relay_network', double_down: 'relay_network', forked_trace: 'relay_network',
  heavy_breach: 'spatial_breach', rift_magnetism: 'spatial_breach', rally_point: 'spatial_breach', rift_duel: 'spatial_breach',
  hyperspace_bleed: 'spatial_breach', incursion: 'spatial_breach',
  redacted_sleepers: 'anomalous_debris', veiled_sleepers: 'anomalous_debris', threatening_sleepers: 'anomalous_debris',
  double_dose: 'anomalous_debris', deep_corruption: 'anomalous_debris', lone_sleeper: 'anomalous_debris',
  restless_sleepers: 'anomalous_debris', silent_choir: 'anomalous_debris',
  officers_ledger: 'listening_post', stolen_charts: 'listening_post', smash_and_grab: 'listening_post', hair_trigger: 'listening_post',
  garrison_pickets: 'listening_post', fortified_depot: 'listening_post', hunting_party: 'listening_post',
  lightning_rod: 'storm_front', supercell: 'storm_front', eye_of_the_storm: 'storm_front', redacted_front: 'storm_front',
  captured_front: 'storm_front', solar_front: 'storm_front', thunder_duel: 'storm_front',
  solar_maximum: 'stellar_flares', golden_hour: 'stellar_flares', tempered_light: 'stellar_flares', sunstruck_convoy: 'stellar_flares',
  perfect_light: 'stellar_flares',
};

const RINGS = [
  ['settled_shafts o', 'threat_embargo i', 'officers_ledger o', 'stolen_charts i', 'heavy_breach o', 'shroud_embargo i'],
  ['twin_shafts i', 'crowded_channel o', 'redacted_sleepers i', 'rift_magnetism o', 'veiled_sleepers i', 'shared_broadcast o', 'sapper_teams i',
    'phase_embargo o', 'extra_relay o'],
  ['open_bores o', 'infested_bolt_holes i', 'threatening_sleepers o', 'scar_collector i', 'rally_point i', 'torn_holds o', 'supply_lines i',
    'hair_trigger o', 'early_echo o', 'smash_and_grab i'],
  ['widened_bores i', 'double_dose o', 'breached_graveyard i', 'rift_duel o', 'garrison_pickets o', 'event_horizon i', 'full_array o',
    'needle_bores i', 'deep_corruption o'],
  ['deeper_bores o', 'bolt_hole_drop i', 'hyperspace_bleed o', 'double_down i', 'open_season o', 'rude_awakening i', 'pitch_black o',
    'lone_sleeper i', 'fortified_depot o', 'forked_trace i', 'mixed_manifest o', 'restless_sleepers i', 'hunting_party o', 'incursion i',
    'silent_choir o'],
];

const MAX_CHANCE_NODES = 12;

const nodes = [];
const bodies = [];
const nodeById = new Map();
const bodyById = new Map();
const typeCounters = new Map();

function nextId(type) {
  const count = (typeCounters.get(type) ?? 0) + 1;
  typeCounters.set(type, count);
  return `${type}_${count}`;
}

function addNode(type, x, y, tags, body) {
  const node = { id: nextId(type), type, connectedTo: [], x: round(x), y: round(y) };
  if (tags.length > 0) {
    node.tags = tags;
  }
  nodes.push(node);
  nodeById.set(node.id, node);
  const entry = { id: node.id, ...body };
  bodies.push(entry);
  bodyById.set(node.id, entry);
  return node;
}

function link(first, second) {
  if (!first.connectedTo.includes(second.id)) first.connectedTo.push(second.id);
  if (!second.connectedTo.includes(first.id)) second.connectedTo.push(first.id);
}

function wheelPlan(notableType) {
  const hybrid = HYBRIDS[notableType];
  if (hybrid) {
    const art = { kind: 'planet', planet: hybrid.planet, size: 162 };
    if (hybrid.anomalies.includes('excavation_sites')) art.light = SITE_LIGHT;
    return {
      notableType,
      tags: [...new Set(notableType === 'event_horizon' ? ['black_hole', 'spatial_breach'] : hybrid.anomalies)],
      colour: hybrid.colour,
      art,
      smallAnomalies: hybrid.anomalies,
      hybrid: true,
    };
  }
  const anomaly = ANOMALY_OF[notableType];
  if (!anomaly) throw new Error(`No anomaly for ${notableType}`);
  return {
    notableType,
    tags: [anomaly],
    colour: ANOMALIES[anomaly].colour,
    art: NOTABLE_ART[notableType]?.() ?? ANOMALIES[anomaly].notable(notableType),
    smallAnomalies: [anomaly, anomaly],
    hybrid: false,
  };
}

function shapeWheel(wheel) {
  wheel.smallArt = wheel.smallAnomalies.map((anomaly) => ANOMALIES[anomaly].small());
  const [outerSize, innerSize] = wheel.smallArt.map((art) => art.size);
  wheel.innerTrack = wheel.art.size + SATELLITE_GAP + innerSize;
  wheel.outerTrack = wheel.innerTrack + innerSize + SATELLITE_GAP + outerSize;
  wheel.reach = wheel.outerTrack + outerSize;
  wheel.spokeLength = SPOKE_CLEARANCE + random() * SPOKE_SPREAD + wheel.reach;
  wheel.extent = wheel.spokeLength + wheel.reach;
}

const plannedWheels = RINGS.flatMap((entries, ringIndex) => entries.map((entry, slot) => {
  const [notableType, side] = entry.split(' ');
  return { ...wheelPlan(notableType), ringIndex, slot, slotCount: entries.length, inside: side === 'i' };
}));
plannedWheels.forEach(shapeWheel);

const extentOf = (ringIndex, inside) => Math.max(HIGHWAY_HALF_WIDTH,
  ...plannedWheels.filter((wheel) => wheel.ringIndex === ringIndex && wheel.inside === inside).map((wheel) => wheel.extent));
const ringRadii = [];
RING_NODE_COUNTS.forEach((_, ringIndex) => {
  if (ringIndex === 0) {
    ringRadii.push(Math.round(STAR_RADIUS + STAR_CLEARANCE + extentOf(0, true)));
    return;
  }
  const outward = extentOf(ringIndex - 1, false);
  const inward = extentOf(ringIndex, true);
  const gap = Math.max((outward + WHEEL_PAIR_CLEARANCE + inward) / 2, Math.max(outward, inward) + HIGHWAY_HALF_WIDTH + HIGHWAY_CLEARANCE);
  ringRadii.push(Math.round(ringRadii[ringIndex - 1] + gap));
});

const root = { id: ROOT_ID, type: ROOT_ID, connectedTo: [], x: 0, y: 0 };
nodes.push(root);
nodeById.set(ROOT_ID, root);
const rootBody = { id: ROOT_ID, art: { kind: 'star', size: STAR_RADIUS } };
bodies.push(rootBody);
bodyById.set(ROOT_ID, rootBody);

const rings = ringRadii.map((radius, ringIndex) => {
  const count = RING_NODE_COUNTS[ringIndex];
  const phase = random() * 360;
  const type = RING_HIGHWAYS[ringIndex];
  const ringNodes = [];
  for (let index = 0; index < count; index++) {
    const angle = phase + index * 360 / count;
    const node = addNode(type, radius * Math.cos(toRadians(angle)), radius * Math.sin(toRadians(angle)), [], {
      highway: true, colour: HIGHWAY_COLOUR, art: { kind: 'sprite', path: pick(HIGHWAY_ROCKS), size: HIGHWAY_ROCK_SIZE, facing: 'spin' },
    });
    ringNodes.push({ node, angle });
  }
  ringNodes.forEach(({ node }, index) => link(node, ringNodes[(index + 1) % count].node));
  return { radius, nodes: ringNodes, wheels: [], spokes: new Set() };
});

const slotsByAnomaly = new Map();
const orderedForChance = [...plannedWheels.filter((wheel) => wheel.hybrid), ...plannedWheels.filter((wheel) => !wheel.hybrid)];
for (const wheel of orderedForChance) {
  wheel.smallTypes = [];
  wheel.smallAnomalies.forEach((anomaly) => {
    const slots = slotsByAnomaly.get(anomaly) ?? [];
    slots.push({ wheel, index: wheel.smallTypes.length });
    wheel.smallTypes.push(null);
    slotsByAnomaly.set(anomaly, slots);
  });
}
for (const [anomaly, slots] of slotsByAnomaly) {
  const others = ANOMALIES[anomaly].others;
  slots.forEach(({ wheel, index }, order) => {
    wheel.smallTypes[index] = order < MAX_CHANCE_NODES ? ANOMALIES[anomaly].chance : others[(order - MAX_CHANCE_NODES) % others.length];
  });
}

function distanceToSegment(point, from, to) {
  const lengthSquared = (to.x - from.x) ** 2 + (to.y - from.y) ** 2;
  const share = lengthSquared === 0 ? 0
    : Math.max(0, Math.min(1, ((point.x - from.x) * (to.x - from.x) + (point.y - from.y) * (to.y - from.y)) / lengthSquared));
  return Math.hypot(point.x - (from.x + share * (to.x - from.x)), point.y - (from.y + share * (to.y - from.y)));
}

const clearOf = (from, to, circles) => circles.every((circle) => distanceToSegment(circle, from, to) >= circle.radius);

function satelliteOffsets(wheel, spokeAngle) {
  const towardSpoke = wheel.inside ? spokeAngle : spokeAngle + 180;
  const side = random() < 0.5 ? 1 : -1;
  const outerAngle = towardSpoke + side * between(OUTER_SMALL_TURN);
  const innerAngle = outerAngle + side * between(INNER_SMALL_TURN);
  return [[wheel.outerTrack, outerAngle], [wheel.innerTrack, innerAngle]]
    .map(([track, angle]) => ({ x: track * Math.cos(toRadians(angle)), y: track * Math.sin(toRadians(angle)) }));
}

const polarAround = (centre, point) => ({ radius: Math.hypot(point.x - centre.x, point.y - centre.y), angle: Math.atan2(point.y - centre.y, point.x - centre.x) });
const turnOf = (centre, from, to) => (Math.sin(polarAround(centre, to).angle - polarAround(centre, from).angle) >= 0 ? 1 : -1);

function spiralPath(from, to, centre, turn) {
  const fromPolar = polarAround(centre, from);
  const toPolar = polarAround(centre, to);
  const inward = fromPolar.radius >= toPolar.radius;
  const outer = inward ? fromPolar : toPolar;
  const inner = { ...(inward ? toPolar : fromPolar) };
  if (inner.radius < 1) inner.angle = outer.angle + turn * toRadians(SPIRAL_HOOK);
  const fullTurn = Math.PI * 2;
  const sweep = turn * ((((inner.angle - outer.angle) * turn) % fullTurn + fullTurn) % fullTurn);
  const points = Array.from({ length: SPIRAL_POINTS }, (_, index) => {
    const share = index / (SPIRAL_POINTS - 1);
    const radius = outer.radius + (inner.radius - outer.radius) * share;
    const angle = outer.angle + sweep * share;
    return { x: centre.x + radius * Math.cos(angle), y: centre.y + radius * Math.sin(angle) };
  });
  return inward ? points : points.reverse();
}

function bowedPath(from, to) {
  const deltaX = to.x - from.x;
  const deltaY = to.y - from.y;
  const length = Math.hypot(deltaX, deltaY);
  const sense = Math.hypot(to.x, to.y) > Math.hypot(from.x, from.y) ? -1 : 1;
  const normal = { x: (-deltaY / length) * sense, y: (deltaX / length) * sense };
  return Array.from({ length: BOW_POINTS }, (_, index) => {
    const share = index / (BOW_POINTS - 1);
    const bulge = length * BOW_SHARE * Math.sin(Math.PI * share);
    return { x: from.x + deltaX * share + normal.x * bulge, y: from.y + deltaY * share + normal.y * bulge };
  });
}

const travelHeading = (angle) => round(-angle - 90);

function withHeading(art, angle) {
  const travels = art.facing === 'travel' || art.kind === 'fleet' || art.kind === 'derelicts' || art.kind === 'station';
  return travels ? { ...art, heading: travelHeading(angle) } : art;
}

const placedWheels = [];
const wheelOf = new Map();
const clashes = ({ x, y }, reach) => placedWheels.some((other) => Math.hypot(other.x - x, other.y - y) < reach + other.reach + WHEEL_CLEARANCE);

for (const wheel of plannedWheels) {
  const ring = rings[wheel.ringIndex];
  const step = ring.nodes.length / wheel.slotCount;
  const jitter = (random() - 0.5) * step * 0.35;
  const target = Math.round(wheel.slot * step + jitter + ring.nodes.length) % ring.nodes.length;
  const centreDistance = ring.radius + (wheel.inside ? -1 : 1) * wheel.spokeLength;
  const centreOf = (spokeIndex) => ({
    x: centreDistance * Math.cos(toRadians(ring.nodes[spokeIndex].angle)),
    y: centreDistance * Math.sin(toRadians(ring.nodes[spokeIndex].angle)),
  });
  const candidates = Array.from({ length: ring.nodes.length * 2 }, (_, offset) => {
    const shift = offset % 2 === 0 ? offset / 2 : -(offset + 1) / 2;
    return (target + shift + ring.nodes.length * 4) % ring.nodes.length;
  }).filter((index, order, all) => all.indexOf(index) === order && !ring.spokes.has(index));
  const spokeIndex = candidates.find((index) => !clashes(centreOf(index), wheel.reach));
  if (spokeIndex === undefined) throw new Error(`No clear spoke for ${wheel.notableType} on ring ${wheel.ringIndex}`);
  ring.spokes.add(spokeIndex);
  const spoke = ring.nodes[spokeIndex];
  const centre = centreOf(spokeIndex);
  placedWheels.push({ x: centre.x, y: centre.y, reach: wheel.reach });
  const offsets = satelliteOffsets(wheel, spoke.angle);
  const turn = turnOf(centre, spoke.node, { x: centre.x + offsets[0].x, y: centre.y + offsets[0].y });
  const notable = addNode(wheel.notableType, centre.x, centre.y, wheel.tags, {
    colour: wheel.colour, spiral: turn, art: withHeading(wheel.art, spoke.angle),
  });
  wheelOf.set(notable.id, { centre: notable, turn });
  const satellites = offsets.map((offset, order) => {
    const smallAnomaly = wheel.smallAnomalies[order];
    const small = addNode(wheel.smallTypes[order], centre.x + offset.x, centre.y + offset.y, [smallAnomaly], {
      colour: ANOMALIES[smallAnomaly].colour, wheel: notable.id, art: withHeading(wheel.smallArt[order], spoke.angle),
    });
    wheelOf.set(small.id, { centre: notable, turn });
    return small;
  });
  link(spoke.node, satellites[0]);
  link(satellites[0], satellites[1]);
  link(satellites[1], notable);
  ring.wheels.push({ x: centre.x, y: centre.y, radius: wheel.reach + WHEEL_CLEARANCE / 2, type: wheel.notableType });
}

const overlaps = [];
const allWheels = rings.flatMap((ring) => ring.wheels);
allWheels.forEach((first, index) => allWheels.slice(index + 1).forEach((second) => {
  const distance = Math.hypot(first.x - second.x, first.y - second.y);
  if (distance < first.radius + second.radius) overlaps.push(`${first.type} and ${second.type} (${Math.round(distance)})`);
}));
if (overlaps.length > 0) throw new Error(`Wheels overlap: ${overlaps.join(', ')}`);

const ringLinkTargets = (ringIndex) => (ringIndex === 0 ? [{ node: root, angle: null }] : rings[ringIndex - 1].nodes);

function spreadRingLinks(ringIndex, count) {
  const pairs = [];
  for (const anchor of rings[ringIndex].nodes) {
    for (const landing of ringLinkTargets(ringIndex)) {
      if (clearOf(anchor.node, landing.node, allWheels)) {
        pairs.push({ anchor, landing, bend: landing.angle === null ? 0 : angularGap(anchor.angle, landing.angle) });
      }
    }
  }
  const chosen = [];
  const phase = random() * 360;
  for (let index = 0; index < count; index++) {
    const target = phase + index * 360 / count;
    const free = pairs.filter((pair) => !chosen.some((picked) => picked.anchor === pair.anchor
      || (pair.landing.angle !== null && picked.landing === pair.landing)));
    if (free.length === 0) throw new Error(`No clear link into ring ${ringIndex}`);
    const cost = (pair) => angularGap(pair.anchor.angle, target) + pair.bend * 2;
    free.sort((first, second) => cost(first) - cost(second));
    chosen.push(free[0]);
  }
  return chosen;
}

function shiftLowestRay(rays) {
  const ringNodes = rings[0].nodes;
  const lowest = rays.reduce((low, ray) => (ray.anchor.node.y > low.anchor.node.y ? ray : low));
  const shifted = ringNodes[(ringNodes.indexOf(lowest.anchor) - LOWEST_ROOT_RAY_SHIFT + ringNodes.length) % ringNodes.length];
  if (rays.some((ray) => ray.anchor === shifted) || !clearOf(shifted.node, root, allWheels)) throw new Error('The lowest root ray cannot shift');
  return rays.map((ray) => (ray === lowest ? { ...ray, anchor: shifted } : ray));
}

rings.forEach((_, ringIndex) => {
  const count = ringIndex === 0 ? ROOT_RAYS : LINKS_INTO_RING[ringIndex];
  const chosen = spreadRingLinks(ringIndex, count);
  (ringIndex === 0 ? shiftLowestRay(chosen) : chosen).forEach(({ anchor, landing }) => link(anchor.node, landing.node));
});

const sameRing = (first, second) => bodyById.get(first.id).highway && bodyById.get(second.id).highway
  && Math.abs(Math.hypot(first.x, first.y) - Math.hypot(second.x, second.y)) < 1;
const wheelLinkOf = (first, second) => (bodyById.get(first.id).wheel ? wheelOf.get(first.id) : bodyById.get(second.id).wheel ? wheelOf.get(second.id) : null);
const crossings = [];
const drawnLinks = nodes.flatMap((node) => node.connectedTo.map((id) => nodeById.get(id))
  .filter((other) => node.id < other.id && !sameRing(node, other)).map((other) => {
    const wheel = wheelLinkOf(node, other);
    return { ends: [node, other], bowed: !wheel, points: wheel ? spiralPath(node, other, wheel.centre, wheel.turn) : bowedPath(node, other) };
  }));
const segmentsOf = (points) => points.slice(1).map((point, index) => [points[index], point]);
for (const { ends, points } of drawnLinks) {
  const blocking = nodes.filter((body) => !ends.includes(body) && body.id !== ROOT_ID)
    .filter((body) => segmentsOf(points).some(([from, to]) => distanceToSegment(body, from, to) < bodyById.get(body.id).art.size + BODY_CLEARANCE));
  if (blocking.length > 0) crossings.push(`${ends[0].id}-${ends[1].id} crosses ${blocking.map((body) => body.id).join(', ')}`);
}
const bowedLinks = drawnLinks.filter(({ bowed }) => bowed);
function crossesCircle(from, to, radius) {
  const deltaX = to.x - from.x;
  const deltaY = to.y - from.y;
  const a = deltaX * deltaX + deltaY * deltaY;
  const b = 2 * (from.x * deltaX + from.y * deltaY);
  const c = from.x * from.x + from.y * from.y - radius * radius;
  const discriminant = b * b - 4 * a * c;
  if (discriminant <= 0) return false;
  const edge = 1 / Math.sqrt(a);
  return [(-b - Math.sqrt(discriminant)) / (2 * a), (-b + Math.sqrt(discriminant)) / (2 * a)].some((share) => share > edge && share < 1 - edge);
}
for (const { ends: [from, to], points } of bowedLinks) {
  const crossed = ringRadii.filter((radius) => segmentsOf(points).some(([start, end]) => crossesCircle(start, end, radius)));
  if (crossed.length > 0) crossings.push(`${from.id}-${to.id} crosses the ring at ${crossed.join(', ')}`);
}
const cross = (a, b, c) => (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
const segmentsCross = ([first, second], [third, fourth]) => (cross(first, second, third) > 0) !== (cross(first, second, fourth) > 0)
  && (cross(third, fourth, first) > 0) !== (cross(third, fourth, second) > 0);
drawnLinks.forEach((link, index) => drawnLinks.slice(index + 1).forEach((other) => {
  if (link.ends.some((end) => other.ends.includes(end))) return;
  if (segmentsOf(link.points).some((segment) => segmentsOf(other.points).some((otherSegment) => segmentsCross(segment, otherSegment)))) {
    crossings.push(`${link.ends[0].id}-${link.ends[1].id} crosses ${other.ends[0].id}-${other.ends[1].id}`);
  }
}));
if (crossings.length > 0) throw new Error(`Links cross: ${crossings.join('; ')}`);

const chanceCounts = {};
for (const node of nodes) {
  if (node.type.endsWith('_chance') && ANOMALIES[node.type.replace(/_chance$/, '')]) {
    chanceCounts[node.type] = (chanceCounts[node.type] ?? 0) + 1;
  }
}

const tree = JSON.parse(readFileSync(TREE_PATH, 'utf8'));
tree.nodes = nodes;
tree.connectorCurves = [];
tree.hiddenConnectors = [];
tree.decorations = [{ kind: 'star', id: 'atlas_star', x: 0, y: 0, radius: STAR_RADIUS, starType: 'star_yellow' }];

const withCrlf = (value) => JSON.stringify(value, null, '\t').replace(/\n/g, '\r\n') + '\r\n';
writeFileSync(TREE_PATH, withCrlf(tree));
writeFileSync(BODIES_PATH, withCrlf({ bodies }));

console.log(`${nodes.length} nodes, ${plannedWheels.length} wheels, rings ${rings.map((ring) => `${ring.nodes.length}@${ring.radius}`).join(' / ')}`);
console.log('chance nodes', chanceCounts);
