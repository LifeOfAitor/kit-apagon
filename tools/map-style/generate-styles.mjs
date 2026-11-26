// Generates the light and dark map styles from the Protomaps basemap definitions (decision 0010).
// Every external resource points inside the app (asset://) so the map works without a network.
// The tile source URL is a placeholder that the app replaces with the installed package path.

import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { layers, namedFlavor } from "@protomaps/basemaps";

const PACKAGE_MAP_PLACEHOLDER = "pmtiles://PACKAGE_MAP_URL";
const LABEL_LANGUAGE = "es";
const FLAVOR_NAMES = ["light", "dark"];
const TRANSLATED_NAME_KEY = `name:${LABEL_LANGUAGE}`;

const outputDirectory = join(
  dirname(fileURLToPath(import.meta.url)),
  "../../feature/map/src/main/assets/map/style",
);

// For names already written in the Latin alphabet, Protomaps shows the Spanish translation first
// ("Irún", "Fuenterrabía") and the local name only as a fallback. Decision 0010 asks for the
// official local name ("Irun", "Hondarribia"), so in exactly that expression the local name goes
// first and the translation becomes the fallback. Names in other alphabets keep their translation.
function isTranslationFirstForLatinNames(expression) {
  return (
    expression.length === 4 &&
    expression[0] === "coalesce" &&
    isGet(expression[1], TRANSLATED_NAME_KEY) &&
    isGet(expression[2], `pgf:${localNameKey(expression[3])}`) &&
    localNameKey(expression[3]) !== undefined
  );
}

function isGet(expression, key) {
  return Array.isArray(expression) && expression.length === 2 && expression[0] === "get" && expression[1] === key;
}

function localNameKey(expression) {
  const isLocalNameGet =
    Array.isArray(expression) && expression[0] === "get" && /^name[0-9]?$/.test(expression[1]);
  return isLocalNameGet ? expression[1] : undefined;
}

function preferLocalNames(node) {
  if (Array.isArray(node)) {
    const expression = node.map(preferLocalNames);
    return isTranslationFirstForLatinNames(expression)
      ? ["coalesce", expression[2], expression[3], expression[1]]
      : expression;
  }
  if (node !== null && typeof node === "object") {
    return Object.fromEntries(Object.entries(node).map(([key, value]) => [key, preferLocalNames(value)]));
  }
  return node;
}

function buildStyle(flavorName) {
  return {
    version: 8,
    sources: {
      protomaps: { type: "vector", url: PACKAGE_MAP_PLACEHOLDER },
    },
    layers: preferLocalNames(layers("protomaps", namedFlavor(flavorName), { lang: LABEL_LANGUAGE })),
    glyphs: "asset://map/fonts/{fontstack}/{range}.pbf",
    sprite: `asset://map/sprites/${flavorName}`,
  };
}

mkdirSync(outputDirectory, { recursive: true });
for (const flavorName of FLAVOR_NAMES) {
  const outputFile = join(outputDirectory, `${flavorName}.json`);
  writeFileSync(outputFile, `${JSON.stringify(buildStyle(flavorName))}\n`);
  console.log(`Generated ${outputFile}`);
}
