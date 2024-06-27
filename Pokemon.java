import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.gui.GenericDialog;

import java.io.File;

public class Pokemon implements PlugIn {
    private static final double THRESHOLD = 10.0; // Example threshold for the number of differing pixels

    @Override
    public void run(String arg) {
        // Prompt the user to select the image to compare
        ImagePlus newImage = IJ.openImage();
        if (newImage == null) {
            IJ.showMessage("Error", "No image selected.");
            return;
        }

        // Prompt the user to select the directory of processed images
        GenericDialog gd = new GenericDialog("Select Directory");
        gd.addDirectoryField("Processed images directory", "");
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        String processedImagesDir = gd.getNextString();

        // Check if the directory is valid
        File dir = new File(processedImagesDir);
        if (!dir.exists() || !dir.isDirectory()) {
            IJ.showMessage("Error", "Invalid directory: " + processedImagesDir);
            return;
        }

        // Check if the image is a duplicate
        boolean isDuplicate = isDuplicate(newImage, processedImagesDir);
        if (isDuplicate) {
            IJ.showMessage("Result", "The image is a duplicate.");
        } else {
            IJ.showMessage("Result", "The image is new.");
        }
    }

    public boolean isDuplicate(ImagePlus newImage, String processedImagesDir) {
        File dir = new File(processedImagesDir);
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null) {
            IJ.showMessage("Error", "Could not list files in the directory: " + processedImagesDir);
            return false;
        }

        for (File file : files) {
            ImagePlus processedImage = IJ.openImage(file.getAbsolutePath());
            if (processedImage == null) {
                IJ.showMessage("Error", "Could not open the image: " + file.getAbsolutePath());
                continue;
            }

            if (compareImages(newImage, processedImage) < THRESHOLD) {
                return true; // Found a duplicate
            }
        }

        return false; // No duplicates found
    }

    private double compareImages(ImagePlus img1, ImagePlus img2) {
        ImageProcessor ip1 = img1.getProcessor();
        ImageProcessor ip2 = img2.getProcessor();

        if (ip1.getWidth() != ip2.getWidth() || ip1.getHeight() != ip2.getHeight()) {
            return Double.MAX_VALUE; // Images are different sizes, treat as entirely different
        }

        int width = ip1.getWidth();
        int height = ip1.getHeight();
        double differingPixels = 0.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int[] pixel1 = ip1.getPixel(x, y, null);
                int[] pixel2 = ip2.getPixel(x, y, null);
                if (!pixelsEqual(pixel1, pixel2)) {
                    differingPixels++;
                }
            }
        }

        return differingPixels;
    }

    private boolean pixelsEqual(int[] pixel1, int[] pixel2) {
        return pixel1[0] == pixel2[0] && pixel1[1] == pixel2[1] && pixel1[2] == pixel2[2];
    }
}
