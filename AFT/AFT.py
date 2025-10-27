# === Batch runner for AFT ===
# Runs over all images in a folder, saves outputs per image, and writes a CSV of order parameters.

from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from skimage import io, color

# Import your functions
from AFT_tools import image_local_order, calculate_order_parameter  # requires AFT_tools.py in the same working dir

# ----------------- USER SETTINGS -----------------
INPUT_DIR  = r"/images/test/"      # folder with input images
OUTPUT_DIR = r"output/"         # where overlays/heatmaps + CSV will be written

# AFT parameters (tweak as desired)
WINDOW_SIZE          = 33      # odd; local window size (pixels)
OVERLAP              = 0.5     # 0–1; step as a fraction of window size
NEIGHBORHOOD_RADIUS  = 2       # in 'vector grid' units (NOT pixels)
INTENSITY_THRESH     = 0       # mean-intensity threshold per window
ECCENTRICITY_THRESH  = 0       # 0–1; filter weak orientation windows

# What to save for each image
SAVE_OVERLAY         = True    # quiver over raw image
SAVE_ANGLE_MAP       = True    # heatmap (degrees) of local orientation
SAVE_ECC_MAP         = False   # heatmap of local eccentricity
# -------------------------------------------------

Path(OUTPUT_DIR).mkdir(parents=True, exist_ok=True)

# Allowed image extensions (feel free to extend)
ALLOWED_EXTS = {".tif", ".tiff", ".png", ".jpg", ".jpeg", ".bmp"}

results = []

img_paths = sorted(p for p in Path(INPUT_DIR).glob("**/*") if p.suffix.lower() in ALLOWED_EXTS)
if not img_paths:
    raise FileNotFoundError(f"No images with {sorted(ALLOWED_EXTS)} found under {INPUT_DIR}")

for i, img_path in enumerate(img_paths, start=1):
    base = img_path.stem
    print(f"[{i}/{len(img_paths)}] Processing: {img_path.name}")

    # --- Load + ensure grayscale float32 ---
    im = io.imread(str(img_path))
    if im.ndim == 3:   # RGB/RGBA -> grayscale
        im = color.rgb2gray(im)
    im = im.astype("float32")

    # --- Compute local orientation (theta grid) & eccentricity ---
    # We let the function keep plotting off; we'll do our own plots with the right basenames.
    x, y, u, v, theta, ecc = image_local_order(
        im,
        window_size=WINDOW_SIZE,
        overlap=OVERLAP,
        im_mask=None,
        intensity_thresh=INTENSITY_THRESH,
        eccentricity_thresh=ECCENTRICITY_THRESH,
        plot_overlay=False,
        plot_angles=False,
        plot_eccentricity=False,
        save_figures=False,
        save_path=""
    )

    # --- Order parameter (single scalar per image) ---
    order_param = float(
        calculate_order_parameter(theta, neighborhood_radius=NEIGHBORHOOD_RADIUS)
    )

    # --- Save figures with the *right name* (based on input basename) ---
    # Overlay (quiver over raw image)
    if SAVE_OVERLAY:
        plt.figure()
        plt.imshow(im, cmap="gray")
        # Note: x, y, u, v are the vector grid positions & components returned by image_local_order
        plt.quiver(
            x, y, u, v,
            pivot="mid",
            scale_units="xy",
            scale=OVERLAP/2,   # matches helper's convention
            headaxislength=0,
            headlength=0,
            width=0.005
        )
        plt.title(f"{base} — orientation overlay")
        plt.axis("off")
        out_path = Path(OUTPUT_DIR, f"{base}_overlay.png")
        plt.savefig(out_path, dpi=300, bbox_inches="tight")
        plt.close()

    # Angle map (in degrees, -90..90); theta is a 2D grid over windows (NOT pixels)
    if SAVE_ANGLE_MAP:
        plt.figure()
        plt.imshow(theta * 180 / np.pi, vmin=-90, vmax=90, cmap="hsv")
        plt.colorbar(label="Angle (°)")
        plt.title(f"{base} — local orientation (window grid)")
        out_path = Path(OUTPUT_DIR, f"{base}_angle.png")
        plt.savefig(out_path, dpi=300, bbox_inches="tight")
        plt.close()

    # Eccentricity map (0..1) on the same window grid
    if SAVE_ECC_MAP:
        plt.figure()
        plt.imshow(ecc, vmin=0, vmax=1)
        plt.colorbar(label="Eccentricity")
        plt.title(f"{base} — local eccentricity (window grid)")
        out_path = Path(OUTPUT_DIR, f"{base}_ecc.png")
        plt.savefig(out_path, dpi=300, bbox_inches="tight")
        plt.close()

    # Record result row
    results.append({
        "filename": img_path.name,
        "path": str(img_path),
        "order_parameter": order_param,
        "window_size": WINDOW_SIZE,
        "overlap": OVERLAP,
        "neighborhood_radius": NEIGHBORHOOD_RADIUS,
        "intensity_thresh": INTENSITY_THRESH,
        "eccentricity_thresh": ECCENTRICITY_THRESH,
        "theta_rows": int(theta.shape[0]),
        "theta_cols": int(theta.shape[1]),
        "img_height": int(im.shape[0]),
        "img_width": int(im.shape[1]),
    })

# --- Write CSV with one row per image ---
df = pd.DataFrame(results)
csv_path = Path(OUTPUT_DIR, "order_parameters.csv")
df.to_csv(csv_path, index=False)
print(f"\nSaved CSV: {csv_path}")
print("Done.")
