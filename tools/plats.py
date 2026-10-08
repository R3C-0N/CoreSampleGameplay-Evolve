"""Builds the crops, the dishes and their recipes from the icon generator's output (sprites.js)."""
import json, os, re, sys
from PIL import Image

src, root = sys.argv[1], sys.argv[2]
items = json.load(open(src))

def camel(slug):
    parts = slug.split("-")
    return parts[0] + "".join(p[:1].upper() + p[1:] for p in parts[1:])

M = "Bestiaire:viande"
CROP = {"Blé": "ble", "Pomme de terre": "pommeDeTerre", "Carotte": "carotte", "Oignon": "oignon",
        "Riz": "riz", "Courge": "courge", "Soja": "soja"}
def c(name): return "CoreSampleGameplay:" + CROP[name]
TOFU = "CoreSampleGameplay:tofu"
# name: (nourishment or None, ingredients or None (raw crop), fuel?)
D = {
    # cultures : crues, seules la pomme de terre, la carotte, l'oignon et la courge se mangent
    "Blé": (None, None), "Pomme de terre": (3, None), "Carotte": (4, None), "Oignon": (2, None),
    "Riz": (None, None), "Courge": (5, None), "Soja": (None, None),
    "Pain": (12, ["2*" + c("Blé")]),
    "Tourte de gibier": (50, ["1*" + c("Blé"), "1*" + M + "Cerf"]),
    "Tourte au tofu": (50, ["1*" + c("Blé"), "1*" + TOFU]),
    "Pâté de lapin en croûte": (42, ["1*" + c("Blé"), "1*" + M + "Lapin", "1*" + c("Oignon")]),
    "Pâté de tofu en croûte": (42, ["1*" + c("Blé"), "1*" + TOFU, "1*" + c("Oignon")]),
    "Pain aux oignons": (24, ["2*" + c("Blé"), "1*" + c("Oignon")]),
    "Pomme de terre au four": (10, ["1*" + c("Pomme de terre")]),
    "Hachis d'ours": (65, ["1*" + c("Pomme de terre"), "1*" + M + "Ours"]),
    "Hachis de tofu": (65, ["1*" + c("Pomme de terre"), "1*" + TOFU]),
    "Gratin de sanglier": (58, ["1*" + c("Pomme de terre"), "1*" + M + "Sanglier", "1*" + c("Oignon")]),
    "Gratin de tofu": (58, ["1*" + c("Pomme de terre"), "1*" + TOFU, "1*" + c("Oignon")]),
    "Purée de carottes": (24, ["1*" + c("Pomme de terre"), "1*" + c("Carotte")]),
    "Carottes rôties": (10, ["1*" + c("Carotte")]),
    "Ragoût de vache": (72, ["1*" + c("Carotte"), "1*" + M + "Vache", "1*" + c("Pomme de terre")]),
    "Ragoût de tofu": (72, ["1*" + c("Carotte"), "1*" + TOFU, "1*" + c("Pomme de terre")]),
    "Lapin aux carottes": (30, ["1*" + c("Carotte"), "1*" + M + "Lapin"]),
    "Tofu aux carottes": (30, ["1*" + c("Carotte"), "1*" + TOFU]),
    "Soupe de légumes": (36, ["1*" + c("Carotte"), "1*" + c("Oignon"), "1*" + c("Courge")]),
    "Oignons confits": (8, ["2*" + c("Oignon")]),
    "Soupe à l'oignon": (26, ["1*" + c("Oignon"), "1*CoreSampleGameplay:pain"]),
    "Brochette de mouflon": (40, ["1*" + c("Oignon"), "1*" + M + "Mouflon"]),
    "Brochette de tofu": (40, ["1*" + c("Oignon"), "1*" + TOFU]),
    "Faisan farci": (40, ["1*" + c("Oignon"), "1*" + M + "Faisan", "1*" + c("Riz")]),
    "Tofu farci": (40, ["1*" + c("Oignon"), "1*" + TOFU, "1*" + c("Riz")]),
    "Riz cuit": (10, ["1*" + c("Riz")]),
    "Riz sauté au lézard": (36, ["1*" + c("Riz"), "1*" + M + "Lezard"]),
    "Riz sauté au tofu": (36, ["1*" + c("Riz"), "1*" + TOFU]),
    "Boulettes de loup": (52, ["1*" + c("Riz"), "1*" + M + "Loup", "1*" + c("Oignon")]),
    "Boulettes de tofu": (52, ["1*" + c("Riz"), "1*" + TOFU, "1*" + c("Oignon")]),
    "Galettes de riz aux carottes": (24, ["1*" + c("Riz"), "1*" + c("Carotte")]),
    "Courge rôtie": (12, ["1*" + c("Courge")]),
    "Velouté de courge": (26, ["1*" + c("Courge"), "1*" + c("Carotte")]),
    "Tarte à la courge": (30, ["1*" + c("Courge"), "1*" + c("Blé")]),
    "Courge farcie": (68, ["1*" + c("Courge"), "1*" + M + "Pure", "1*" + c("Riz")]),
    "Courge farcie au tofu": (68, ["1*" + c("Courge"), "1*" + TOFU, "1*" + c("Riz")]),
    "Tofu": (12, ["2*" + c("Soja")]),
    "Edamame grillés": (10, ["1*" + c("Soja")]),
    "Tofu grillé": (30, ["1*" + TOFU]),
    "Soupe miso": (34, ["1*" + c("Soja"), "1*" + c("Riz"), "1*" + c("Oignon")]),
    "Tofu sauté aux légumes": (58, ["1*" + TOFU, "1*" + c("Carotte"), "1*" + c("Oignon")]),
}
FUEL = "1*CoreAssets:Coal|CoreAssets:Plank"
names = [i["name"] for i in items]
assert set(names) == set(D), (set(names) ^ set(D))

cols = 8
rows = (len(items) + cols - 1) // cols
atlas = Image.new("RGBA", (16 * cols, 16 * rows), (0, 0, 0, 0))
tiles = []
for k, it in enumerate(items):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for i, col in enumerate(it["px"]):
        if col:
            img.putpixel((i % 16, i // 16), tuple(int(col[j:j + 2], 16) for j in (1, 3, 5)) + (255,))
    atlas.paste(img, ((k % cols) * 16, (k // cols) * 16))
    tiles.append(camel(it["id"]) if it["name"] not in CROP else CROP[it["name"]])
atlas.save(os.path.join(root, "assets/textures/plats.png"))
json.dump({"texture": "CoreSampleGameplay:plats", "textureSize": [16 * cols, 16 * rows],
           "grid": {"tileSize": [16, 16], "gridDimensions": [cols, rows], "gridOffset": [0, 0],
                    "tileNames": tiles + [""] * (cols * rows - len(tiles))}},
          open(os.path.join(root, "assets/atlas/plats.atlas"), "w"), indent=4, ensure_ascii=False)

os.makedirs(os.path.join(root, "assets/prefabs/food/plats"), exist_ok=True)
os.makedirs(os.path.join(root, "assets/prefabs/food/cultures"), exist_ok=True)
for it, tid in zip(items, tiles):
    name = it["name"]
    food, ingredients = D[name]
    crop = name in CROP
    p = {"parent": "engine:iconItem", "DisplayName": {"name": name},
         "Item": {"icon": "CoreSampleGameplay:plats#" + tid, "stackId": "CoreSampleGameplay:" + tid, "maxStackSize": 99}}
    if food is not None:
        p["DisplayName"]["description"] = f"Se mange au clic droit. Rassasie de {food}."
        p["Item"]["consumedOnUse"] = True
        p["Food"] = {"nourishment": food}
    sub = "cultures" if crop else "plats"
    json.dump(p, open(os.path.join(root, f"assets/prefabs/food/{sub}/{tid}.prefab"), "w"), indent=4, ensure_ascii=False)
    if ingredients:
        # Le tofu se presse a la main ; tout le reste cuit au fourneau, avec un combustible.
        r = {"Recipe": {"ingredients": ingredients + ([] if name == "Tofu" else [FUEL]),
                        "result": "CoreSampleGameplay:" + tid}}
        if name != "Tofu":
            r["Recipe"]["station"] = "furnace"
        rid = "RecipePlat" + tid[0].upper() + tid[1:]
        json.dump(r, open(os.path.join(root, f"assets/prefabs/recipes/{rid}.prefab"), "w"), indent=4, ensure_ascii=False)
print(len(items), "objets", sum(1 for n in D if D[n][1]), "recettes")
