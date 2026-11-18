import fs from 'node:fs';
import path from 'node:path';

export function classifyJavaSource(relPath) {
  const normalized = relPath.replace(/\\/g, '/');
  const fileName = path.posix.basename(normalized);

  if (normalized.includes('/net/minecraft/argon/tnt/')) {
    return 'TNT & Explosion Scheduler';
  }

  if (normalized.includes('/net/minecraft/argon/guns/')) {
    return 'Guns & Techguns';
  }

  if (normalized.includes('/net/minecraft/argon/portals/') || fileName.includes('Portal')) {
    return 'BetterPortals (Seamless)';
  }

  if (normalized.includes('/net/minecraft/argon/create/')) {
    if (fileName.includes('Train') || fileName.includes('Bogey') || fileName.includes('Schedule')) {
      return 'Create: Trains & Schedules';
    }
    if (fileName.includes('Piston') || fileName.includes('Gantry') || fileName.includes('SuperGlue') || fileName.includes('Contraption') || fileName.includes('Sail') || fileName.includes('Windmill')) {
      return 'Create: Contraptions';
    }
    if (fileName.includes('Fluid') || fileName.includes('Basin') || fileName.includes('Belt') || fileName.includes('Funnel') || fileName.includes('Press') || fileName.includes('Crushing') || fileName.includes('Blaze') || fileName.includes('Recipes')) {
      return 'Create: Logistics & Processing';
    }
    return 'Create: Kinetics & Rendering';
  }

  if (fileName.includes('Skin')) {
    return 'Skin Maker';
  }
  if (fileName.includes('Voice')) {
    return 'Proximity Voice';
  }
  if (fileName.includes('Waypoint')) {
    return 'Minimap & Waypoints';
  }
  if (fileName.includes('Argon')) {
    return 'Argon Settings & Core';
  }
  if (normalized.includes('/world/biome/') || normalized.includes('/world/gen/')) {
    return 'WorldGen & Biomes';
  }
  if (fileName.includes('Weather') || fileName.includes('Tornado') || fileName.includes('Radar')) {
    return 'Weather & Storms';
  }
  if (fileName.includes('Sound') || fileName.includes('Ambient') || fileName.includes('Item')) {
    return 'Audio & Visual Polish';
  }
  return 'Core Engine';
}

export function scanArgonModules(rootDir) {
  const javaRoot = path.join(rootDir, 'src', 'main', 'java');
  const targetDirs = [
    path.join(javaRoot, 'net', 'minecraft', 'argon'),
    path.join(javaRoot, 'net', 'minecraft', 'src'),
  ];

  const modules = [];

  function walk(dir) {
    if (!fs.existsSync(dir)) return;
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        walk(fullPath);
      } else if (entry.isFile() && entry.name.endsWith('.java')) {
        const relPath = path.relative(rootDir, fullPath).replace(/\\/g, '/');
        const category = classifyJavaSource(relPath);
        if (category !== 'Core Engine' || entry.name.startsWith('GuiArgon') || entry.name.startsWith('Argon')) {
          const stat = fs.statSync(fullPath);
          modules.push({
            name: entry.name.replace(/\.java$/, ''),
            fileName: entry.name,
            path: relPath,
            category,
            bytes: stat.size,
          });
        }
      }
    }
  }

  for (const dir of targetDirs) {
    walk(dir);
  }

  modules.sort((a, b) => a.category.localeCompare(b.category) || a.name.localeCompare(b.name));
  return modules;
}

export function filterModules(modules, category, query) {
  const normalizedCategory = (category || 'All').trim();
  const normalizedQuery = (query || '').trim().toLowerCase();

  return modules.filter((item) => {
    const matchesCategory = normalizedCategory === 'All' || item.category === normalizedCategory;
    if (!matchesCategory) return false;
    if (!normalizedQuery) return true;
    return (
      item.name.toLowerCase().includes(normalizedQuery) ||
      item.path.toLowerCase().includes(normalizedQuery) ||
      item.category.toLowerCase().includes(normalizedQuery)
    );
  });
}

export function summarizeCategories(modules) {
  const counts = new Map();
  for (const item of modules) {
    counts.set(item.category, (counts.get(item.category) || 0) + 1);
  }
  return Array.from(counts.entries())
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name));
}
