"""Enlarge the MotorPH logo 4x faithfully, without a white halo.

1. Background = near-white connected to the image border (flood fill); whites inside the logo (wheel, chrome) stay.
2. Edge band (4 px next to the background): each pixel is the logo colour mixed with white. Its true colour is taken
   from the nearest interior pixels, and its opacity is solved from  pixel = alpha*colour + (1-alpha)*white.
3. Lanczos 4x, crisp silhouette (steep curve on the smoothed alpha), light unsharp mask on the colour.
"""
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

def spread(color, mask):
    """Fill pixels outside mask with the average of nearby mask pixels only (normalized blur, growing radius)."""
    color = color.copy()
    have = mask.astype(np.float32)
    for radius in (1, 2, 4, 8, 16, 32):
        den = np.asarray(Image.fromarray((have * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius)), dtype=np.float32) / 255.0
        est = np.zeros_like(color)
        for ch in range(3):
            layer = Image.fromarray((color[:, :, ch] * have * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius))
            est[:, :, ch] = np.asarray(layer, dtype=np.float32) / 255.0
        ok = (den > 0.02) & (have < 0.5)
        color[ok] = est[ok] / den[ok, None]
        have = np.maximum(have, ok.astype(np.float32))
    return np.clip(color, 0, 1)


src, out, scale = sys.argv[1], sys.argv[2], int(sys.argv[3]) if len(sys.argv) > 3 else 4
img = np.asarray(Image.open(src).convert('RGB'), dtype=np.float32) / 255.0
h, w = img.shape[:2]

whiteish = Image.fromarray(((img.min(axis=2) > 0.94) * 255).astype(np.uint8))
fill = whiteish.copy()
for x in range(w):
    for y in (0, h - 1):
        if fill.getpixel((x, y)) == 255:
            ImageDraw.floodfill(fill, (x, y), 128)
for y in range(h):
    for x in (0, w - 1):
        if fill.getpixel((x, y)) == 255:
            ImageDraw.floodfill(fill, (x, y), 128)
background = np.asarray(fill) == 128

bg_img = Image.fromarray((background * 255).astype(np.uint8))
band = (np.asarray(bg_img.filter(ImageFilter.MaxFilter(9))) > 0) & ~background
interior = ~background & ~band

# True colour of edge pixels: the average of nearby interior pixels only.
fg = spread(img, interior)

# Opacity by least squares on (1 - pixel) = alpha * (1 - colour), per pixel over the three channels.
num = ((1 - img) * (1 - fg)).sum(axis=2)
den = np.maximum(((1 - fg) ** 2).sum(axis=2), 0.02)
alpha = np.clip(num / den, 0, 1)
alpha[interior] = 1
alpha[background] = 0
color = np.where(band[:, :, None], fg, img)

# Spread colour past the edge so resizing never averages in white.
filled = Image.fromarray((spread(color, alpha > 0.05) * 255).astype(np.uint8))

size = (w * scale, h * scale)
c_big = filled.resize(size, Image.LANCZOS).filter(ImageFilter.UnsharpMask(radius=scale * 1.2, percent=80, threshold=2))
a_big = Image.fromarray((alpha * 255).astype(np.uint8)).resize(size, Image.LANCZOS).filter(ImageFilter.GaussianBlur(scale * 0.6))
a = np.asarray(a_big, dtype=np.float32) / 255.0
a = np.clip((a - 0.4) / 0.25, 0, 1)
a = a * a * (3 - 2 * a)

result = c_big.convert('RGBA')
result.putalpha(Image.fromarray((a * 255).astype(np.uint8)))
bbox = result.getchannel('A').point(lambda v: 255 if v > 8 else 0).getbbox()
result = result.crop(bbox)
result.save(out, optimize=True)
print('saved', out, result.size)
