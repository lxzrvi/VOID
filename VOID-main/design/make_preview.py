import re
import pathlib

BASE = pathlib.Path("/home/user/void")
mark = re.search(r'd="([^"]+)"', (BASE / "icon/void_inner_icon.svg").read_text()).group(1)


def glyph(x, y, size, kind, color):
    s = size / 24.0
    t = 'transform="translate(%s %s) scale(%s)"' % (x, y, s)
    if kind == "left":
        d = "M12 19l-7-7 7-7M19 12H5"
    elif kind == "right":
        d = "M12 5l7 7-7 7M5 12h14"
    elif kind == "dots":
        return ('<g %s fill="%s"><circle cx="5" cy="12" r="1.9"/>'
                '<circle cx="12" cy="12" r="1.9"/><circle cx="19" cy="12" r="1.9"/></g>' % (t, color))
    else:
        d = "M18 6L6 18M6 6l12 12"
    return ('<path %s d="%s" fill="none" stroke="%s" stroke-width="2.5" '
            'stroke-linecap="round" stroke-linejoin="round"/>' % (t, d, color))


def mark_svg(cx, cy, size, fill="#FFFFFF"):
    s = size / 1800.0
    return ('<g transform="translate(%s %s) scale(%s) translate(-1309 -1309)">'
            '<path fill="%s" d="%s"/></g>' % (cx, cy, s, fill, mark))


def phone(x, y, label, grown, dark):
    base = pathlib.Path("/home/user/void/design")
    bg = "#000000" if dark else "#FFFFFF"
    ink = "#FFFFFF" if dark else "#000000"
    muted = "#9A9A9A" if dark else "#5A5A5A"
    outline = "#2A2A2A" if dark else "#D8D8D8"
    surf_var = "#121216" if dark else "#F6F6F8"
    bar = "#1C1C22" if dark else "#EDEDF1"
    W, H = 420, 160
    out = ['<g><rect x="%s" y="%s" width="%s" height="%s" rx="26" fill="%s" stroke="%s"/>'
           % (x, y, W, H, bg, outline)]
    out.append('<text x="%s" y="%s" font-family="Inter,Segoe UI,sans-serif" font-size="13" '
               'font-weight="600" fill="#8a8a8a">%s</text>' % (x + 18, y - 10, label))
    iy = y + H - 18 - 56
    if not grown:
        out.append('<rect x="%s" y="%s" width="56" height="56" rx="28" fill="#C10520"/>'
                   % (x + W - 18 - 56, iy))
        out.append(mark_svg(x + W - 18 - 28, iy + 28, 21))
    else:
        ix = x + 16
        iw = W - 32
        out.append('<rect x="%s" y="%s" width="%s" height="56" rx="28" fill="%s" stroke="%s"/>'
                   % (ix, iy, iw, bar, outline))
        out.append('<text x="%s" y="%s" font-family="Inter,Segoe UI,sans-serif" font-size="15" '
                   'font-weight="500" fill="%s">Search</text>' % (ix + 22, iy + 34, muted))
        pw, ph = 176, 40
        px, py = ix + iw - 8 - pw, iy + 8
        out.append('<rect x="%s" y="%s" width="%s" height="%s" rx="20" fill="%s" stroke="%s"/>'
                   % (px, py, pw, ph, bg, outline))
        out.append(glyph(px + 10, py + 8, 24, "left", ink))
        out.append('<rect x="%s" y="%s" width="46" height="26" rx="8" fill="%s"/>'
                   % (px + 40, py + 7, surf_var))
        out.append('<text x="%s" y="%s" text-anchor="middle" font-family="Inter,Segoe UI,sans-serif" '
                   'font-size="12" font-weight="600" fill="%s">1/9</text>' % (px + 63, py + 25, ink))
        out.append(glyph(px + 92, py + 8, 24, "right", ink))
        out.append('<rect x="%s" y="%s" width="1" height="18" fill="%s"/>' % (px + 126, py + 11, outline))
        out.append('<circle cx="%s" cy="%s" r="16" fill="%s"/>' % (px + pw - 24, py + 20, surf_var))
        out.append(glyph(px + pw - 36, py + 8, 24, "dots", ink))
    out.append("</g>")
    return "\n  ".join(out)


parts = [
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 940 470" width="940" height="470">',
    '<rect width="940" height="470" fill="#0b0b0b"/>',
    '<text x="40" y="40" font-family="Inter,Segoe UI,sans-serif" font-size="19" font-weight="700" '
    'fill="#ffffff">VOID - circle to island</text>',
    '<text x="40" y="62" font-family="Inter,Segoe UI,sans-serif" font-size="13" fill="#8a8a8a">'
    'Ek blank page, bottom-right me circle. Tap se woh smoothly left tak grow hota hai.</text>',
    phone(40, 100, "REST | light - circle pinned bottom-right", False, False),
    phone(500, 100, "GROWN | light - search + pill (arrows, 1/9, dots)", True, False),
    phone(40, 310, "REST | dark", False, True),
    phone(500, 310, "GROWN | dark", True, True),
    '</svg>',
]
(BASE / "design/preview.svg").write_text("\n  ".join(parts) + "\n")
print("written", (BASE / "design/preview.svg").stat().st_size, "bytes")
