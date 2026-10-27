import catalog from "./mainland-cities.json";

export const mainlandCities = catalog.cities;
export const mainlandProvinces = catalog.provinces;
const index = new Map<string, (typeof mainlandCities)[number]>();
const aliases = new Map<string, typeof mainlandCities>();
for (const city of mainlandCities) {
  for (const key of [city.code, city.name, city.value]) index.set(key, city);
  aliases.set(city.alias, [...(aliases.get(city.alias) ?? []), city]);
}
for (const [alias, cities] of aliases)
  if (cities.length === 1 && !index.has(alias)) index.set(alias, cities[0]);

export function mainlandCity(value: string) { return index.get(value.trim()); }
export function normalizeCity(value: string) { return mainlandCity(value)?.value ?? value.trim(); }
export function sameCity(a: string, b: string) { return normalizeCity(a) === normalizeCity(b); }
export function cityDescription(city: (typeof mainlandCities)[number]) {
  return [mainlandProvinces.find(p => p.code === city.provinceCode)?.name, city.parentName].filter(Boolean).join(" · ");
}
export function searchMainlandCities(query = "", provinceCode = "") {
  const term = query.trim().toLowerCase().replaceAll(" ", "");
  return mainlandCities.filter(city => (!provinceCode || city.provinceCode === provinceCode) &&
    (!term || [city.code, city.name, city.value, city.alias, city.pinyin, cityDescription(city)].some(text => text.toLowerCase().includes(term))));
}
