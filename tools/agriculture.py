"""Builds the farming blocks, their entities and the seeds from the agriculture atlas (assets/textures/agriculture.png).

The atlas holds one crop per row, its seed then its four stages, and the two tilled soils on the last row.
The item icons read it whole, through an .atlas; the blocks need one tile per file, cut from it here.
"""
import json, os, sys
from PIL import Image

root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..")
M = "CoreSampleGameplay:"

# row order of the atlas: (crop item, block prefix, display name, seed name)
CROPS = [
    ("ble", "Ble", "Blé", "Graines de blé"),
    ("pommeDeTerre", "PommeDeTerre", "Pomme de terre", "Germes de pomme de terre"),
    ("carotte", "Carotte", "Carotte", "Graines de carotte"),
    ("oignon", "Oignon", "Oignon", "Bulbilles d'oignon"),
    ("riz", "Riz", "Riz", "Graines de riz"),
    ("courge", "Courge", "Courge", "Graines de courge"),
    ("soja", "Soja", "Soja", "Graines de soja"),
]
STAGES = ["semis", "jeune pousse", "plant", "mûr"]
SOILS = [("TerreLabouree", "Terre labourée"), ("TerreLaboureeHumide", "Terre labourée humide")]

def path(*parts):
    p = os.path.join(root, *parts)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p

def dump(obj, *parts):
    with open(path(*parts), "w") as f:
        json.dump(obj, f, indent=4, ensure_ascii=False)
        f.write("\n")

atlas = Image.open(path("assets/textures/agriculture.png")).convert("RGBA")
def tile(col, row, name):
    atlas.crop((16 * col, 16 * row, 16 * col + 16, 16 * row + 16)).save(path("assets/blockTiles/agriculture", name + ".png"))

names = []
for row, (crop, block, name, seed_name) in enumerate(CROPS):
    seed = "graines" + block
    names += [seed] + [f"{block}Stade{s}" for s in range(1, 5)]
    dump({
        "parent": "engine:iconItem",
        "DisplayName": {"name": seed_name},
        "Item": {"icon": M + "agriculture#" + seed, "stackId": M + seed, "maxStackSize": 99, "consumedOnUse": True},
        "Seed": {"plant": f"{M}{block}Stade1"},
    }, "assets/prefabs/farming/seeds", seed + ".prefab")
    for s in range(1, 5):
        stage = f"{block}Stade{s}"
        tile(s, row, stage)
        ripe = s == 4
        dump({
            "Crop": {"next": "" if ripe else f"{M}{block}Stade{s + 1}", "seed": M + seed},
            "DropGrammar": {"itemDrops": [f"1-3*{M}{crop}", f"1-2*{M}{seed}"] if ripe else [f"1*{M}{seed}"]},
        }, "assets/prefabs/farming/crops", stage + "Entity.prefab")
        definition = {
            "basedOn": "CoreAssets:plant",
            "displayName": f"{name} — {STAGES[s - 1]}",
            "tile": M + stage,
            "entity": {"prefab": f"{M}{stage}Entity", "keepActive": True},
        }
        # Rice stands in a sheet of water drawn into its tile; a waving top would tear it from its own stalks.
        if crop != "riz":
            definition["waving"] = True
        dump(definition, "assets/blocks/agriculture", stage + ".block")

names += [s for s, _ in SOILS] + [""] * 3
for col, (soil, name) in enumerate(SOILS):
    tile(col, len(CROPS), soil)
    dump({
        "basedOn": "CoreAssets:soil",
        "displayName": name,
        "tile": "CoreAssets:Dirt",
        "tiles": {"top": M + soil},
        "entity": {"prefab": M + "TerreLaboureeEntity", "keepActive": True},
    }, "assets/blocks/agriculture", soil + ".block")
dump({"TilledSoil": {}, "DropGrammar": {"blockDrops": ["CoreAssets:Dirt"]}},
     "assets/prefabs/farming", "TerreLaboureeEntity.prefab")

dump({"texture": M + "agriculture", "textureSize": list(atlas.size),
      "grid": {"tileSize": [16, 16], "gridDimensions": [5, len(CROPS) + 1], "gridOffset": [0, 0], "tileNames": names}},
     "assets/atlas", "agriculture.atlas")
