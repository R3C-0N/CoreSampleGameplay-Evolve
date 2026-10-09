"""Gives every tool, weapon and piece of armour its durability, and every metal one a repair at its station.

The durability is read from the material the item is named after, the first word of its prefab: one point is one
block broken with a tool, one blow landed with a weapon, four points of damage absorbed by armour — one at least per
blow. Wood, hide and flint have no repair: they are made again, from what the start of the game gives. Everything
forged from an ingot is repaired where it is made, with a third of its ingots.

Run it again after adding an item or changing the table: it rewrites the Durability component and the repair list,
and touches nothing else.
"""
import glob, json, os, re, sys

root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..")
M = "CoreSampleGameplay:"

# Longest prefix first: EtheriumMithril before Mithril, BoneStudded before Bone.
DURABILITY = [
    ("Etherium", 5000),
    ("Mithril", 3000), ("Orichalcum", 3000), ("Adamantine", 3000),
    ("Steel", 2000),
    ("Iron", 900),
    ("Bronze", 500),
    ("Copper", 300),
    ("BoneStudded", 220),
    ("Bone", 150), ("Flint", 150),
]
# Wood, log, plank, wicker, hide, and the items named after no material: a club, a bow, a tinker's kit.
BASE = 80

def durability(name):
    for prefix, points in DURABILITY:
        if name.startswith(prefix):
            return points
    return BASE

def load(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)

def dump(obj, p):
    with open(p, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=4, ensure_ascii=False)
        f.write("\n")

durable = set()
for p in sorted(glob.glob(os.path.join(root, "assets/prefabs/equipment/*/*.prefab"))):
    name = os.path.splitext(os.path.basename(p))[0]
    prefab = load(p)
    if prefab.get("Item", {}).get("maxStackSize", 1) > 1:
        continue  # arrows and bolts are spent, not worn
    points = durability(name)
    prefab["Durability"] = {"durability": points, "maxDurability": points}
    dump(prefab, p)
    durable.add(M + name)

repairs = 0
for p in sorted(glob.glob(os.path.join(root, "assets/prefabs/recipes/*.prefab"))):
    prefab = load(p)
    recipe = prefab.get("Recipe", {})
    if recipe.get("result") not in durable:
        continue
    recipe.pop("repair", None)
    for ingredient in recipe["ingredients"]:
        count, uri = ingredient.split("*", 1)
        if re.search(r"Ingot$", uri):
            recipe["repair"] = [f"{max(1, round(int(count) / 3))}*{uri}"]
            repairs += 1
            break
    dump(prefab, p)

print(f"{len(durable)} durable items, {repairs} repairs")
