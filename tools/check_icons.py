"""Fails the build if an Android drawable is not a real VectorDrawable.

Guards against the crash that shipped in 0.1.0:

    Binary XML file line #1 <VectorGraphic> tag requires viewportWidth > 0

which happens when raw SVG (`<svg viewBox=...>`) ends up in res/drawable, or when
a vector is missing its viewport/width/height. Run: python3 tools/check_icons.py
"""
import pathlib
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
ROOT = pathlib.Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
DRAWABLE = RES / "drawable"

VECTOR_TAGS = {"vector", "animated-vector", "animated-selector", "selector", "layer-list", "ripple"}
ALLOWED_ROOTS = VECTOR_TAGS | {"shape", "inset", "clip", "bitmap", "nine-patch", "level-list",
                               "transition", "adaptive-icon", "animation-list", "color", "scale",
                               "animated-rotate", "font-family"}

errors = []
checked = 0

for path in sorted(RES.rglob("*.xml")):
    # values/, values-night/, values-v31/ ... are not drawables
    if any(part.startswith("values") for part in path.parts):
        continue
    try:
        tree = ET.parse(path)  # expat also rejects duplicate attributes
    except ET.ParseError as exc:
        errors.append(f"{path.relative_to(ROOT)}: not parseable XML ({exc})")
        continue

    root = tree.getroot()
    tag = root.tag.split("}")[-1]
    checked += 1

    if tag == "svg" or root.tag == "svg":
        errors.append(
            f"{path.relative_to(ROOT)}: raw <svg> in res/. Android needs a <vector> "
            "VectorDrawable - run tools/make_icons.py"
        )
        continue
    if tag not in ALLOWED_ROOTS:
        errors.append(f"{path.relative_to(ROOT)}: unexpected root <{tag}>")
        continue
    if tag != "vector":
        continue

    for attr in ("viewportWidth", "viewportHeight"):
        raw = root.get(ANDROID + attr)
        try:
            value = float(raw)
        except (TypeError, ValueError):
            errors.append(f"{path.relative_to(ROOT)}: android:{attr} missing or not a number ({raw!r})")
            continue
        if value <= 0:
            errors.append(f"{path.relative_to(ROOT)}: android:{attr} must be > 0 (got {value})")

    for attr in ("width", "height"):
        if not root.get(ANDROID + attr):
            errors.append(f"{path.relative_to(ROOT)}: android:{attr} missing")

    has_drawing = False
    for node in root.iter():
        node_tag = node.tag.split("}")[-1]
        if node_tag in {"path", "group", "clip-path"}:
            has_drawing = True
        if node_tag == "path":
            if not node.get(ANDROID + "pathData"):
                errors.append(f"{path.relative_to(ROOT)}: <path> without android:pathData")
            if not (node.get(ANDROID + "fillColor") or node.get(ANDROID + "strokeColor")):
                errors.append(f"{path.relative_to(ROOT)}: <path> without a fill or stroke colour")
    if not has_drawing:
        errors.append(f"{path.relative_to(ROOT)}: <vector> draws nothing")

print(f"checked {checked} drawable/resource xml file(s)")

if errors:
    print("\n".join(f"FAIL {e}" for e in errors))
    sys.exit(1)

print("all drawables are valid VectorDrawables")
