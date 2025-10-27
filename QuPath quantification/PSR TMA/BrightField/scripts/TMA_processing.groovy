// ----- user settings ----------------------------------------------------
def tissuePC = "Tissue"          // saved .pxc thresholder
def psrPC    = "PS RED"          // saved .pxc trainer
// ------------------------------------------------------------------------


/* ----------- apply pixel classifiers & create annotations ---------- */
    resetSelection()                                                                                 
    createAnnotationsFromPixelClassifier(tissuePC, 0, 0)                                             
    createAnnotationsFromPixelClassifier(psrPC,    0, 0)                
    
    
/* ----------- add Optical‑Density‑Sum features ---------------------- */
    selectObjects(getAnnotationObjects().findAll {
        it.getPathClass()?.getName() == 'Picrosirius Red'
    })
    
    runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin',
              '{"pixelSizeMicrons":2.0,"region":"ROI","tileSizeMicrons":25.0,' +
              '"colorOD":true,"doMean":true,"doMedian":true,"doStdDev":true,"doMinMax":true}')