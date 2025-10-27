import qupath.lib.regions.RegionRequest
import ij.ImagePlus
import ij.process.ShortProcessor
import ij.IJ
import java.awt.image.BufferedImage

def imageData = getCurrentImageData()
def server    = imageData.getServer()

// ---- SETTINGS ----
double downsample = 5.0  // 1.0 = full-res. Increase (2, 4, …) if memory is tight.
def outDir = "Z:\\BijlsmaTeam\\DenivanSchie\\20250122_paulPDAC_rebuttal_analysis\\AFT\\images"

// Center box by FRACTION of whole image (0–1). Used if boxWidthUm/boxHeightUm are NaN.
double fracW = 0.68
double fracH = 0.68

// Alternatively, center box by FIXED SIZE in microns. Set both to real numbers to enable.
// Example: 5000 µm × 5000 µm; keep NaN to disable.
double boxWidthUm  = Double.NaN
double boxHeightUm = Double.NaN
// -------------------

new File(outDir).mkdirs()

def baseName = getProjectEntry() != null ? getProjectEntry().getImageName() : server.getShortServerName()

// Full image dimensions (level-0 pixels)
int fullW = server.getWidth()
int fullH = server.getHeight()

// Determine box size in level-0 pixels
int boxW, boxH
if (!Double.isNaN(boxWidthUm) && !Double.isNaN(boxHeightUm)) {
    def cal = server.getPixelCalibration()
    double umPerPxX = (cal != null && !Double.isNaN(cal.pixelWidthMicrons))  ? cal.pixelWidthMicrons  : server.getAveragedPixelSizeMicrons()
    double umPerPxY = (cal != null && !Double.isNaN(cal.pixelHeightMicrons)) ? cal.pixelHeightMicrons : server.getAveragedPixelSizeMicrons()
    boxW = Math.min(fullW, (int)Math.round(boxWidthUm  / umPerPxX))
    boxH = Math.min(fullH, (int)Math.round(boxHeightUm / umPerPxY))
} else {
    boxW = Math.max(1, (int)Math.round(fullW * fracW))
    boxH = Math.max(1, (int)Math.round(fullH * fracH))
}

// Top-left of centered box (level-0 pixels)
int x0 = Math.max(0, (int)Math.round((fullW - boxW) / 2.0))
int y0 = Math.max(0, (int)Math.round((fullH - boxH) / 2.0))

// Build a region request for the centered box
def request = RegionRequest.createInstance(server.getPath(), downsample, x0, y0, boxW, boxH)

// Output path (include crop info to avoid overwriting)
def outPath  = outDir + File.separator + String.format("%s_center_%dx%d_at_%d_%d_grayscale16.tif",
        baseName, boxW, boxH, x0, y0)

// Render to an RGB BufferedImage
print "Rendering centered region..."
BufferedImage rgb = server.readBufferedImage(request)

// Convert to 16-bit grayscale using luminance
print " Converting to 16-bit grayscale..."
int width  = rgb.getWidth()
int height = rgb.getHeight()
short[] pixels16 = new short[width * height]

int idx = 0
for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
        int argb = rgb.getRGB(x, y)
        int r = (argb >> 16) & 0xFF
        int g = (argb >> 8)  & 0xFF
        int b = (argb)       & 0xFF
        double yLum = 0.2126*r + 0.7152*g + 0.0722*b   // 0..255
        int v16 = (int)Math.round((yLum / 255.0) * 65535.0)
        if (v16 < 0) v16 = 0
        if (v16 > 65535) v16 = 65535
        pixels16[idx++] = (short)(v16 & 0xFFFF)
    }
}

// Wrap in an ImageJ ShortProcessor & save as TIFF
ShortProcessor sp = new ShortProcessor(width, height, pixels16, null)
ImagePlus imp = new ImagePlus(baseName + "_grayscale16_center", sp)

print " Writing: " + outPath
IJ.saveAs(imp, "Tiff", outPath)
print " Done."
