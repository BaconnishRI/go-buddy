import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const GAME_MASTER_URL =
  'https://raw.githubusercontent.com/PokeMiners/game_masters/master/latest/latest.json';

const outPath = join(
  dirname(fileURLToPath(import.meta.url)),
  '..', 'app', 'src', 'main', 'assets', 'species.json',
);

console.log('Downloading game master (~20 MB)...');
const gm = await (await fetch(GAME_MASTER_URL)).json();

const seen = new Map();
for (const t of gm) {
  const ps = t.data?.pokemonSettings;
  if (!ps || ps.kmBuddyDistance === undefined) continue;
  if (!ps.stats || ps.stats.baseAttack === undefined) continue;
  const m = /^V(\d{4})_POKEMON_/.exec(t.templateId);
  if (!m) continue;
  if (seen.has(ps.pokemonId)) continue;
  const name = ps.pokemonId
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
  const family = ps.familyId
    ? ps.familyId
        .replace(/^FAMILY_/, '')
        .toLowerCase()
        .split('_')
        .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
        .join(' ')
    : name;
  seen.set(ps.pokemonId, {
    dex: parseInt(m[1], 10),
    name,
    km: ps.kmBuddyDistance,
    atk: ps.stats.baseAttack,
    def: ps.stats.baseDefense,
    sta: ps.stats.baseStamina,
    family,
  });
}

const list = [...seen.values()].sort(
  (a, b) => a.dex - b.dex || a.name.localeCompare(b.name),
);
writeFileSync(outPath, '[\n' + list.map((s) => JSON.stringify(s)).join(',\n') + '\n]\n');
console.log(`Wrote ${list.length} species to ${outPath}`);
