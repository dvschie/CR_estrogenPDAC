// QuPath ≥ 0.4  – ‘Include default imports’ must be ON
import qupath.lib.regions.RegionRequest
import qupath.lib.gui.scripting.QPEx
import qupath.lib.images.ImageData.ImageType

double ds = 1.0d
def outDir = createDirectory(PROJECT_BASE_DIR, 'core_extracts')
def cores  = getTMACoreList()                       // all TMACoreObjects  :contentReference[oaicite:3]{index=3}
println "Found ${cores.size()} TMA cores."

for (core in cores) {
    def safe = core.getName() ?: UUID.randomUUID()
    println "\n• Exporting $safe"
    resetSelection()                                // clears blue outline  :contentReference[oaicite:4]{index=4}

    // Describe the region
    def req = RegionRequest.createInstance(
                 getCurrentServer().getPath(), ds, core.getROI())         // request overload  :contentReference[oaicite:5]{index=5}

    // Write *original* pixels (no overlays) straight from the server
    def out = buildFilePath(outDir, safe + '.tif')
    writeImageRegion(getCurrentServer(), req, out)                        // fast writer  :contentReference[oaicite:6]{index=6}
}
println "\n✓  Core export finished – files in $outDir"
