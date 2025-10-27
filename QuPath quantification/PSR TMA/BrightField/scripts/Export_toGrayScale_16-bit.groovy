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
// -------------------

new File(outDir).mkdirs()

def baseName = getProjectEntry() != null ? getProjectEntry().getImageName() : server.getShortServerName()
def outPath  = outDir + File.separator + baseName + "_grayscale16.tif"

// Build a full-image request
def w = server.getWidth()
def h = server.getHeight()
def request = RegionRequest.createInstance(server.getPath(), downsample, 0, 0, w, h)

// Render to an RGB BufferedImage
print "Rendering image..."
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
ImagePlus imp = new ImagePlus(baseName + "_grayscale16", sp)

print " Writing: " + outPath
IJ.saveAs(imp, "Tiff", outPath)
print " Done."
