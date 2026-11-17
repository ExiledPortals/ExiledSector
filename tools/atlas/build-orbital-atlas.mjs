import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const TREE_PATH = resolve(repoRoot, 'data/skilltrees/atlas_tree.json');
const ORBITS_PATH = resolve(repoRoot, 'data/skilltrees/atlas_orbits.json');

const ROOT_ID = 'atlas_root';
const STAR_RADIUS = 520;
const STAR_CLEARANCE = 260;
const RING_CLEARANCE = 160;
const BELT_HALF_WIDTH = 75;
const RING_NODE_COUNTS = [8, 12, 18, 20, 24];
const INNER_RING_SPEED = 1.2;
const RING_HIGHWAYS = ['travel_boss_star_charts', 'travel_boss_star_charts', 'travel_catalysts', 'travel_catalysts', 'travel_chart_modifier_effect'];
const HIGHWAY_COLOUR = '#ffffff';
const HIGHWAY_ROCKS = Array.from({ length: 12 }, (_, index) => `graphics/asteroids/ring_asteroid${String(index).padStart(2, '0')}.png`);
const HIGHWAY_ROCK_SIZE = 16;
const ROOT_RAYS = 3;
const TETHERS_INTO_RING = [0, 3, 4, 5, 6];
const SPOKE_CLEARANCE = BELT_HALF_WIDTH + 40;
const SPOKE_SPREAD = 30;
const SATELLITE_GAP = 100;
const WHEEL_CLEARANCE = 60;
const BELT_CLEARANCE = 40;
const INNER_SATELLITE_SPEED = [3.2, 5.2];
const OUTER_SATELLITE_SPEED = [1.8, 3];
const ESCORT_BESIDE = [1, 0.75];
const ESCORT_BEHIND = [0.5, 0.6];
const SITE_LIGHT = '#ffbe5a';

const MOON_PLANETS = ['barren', 'barren2', 'barren3', 'rocky_ice', 'frozen1', 'rocky_metallic'];
const SITE_PLANETS = ['lava', 'frozen', 'frozen2', 'frozen3', 'cryovolcanic', 'barren', 'barren_castiron', 'barren2', 'barren3', 'rocky_metallic',
  'rocky_unstable', 'rocky_ice', 'barren-bombarded', 'toxic_cold', 'terran', 'terran-eccentric', 'jungle', 'water', 'tundra', 'desert', 'desert1',
  'barren-desert'];
const CONVOY_LEADS = ['atlas', 'colossus', 'prometheus'];
const FREIGHTERS = ['buffalo', 'tarsus', 'hermes', 'phaeton', 'dram', 'mule'];
const COMBAT_FREIGHTERS = ['gemini', 'wayfarer', 'mule', 'cerberus', 'hound'];
const ESCORTS = ['lasher', 'wolf', 'hound', 'vigilance', 'centurion', 'brawler', 'enforcer', 'hammerhead', 'sunder', 'monitor'];
const DORMANT_HULLS = {
  remnant: { lead: ['brilliant', 'radiant', 'nova', 'apex'], rest: ['glimmer', 'lumen', 'fulgent', 'scintilla'] },
  threat: { lead: ['fabricator_unit', 'hive_unit', 'standoff_unit'], rest: ['skirmish_unit', 'assault_unit', 'overseer_unit'] },
  dweller: { lead: ['shrouded_maw', 'shrouded_maelstrom'], rest: ['shrouded_tendril', 'shrouded_eye'] },
  derelict: { lead: ['guardian', 'rampart', 'bastillon', 'berserker'], rest: ['picket', 'sentry', 'warden', 'defender'] },
};
const DORMANT_FACTION = { redacted_sleepers: 'remnant', threatening_sleepers: 'threat', veiled_sleepers: 'dweller' };

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

const escortFleet = () => ({ kind: 'fleet', hulls: pickSome(ESCORTS, 2, 3), size: 52, facing: 'travel' });
const convoyFleet = (hulls) => ({ kind: 'fleet', hulls, size: 140, facing: 'travel' });

function dormantFleet(notableType) {
  const hulls = DORMANT_HULLS[DORMANT_FACTION[notableType] ?? 'derelict'];
  return { kind: 'derelicts', hulls: [pick(hulls.lead), ...pickSome(hulls.rest, 3, 5)], size: 140 };
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
    small: escortFleet,
    notable: () => convoyFleet([pick(CONVOY_LEADS), ...pickSome(FREIGHTERS, 4, 6)]),
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
    small: () => ({ kind: 'sprite', path: pick(['graphics/ships/wolf/wolf_base.png', 'graphics/ships/lasher/lasher_base.png']), size: 34, facing: 'travel' }),
    notable: () => ({ kind: 'sprite', path: pick(['graphics/stations/station_side01.png', 'graphics/stations/station_side02.png', 'graphics/stations/station_side03.png', 'graphics/stations/station_side04.png']), size: 116, facing: 'spin' }),
  },
};

const NOTABLE_ART = {
  silent_choir: () => ({ kind: 'sprite', path: 'graphics/stations/cryosleeper.png', size: 184, facing: 'spin' }),
  mixed_manifest: () => ({ kind: 'sprite', path: 'graphics/stations/station_midline1.png', size: 184, facing: 'spin' }),
  incursion: () => ({ kind: 'sprite', path: 'graphics/stations/station_side00.png', size: 128, facing: 'spin', spin: 0.3 }),
  fortified_depot: () => ({ kind: 'station', hulls: ['station2'], size: 160 }),
  double_down: () => ({ kind: 'binary', paths: ['graphics/ships/com_relay.png', 'graphics/ships/sensor_array.png'], size: 80, facing: 'spin', spin: 0.2 }),
  open_season: () => convoyFleet(pickSome(COMBAT_FREIGHTERS, 5, 6)),
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
};

const RINGS = [
  ['settled_shafts o', 'threat_embargo i', 'officers_ledger o', 'redacted_sleepers i', 'heavy_breach o', 'shroud_embargo i'],
  ['twin_shafts i', 'crowded_channel o', 'stolen_charts i', 'rift_magnetism o', 'veiled_sleepers i', 'shared_broadcast o', 'sapper_teams i',
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
const orbits = [];
const nodeById = new Map();
const typeCounters = new Map();

function nextId(type) {
  const count = (typeCounters.get(type) ?? 0) + 1;
  typeCounters.set(type, count);
  return `${type}_${count}`;
}

function addNode(type, x, y, tags, orbit) {
  const node = { id: nextId(type), type, connectedTo: [], x: round(x), y: round(y) };
  if (tags.length > 0) {
    node.tags = tags;
  }
  nodes.push(node);
  nodeById.set(node.id, node);
  orbits.push({ id: node.id, ...orbit });
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
  wheel.convoy = !wheel.hybrid && wheel.art.kind === 'fleet';
  const [outerSize, innerSize] = wheel.smallArt.map((art) => art.size);
  if (wheel.convoy) {
    const spacing = wheel.art.size + SATELLITE_GAP + Math.max(outerSize, innerSize);
    wheel.escortSpacing = spacing;
    const outerTrack = spacing * Math.hypot(ESCORT_BESIDE[0], ESCORT_BEHIND[0]);
    wheel.reach = outerTrack + outerSize;
  } else {
    wheel.innerTrack = wheel.art.size + SATELLITE_GAP + innerSize;
    wheel.outerTrack = wheel.innerTrack + innerSize + SATELLITE_GAP + outerSize;
    wheel.reach = wheel.outerTrack + outerSize;
  }
  wheel.spokeLength = SPOKE_CLEARANCE + random() * SPOKE_SPREAD + wheel.reach;
  wheel.extent = wheel.spokeLength + wheel.reach;
}

const plannedWheels = RINGS.flatMap((entries, ringIndex) => entries.map((entry, slot) => {
  const [notableType, side] = entry.split(' ');
  return { ...wheelPlan(notableType), ringIndex, slot, slotCount: entries.length, inside: side === 'i' };
}));
plannedWheels.forEach(shapeWheel);

const extentOf = (ringIndex, inside) => Math.max(BELT_HALF_WIDTH,
  ...plannedWheels.filter((wheel) => wheel.ringIndex === ringIndex && wheel.inside === inside).map((wheel) => wheel.extent));
const ringRadii = [];
RING_NODE_COUNTS.forEach((_, ringIndex) => {
  if (ringIndex === 0) {
    ringRadii.push(Math.round(STAR_RADIUS + STAR_CLEARANCE + extentOf(0, true)));
    return;
  }
  const outward = extentOf(ringIndex - 1, false);
  const inward = extentOf(ringIndex, true);
  const gap = Math.max((outward + RING_CLEARANCE + inward) / 2, Math.max(outward, inward) + BELT_HALF_WIDTH + BELT_CLEARANCE);
  ringRadii.push(Math.round(ringRadii[ringIndex - 1] + gap));
});

function ringSpeed(ringIndex) {
  return round(INNER_RING_SPEED * Math.pow(ringRadii[0] / ringRadii[ringIndex], 1.5) * 1000) / 1000;
}

const root = { id: ROOT_ID, type: ROOT_ID, connectedTo: [], x: 0, y: 0 };
nodes.push(root);
nodeById.set(ROOT_ID, root);
orbits.push({ id: ROOT_ID, art: { kind: 'star', size: STAR_RADIUS } });

const rings = ringRadii.map((radius, ringIndex) => {
  const count = RING_NODE_COUNTS[ringIndex];
  const phase = random() * 360;
  const type = RING_HIGHWAYS[ringIndex];
  const speed = ringSpeed(ringIndex);
  const ringNodes = [];
  for (let index = 0; index < count; index++) {
    const angle = phase + index * 360 / count;
    const node = addNode(type, radius * Math.cos(toRadians(angle)), radius * Math.sin(toRadians(angle)), [], {
      around: ROOT_ID, speed, colour: HIGHWAY_COLOUR, art: { kind: 'sprite', path: pick(HIGHWAY_ROCKS), size: HIGHWAY_ROCK_SIZE, facing: 'spin' },
    });
    ringNodes.push({ node, angle });
  }
  ringNodes.forEach(({ node }, index) => link(node, ringNodes[(index + 1) % count].node));
  return { radius, speed, nodes: ringNodes, wheels: [], insideSpokes: [], usedSpokes: new Set() };
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

function satelliteOffsets(wheel, spokeAngle) {
  const phases = [random() * Math.PI * 2, random() * Math.PI * 2];
  if (!wheel.convoy) {
    return [wheel.outerTrack, wheel.innerTrack].map((track, order) => ({ x: track * Math.cos(phases[order]), y: track * Math.sin(phases[order]) }));
  }
  const outwardX = Math.cos(toRadians(spokeAngle));
  const outwardY = Math.sin(toRadians(spokeAngle));
  const travelX = -outwardY;
  const travelY = outwardX;
  const highwaySide = wheel.inside ? 1 : -1;
  return [highwaySide, -highwaySide].map((side, order) => ({
    x: wheel.escortSpacing * (side * ESCORT_BESIDE[order] * outwardX - ESCORT_BEHIND[order] * travelX),
    y: wheel.escortSpacing * (side * ESCORT_BESIDE[order] * outwardY - ESCORT_BEHIND[order] * travelY),
  }));
}

const placedWheels = [];
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
  }).filter((index, order, all) => all.indexOf(index) === order && !ring.usedSpokes.has(index));
  const spokeIndex = candidates.find((index) => !clashes(centreOf(index), wheel.reach));
  if (spokeIndex === undefined) throw new Error(`No clear spoke for ${wheel.notableType} on ring ${wheel.ringIndex}`);
  ring.usedSpokes.add(spokeIndex);
  const spoke = ring.nodes[spokeIndex];
  const { x: centreX, y: centreY } = centreOf(spokeIndex);
  placedWheels.push({ x: centreX, y: centreY, reach: wheel.reach });
  const notable = addNode(wheel.notableType, centreX, centreY, wheel.tags, { around: ROOT_ID, speed: ring.speed, colour: wheel.colour, art: wheel.art });
  const offsets = satelliteOffsets(wheel, spoke.angle);
  const satellites = offsets.map((offset, order) => {
    const smallAnomaly = wheel.smallAnomalies[order];
    const speedRange = order === 0 ? OUTER_SATELLITE_SPEED : INNER_SATELLITE_SPEED;
    const speed = wheel.convoy ? 0 : round(between(speedRange) * 10) / 10;
    return addNode(wheel.smallTypes[order], centreX + offset.x, centreY + offset.y, [smallAnomaly], {
      around: notable.id, speed, colour: ANOMALIES[smallAnomaly].colour, art: wheel.smallArt[order],
    });
  });
  link(spoke.node, satellites[0]);
  link(satellites[0], satellites[1]);
  link(satellites[1], notable);
  ring.wheels.push({ x: centreX, y: centreY, reach: wheel.reach, type: wheel.notableType, angle: spoke.angle, inside: wheel.inside, distance: centreDistance });
  if (wheel.inside) ring.insideSpokes.push(spoke.node.id);
}

const overlaps = [];
const allWheels = rings.flatMap((ring) => ring.wheels);
allWheels.forEach((first, index) => allWheels.slice(index + 1).forEach((second) => {
  const distance = Math.hypot(first.x - second.x, first.y - second.y);
  if (distance < first.reach + second.reach + WHEEL_CLEARANCE) overlaps.push(`${first.type} and ${second.type} (${Math.round(distance)})`);
}));
if (overlaps.length > 0) throw new Error(`Wheels overlap: ${overlaps.join(', ')}`);

function wheelsBelow(ringIndex) {
  const inside = rings[ringIndex].wheels.filter((wheel) => wheel.inside);
  return ringIndex === 0 ? inside : [...inside, ...rings[ringIndex - 1].wheels.filter((wheel) => !wheel.inside)];
}

function clearOfWheels(wheels, angle) {
  return wheels.every(({ angle: wheelAngle, distance, reach }) => {
    const clearance = Math.asin(Math.min(1, (reach + WHEEL_CLEARANCE) / distance)) * 180 / Math.PI;
    return angularGap(angle, wheelAngle) > clearance;
  });
}

function spreadAnchors(ringIndex, count) {
  const ring = rings[ringIndex];
  const below = wheelsBelow(ringIndex);
  const chosen = [];
  const phase = random() * 360;
  for (let anchor = 0; anchor < count; anchor++) {
    const target = phase + anchor * 360 / count;
    const candidates = ring.nodes
      .filter(({ node }) => !ring.insideSpokes.includes(node.id) && !chosen.some((picked) => picked.node === node))
      .sort((first, second) => (clearOfWheels(below, second.angle) - clearOfWheels(below, first.angle))
        || angularGap(first.angle, target) - angularGap(second.angle, target));
    if (candidates.length === 0) throw new Error(`No clear tether anchor on ring at ${ring.radius}`);
    chosen.push(candidates[0]);
  }
  return chosen;
}

spreadAnchors(0, ROOT_RAYS).forEach(({ node }) => link(root, node));
for (let ringIndex = 1; ringIndex < rings.length; ringIndex++) {
  const below = rings[ringIndex - 1];
  spreadAnchors(ringIndex, TETHERS_INTO_RING[ringIndex]).forEach(({ node, angle }) => {
    const landing = [...below.nodes].sort((first, second) => angularGap(first.angle, angle) - angularGap(second.angle, angle))[0];
    link(node, landing.node);
  });
}

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
writeFileSync(ORBITS_PATH, withCrlf({ orbits }));

console.log(`${nodes.length} nodes, ${plannedWheels.length} wheels, rings ${rings.map((ring) => `${ring.nodes.length}@${ring.radius}`).join(' / ')}`);
console.log('chance nodes', chanceCounts);
const blockedAnchors = rings.flatMap((ring, ringIndex) => ring.nodes
  .filter(({ node }) => node.connectedTo.some((id) => id === ROOT_ID || (ringIndex > 0 && rings[ringIndex - 1].nodes.some(({ node: lower }) => lower.id === id))))
  .filter(({ angle }) => !clearOfWheels(wheelsBelow(ringIndex), angle)));
console.log('ring links crossing a wheel', blockedAnchors.length);
