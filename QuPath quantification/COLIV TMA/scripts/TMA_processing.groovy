/* ----------- apply pixel classifiers & create annotations ---------- */
    resetSelection()                                                                                 
    createAnnotationsFromPixelClassifier("tissue_classifier", 0.0, 0.0)
    
/* ----------- add Optical‑Density‑Sum features ---------------------- */
    selectObjects(getAnnotationObjects().findAll {
        it.getPathClass()?.getName() == 'Tissue'
    })
    
    runPlugin('qupath.lib.algorithms.IntensityFeaturesPlugin', '{"pixelSizeMicrons":2.0,"region":"ROI","tileSizeMicrons":25.0,"colorOD":false,"colorStain1":false,"colorStain2":true,"colorStain3":false,"colorRed":false,"colorGreen":false,"colorBlue":false,"colorHue":false,"colorSaturation":false,"colorBrightness":false,"doMean":true,"doStdDev":true,"doMinMax":true,"doMedian":true,"doHaralick":false,"haralickDistance":1,"haralickBins":32}')