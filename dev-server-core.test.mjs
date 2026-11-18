import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  classifyJavaSource,
  filterModules,
  scanArgonModules,
  summarizeCategories,
} from './dev-server-core.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

test('classifyJavaSource categorizes Create train and kinetic files accurately', () => {
  assert.equal(
    classifyJavaSource('src/main/java/net/minecraft/argon/create/EntityArgonTrain.java'),
    'Create: Trains & Schedules'
  );
  assert.equal(
    classifyJavaSource('src/main/java/net/minecraft/argon/create/KineticNetwork.java'),
    'Create: Kinetics & Rendering'
  );
  assert.equal(
    classifyJavaSource('src/main/java/net/minecraft/src/GuiArgonSkinMaker.java'),
    'Skin Maker'
  );
  assert.equal(
    classifyJavaSource('src/main/java/net/minecraft/src/ArgonVoiceChatManager.java'),
    'Proximity Voice'
  );
  assert.equal(
    classifyJavaSource('src/main/java/net/minecraft/argon/portals/ArgonBetterPortals.java'),
    'BetterPortals (Seamless)'
  );
  assert.equal(
    classifyJavaSource('src/main/java/net/minecraft/argon/guns/ItemArgonGun.java'),
    'Guns & Techguns'
  );
  assert.equal(
    classifyJavaSource('src/main/java/net/minecraft/argon/tnt/ArgonExplosionScheduler.java'),
    'TNT & Explosion Scheduler'
  );
});

test('scanArgonModules discovers real Argon, Create, Portal, Gun, and TNT Java files in the repository', () => {
  const modules = scanArgonModules(__dirname);
  assert.ok(modules.length >= 86, `Expected >= 86 modules, got ${modules.length}`);
  assert.ok(modules.some((m) => m.name === 'EntityArgonTrain'));
  assert.ok(modules.some((m) => m.name === 'GuiArgonSettings'));
  assert.ok(modules.some((m) => m.name === 'ItemArgonGun' && m.category === 'Guns & Techguns'));
  assert.ok(modules.some((m) => m.name === 'EntityArgonSoldier' && m.category === 'Guns & Techguns'));
  assert.ok(modules.some((m) => m.name === 'TileEntityAutoTurret' && m.category === 'Guns & Techguns'));
  assert.ok(modules.some((m) => m.name === 'ArgonExplosionScheduler' && m.category === 'TNT & Explosion Scheduler'));
  assert.ok(modules.some((m) => m.name === 'EntityArgonTNTPrimed' && m.category === 'TNT & Explosion Scheduler'));
  assert.ok(modules.some((m) => m.name === 'TntEffects' && m.category === 'TNT & Explosion Scheduler'));
});

test('filterModules and summarizeCategories filter by category and search term', () => {
  const sample = [
    { name: 'EntityArgonTrain', path: 'a/EntityArgonTrain.java', category: 'Create: Trains & Schedules', bytes: 100 },
    { name: 'KineticNetwork', path: 'a/KineticNetwork.java', category: 'Create: Kinetics & Rendering', bytes: 200 },
    { name: 'ArgonSkinManager', path: 'b/ArgonSkinManager.java', category: 'Skin Maker', bytes: 300 },
  ];

  const trainsOnly = filterModules(sample, 'Create: Trains & Schedules', '');
  assert.equal(trainsOnly.length, 1);
  assert.equal(trainsOnly[0].name, 'EntityArgonTrain');

  const queryMatch = filterModules(sample, 'All', 'skin');
  assert.equal(queryMatch.length, 1);
  assert.equal(queryMatch[0].name, 'ArgonSkinManager');

  const summary = summarizeCategories(sample);
  assert.equal(summary.length, 3);
});
