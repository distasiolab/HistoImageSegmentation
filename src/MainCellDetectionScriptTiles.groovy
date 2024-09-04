import qupath.lib.gui.QuPathGUI
import qupath.lib.objects.PathAnnotationObject
import qupath.lib.objects.classes.PathClassFactory
import qupath.lib.roi.RectangleROI
import qupath.lib.roi.PolygonROI
import qupath.lib.objects.PathObjects
import qupath.lib.roi.ROIs
import qupath.lib.roi.RoiTools
import qupath.lib.regions.ImagePlane


// Get main data structures
def imageData = getCurrentImageData()

// Get the current viewer & the location of the pixel currently in the center
def viewer = getCurrentViewer()
double cx = viewer.getCenterPixelX()
double cy = viewer.getCenterPixelY()
def plane = getCurrentViewer().getImagePlane()
def server = imageData.getServer()
def cal = server.getPixelCalibration()
printf("cal.getAveragedPixelSizeMicrons:" + cal.getAveragedPixelSizeMicrons())

// Clear any existing annotations
toRemove = getAnnotationObjects().findAll()
removeObjects(toRemove, true)
toRemove = getDetectionObjects().findAll()
removeObjects(toRemove, true)
fireHierarchyUpdate()
resetSelection()


// Create an annotation for the whole tissue region
createAnnotationsFromPixelClassifier("Tissue_Detection", 0.0, 0.0, "SELECT_NEW")

def tissue_area = imageData.getHierarchy().getAnnotationObjects()[0]
tissue_roi = tissue_area.getROI()


// Define the size of the tiles to create
double sizeMicrons = 1000.0
int sizePixels = Math.round(sizeMicrons / cal.getAveragedPixelSizeMicrons())


for (int i = 0; i < Math.round(server.getWidth()/ sizePixels); i++) {
    for (int j = 0; j < Math.round(server.getHeight()/ sizePixels); j++) {
        // Create a new Rectangle ROI
        int x0 = i*sizePixels
        int y0 = j*sizePixels
        def roi = new RectangleROI(x0, y0, sizePixels, sizePixels)
        
        intersection = RoiTools.intersection([tissue_roi, roi])
        if (intersection.getArea() > Math.pow(sizePixels,2)/2) {
           // Create & new annotation & add it to the object hierarchy
            def annotation = new PathAnnotationObject(roi, PathClassFactory.getPathClass("Region"))
            imageData.getHierarchy().addPathObject(annotation, true)
        
        }

    }
}






runPlugin('qupath.imagej.detect.cells.PositiveCellDetection', '{"detectionImageBrightfield": "Optical density sum",  "requestedPixelSizeMicrons": 0.5,  "backgroundRadiusMicrons": 8.0,  "medianRadiusMicrons": 0.0,  "sigmaMicrons": 1.5,  "minAreaMicrons": 10.0,  "maxAreaMicrons": 400.0,  "threshold": 0.1,  "maxBackground": 2.0,  "watershedPostProcess": true,  "excludeDAB": false,  "cellExpansionMicrons": 5.0,  "includeNuclei": true,  "smoothBoundaries": true,  "makeMeasurements": true,  "thresholdCompartment": "Nucleus: DAB OD mean",  "thresholdPositive1": 0.2,  "thresholdPositive2": 0.4,  "thresholdPositive3": 0.6000000000000001,  "singleThreshold": true}');

fireHierarchyUpdate()
resetSelection()



// Export Results

//Use either "project" OR "outputFolder" to determine where your detection files will go
def project = QuPathGUI.getInstance().getProject().getBaseDirectory()
project = project.toString()+"/output/"

//Make sure the output folder exists
mkdirs(project)

//def server = getCurrentServer()
def String currentImageName =  server.getMetadata()["name"].toString()
String outfile = String.format("%s%s_measurements.tsv", project, currentImageName)

saveAnnotationMeasurements(outfile)

print("Saved Measurements to: " + outfile)
print("Done!")


