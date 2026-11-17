import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..');
const TREE_PATH = resolve(repoRoot, 'data/skilltrees/atlas_tree.json');
const ORBITS_PATH = resolve(repoRoot, 'data/skilltrees/atlas_orbits.json');

const ROOT_ID = 'atlas_root';
const STAR_RADIUS = 260;
const RING_RADII = [1000, 2000, 3000, 4000, 5000];
const RING_NODE_COUNTS = [8, 12, 18, 20, 24];
const INNER_RING_SPEED = 1.2;
const RING_HIGHWAYS = ['travel_boss_star_charts', 'travel_boss_star_charts', 'travel_catalysts', 'travel_catalysts', 'travel_chart_modifier_effect'];
const HIGHWAY_COLOURS = {
  travel_boss_star_charts: '#b4d2ff',
  travel_catalysts: '#ffc85a',
  travel_chart_modifier_effect: '#e68cff',
};
const HIGHWAY_ROCKS = Array.from({ length: 12 }, (_, index) => `graphics/asteroids/ring_asteroid${String(index).padStart(2, '0')}.png`);
const HIGHWAY_ROCK_SIZE = 16;
const ROOT_RAYS = 3;
const TETHERS_INTO_RING = [0, 3, 4, 5, 6];
const SPOKE_LENGTH_MIN = 95;
const SPOKE_LENGTH_SPREAD = 70;
const SATELLITE_GAP = 42;
const INNER_SATELLITE_SPEED = [8, 13];
const OUTER_SATELLITE_SPEED = [4.5, 7.5];

const ANOMALIES = {
  excavation_sites: {
    colour: '#ffaa50',
    chance: 'excavation_sites_chance',
    others: ['additional_site', 'inhibitor_quantity'],
    small: (pick) => ({ kind: 'planet', planet: pick(['barren', 'barren2', 'barren3', 'rocky_ice', 'frozen1']), size: 15 }),
    notable: (pick) => ({ kind: 'planet', planet: pick(['barren', 'barren_castiron', 'rocky_metallic', 'rocky_unstable', 'barren-bombarded', 'cryovolcanic', 'barren-desert', 'rocky_ice']), size: 50 }),
  },
  supply_convoy: {
    colour: '#78dc8c',
    chance: 'supply_convoy_chance',
    others: ['threat_feedstock_chance', 'shroud_feedstock_chance', 'phase_feedstock_chance', 'feedstock_duplication', 'additional_convoy_chance'],
    small: (pick) => ({ kind: 'sprite', path: pick(['graphics/ships/buffalo/buffalo_base.png', 'graphics/ships/hermes/hermes_base.png', 'graphics/ships/tarsus/tarsus_base.png', 'graphics/ships/mule/mule_base.png']), size: 20, facing: 'travel' }),
    notable: () => ({ kind: 'fleet', paths: ['graphics/ships/atlas/atlas.png', 'graphics/ships/buffalo/buffalo_base.png', 'graphics/ships/buffalo/buffalo_base.png'], size: 58 }),
  },
  relay_network: {
    colour: '#6ec8ff',
    chance: 'relay_network_chance',
    others: ['relay_reward_rarity'],
    small: () => ({ kind: 'sprite', path: 'graphics/ships/nav_buoy.png', size: 17, facing: 'spin' }),
    notable: (pick) => ({ kind: 'sprite', path: pick(['graphics/ships/com_relay.png', 'graphics/ships/sensor_array.png']), size: 50, facing: 'spin' }),
  },
  spatial_breach: {
    colour: '#be78ff',
    chance: 'spatial_breach_chance',
    others: ['breach_cache_quantity', 'breach_cache_items', 'warped_chance'],
    small: () => ({ kind: 'breach', size: 17 }),
    notable: () => ({ kind: 'breach', size: 52 }),
  },
  anomalous_debris: {
    colour: '#ff6e5a',
    chance: 'anomalous_debris_chance',
    others: ['additional_debris', 'engram_quantity'],
    small: (pick) => ({ kind: 'sprite', path: pick(['graphics/asteroids/asteroid1.png', 'graphics/asteroids/asteroid2.png', 'graphics/asteroids/asteroid3.png', 'graphics/asteroids/asteroid4.png']), size: 19, facing: 'spin' }),
    notable: (pick) => ({ kind: 'fleet', paths: pick([
      ['graphics/ships/derelict/derelict_warden.png', 'graphics/ships/derelict/derelict_sentry.png', 'graphics/ships/derelict/derelict_picket.png'],
      ['graphics/ships/derelict/derelict_bastillon.png', 'graphics/ships/derelict/derelict_defender.png', 'graphics/ships/derelict/derelict_defender.png'],
      ['graphics/ships/derelict/derelict_berserker.png', 'graphics/ships/derelict/derelict_picket.png', 'graphics/ships/derelict/derelict_sentry.png'],
      ['graphics/ships/derelict/derelict_rampart.png', 'graphics/ships/derelict/derelict_warden.png', 'graphics/ships/derelict/derelict_picket.png'],
    ]), size: 58 }),
  },
  listening_post: {
    colour: '#f0dc6e',
    chance: 'listening_post_chance',
    others: ['vault_quantity', 'slow_purge'],
    small: (pick) => ({ kind: 'sprite', path: pick(['graphics/ships/wolf/wolf_base.png', 'graphics/ships/lasher/lasher_base.png']), size: 17, facing: 'travel' }),
    notable: (pick) => ({ kind: 'sprite', path: pick(['graphics/stations/station_side00.png', 'graphics/stations/station_side01.png', 'graphics/stations/station_side02.png', 'graphics/stations/station_side03.png', 'graphics/stations/station_side04.png']), size: 58, facing: 'spin' }),
  },
};

const KEYSTONE_ART = {
  silent_choir: { kind: 'sprite', path: 'graphics/stations/cryosleeper.png', size: 92, facing: 'spin' },
  mixed_manifest: { kind: 'sprite', path: 'graphics/stations/station_midline1.png', size: 92, facing: 'spin' },
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
const round = (value) => Math.round(value * 10) / 10;
const toRadians = (degrees) => degrees * Math.PI / 180;
const angularGap = (first, second) => Math.abs(((first - second + 540) % 360) - 180);

function ringSpeed(ringIndex) {
  return round(INNER_RING_SPEED * Math.pow(RING_RADII[0] / RING_RADII[ringIndex], 1.5) * 1000) / 1000;
}

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

const root = { id: ROOT_ID, type: ROOT_ID, connectedTo: [], x: 0, y: 0 };
nodes.push(root);
nodeById.set(ROOT_ID, root);
orbits.push({ id: ROOT_ID, art: { kind: 'star', size: STAR_RADIUS } });

const rings = RING_RADII.map((radius, ringIndex) => {
  const count = RING_NODE_COUNTS[ringIndex];
  const phase = random() * 360;
  const type = RING_HIGHWAYS[ringIndex];
  const speed = ringSpeed(ringIndex);
  const ringNodes = [];
  for (let index = 0; index < count; index++) {
    const angle = phase + index * 360 / count;
    const node = addNode(type, radius * Math.cos(toRadians(angle)), radius * Math.sin(toRadians(angle)), [], {
      around: ROOT_ID, speed, colour: HIGHWAY_COLOURS[type], art: { kind: 'sprite', path: pick(HIGHWAY_ROCKS), size: HIGHWAY_ROCK_SIZE, facing: 'spin' },
    });
    ringNodes.push({ node, angle });
  }
  ringNodes.forEach(({ node }, index) => link(node, ringNodes[(index + 1) % count].node));
  return { radius, speed, nodes: ringNodes, insideWheelAngles: [], insideSpokes: [], usedSpokes: new Set() };
});

function wheelPlan(notableType) {
  const hybrid = HYBRIDS[notableType];
  if (hybrid) {
    return {
      notableType,
      tags: [...new Set(notableType === 'event_horizon' ? ['black_hole', 'spatial_breach'] : hybrid.anomalies)],
      colour: hybrid.colour,
      art: { kind: 'planet', planet: hybrid.planet, size: 54 },
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
    art: KEYSTONE_ART[notableType] ?? ANOMALIES[anomaly].notable(pick),
    smallAnomalies: [anomaly, anomaly],
    hybrid: false,
  };
}

const plannedWheels = RINGS.flatMap((entries, ringIndex) => entries.map((entry, slot) => {
  const [notableType, side] = entry.split(' ');
  return { ...wheelPlan(notableType), ringIndex, slot, slotCount: entries.length, inside: side === 'i' };
}));

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

for (const wheel of plannedWheels) {
  const ring = rings[wheel.ringIndex];
  const step = ring.nodes.length / wheel.slotCount;
  const jitter = (random() - 0.5) * step * 0.35;
  let spokeIndex = Math.round(wheel.slot * step + jitter + ring.nodes.length) % ring.nodes.length;
  while (ring.usedSpokes.has(spokeIndex)) spokeIndex = (spokeIndex + 1) % ring.nodes.length;
  ring.usedSpokes.add(spokeIndex);
  const spoke = ring.nodes[spokeIndex];
  const bodySize = wheel.art.size;
  const innerTrack = bodySize + SATELLITE_GAP;
  const outerTrack = innerTrack + SATELLITE_GAP;
  const spokeLength = SPOKE_LENGTH_MIN + random() * SPOKE_LENGTH_SPREAD;
  const centreDistance = ring.radius + (wheel.inside ? -1 : 1) * (outerTrack + spokeLength);
  const angle = toRadians(spoke.angle);
  const centreX = centreDistance * Math.cos(angle);
  const centreY = centreDistance * Math.sin(angle);
  const tags = wheel.tags;
  const notable = addNode(wheel.notableType, centreX, centreY, tags, { around: ROOT_ID, speed: ring.speed, colour: wheel.colour, art: wheel.art });
  const satellites = [outerTrack, innerTrack].map((trackRadius, order) => {
    const smallAnomaly = wheel.smallAnomalies[order];
    const smallType = wheel.smallTypes[order];
    const phase = random() * Math.PI * 2;
    const speedRange = order === 0 ? OUTER_SATELLITE_SPEED : INNER_SATELLITE_SPEED;
    const art = ANOMALIES[smallAnomaly].small(pick);
    return addNode(smallType, centreX + trackRadius * Math.cos(phase), centreY + trackRadius * Math.sin(phase), [smallAnomaly], {
      around: notable.id, speed: round(between(speedRange) * 10) / 10, colour: ANOMALIES[smallAnomaly].colour, art,
    });
  });
  link(spoke.node, satellites[0]);
  link(satellites[0], satellites[1]);
  link(satellites[1], notable);
  if (wheel.inside) {
    ring.insideWheelAngles.push({ angle: spoke.angle, reach: outerTrack + spokeLength + outerTrack });
  }
  if (wheel.inside) ring.insideSpokes.push(spoke.node.id);
}

function clearOfInsideWheels(ring, angle) {
  return ring.insideWheelAngles.every(({ angle: wheelAngle, reach }) => {
    const clearance = Math.atan2(reach + 60, ring.radius - reach) * 180 / Math.PI;
    return angularGap(angle, wheelAngle) > clearance;
  });
}

function spreadAnchors(ring, count) {
  const chosen = [];
  const phase = random() * 360;
  for (let anchor = 0; anchor < count; anchor++) {
    const target = phase + anchor * 360 / count;
    const candidates = ring.nodes
      .filter(({ node }) => !ring.insideSpokes.includes(node.id) && !chosen.some((picked) => picked.node === node))
      .sort((first, second) => (clearOfInsideWheels(ring, second.angle) - clearOfInsideWheels(ring, first.angle))
        || angularGap(first.angle, target) - angularGap(second.angle, target));
    if (candidates.length === 0) throw new Error(`No clear tether anchor on ring at ${ring.radius}`);
    chosen.push(candidates[0]);
  }
  return chosen;
}

spreadAnchors(rings[0], ROOT_RAYS).forEach(({ node }) => link(root, node));
for (let ringIndex = 1; ringIndex < rings.length; ringIndex++) {
  const below = rings[ringIndex - 1];
  spreadAnchors(rings[ringIndex], TETHERS_INTO_RING[ringIndex]).forEach(({ node, angle }) => {
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

console.log(`${nodes.length} nodes, ${plannedWheels.length} wheels, rings ${rings.map((ring) => ring.nodes.length).join('/')}`);
console.log('chance nodes', chanceCounts);
