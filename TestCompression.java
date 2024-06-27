import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Pokemon implements PlugIn {

    @Override
    public void run(String arg) {
        // Pede ao usuário para escolher uma pasta
        String folderPath = IJ.getDirectory("Escolha a pasta com as imagens:");
        if (folderPath == null) {
            IJ.showMessage("Erro", "Nenhuma pasta foi selecionada.");
            return;
        }

        // Pede ao usuário para escolher a nova imagem
        ImagePlus newImage = IJ.openImage();
        if (newImage == null) {
            IJ.showMessage("Erro", "Nenhuma imagem foi selecionada.");
            return;
        }

        // Pede ao usuário para definir o limiar
        int threshold = (int) IJ.getNumber("Digite o limiar de diferença de pixels:", 100);

        // Carrega todas as imagens da pasta
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
        if (listOfFiles == null) {
            IJ.showMessage("Erro", "Nenhuma imagem encontrada na pasta.");
            return;
        }

        // Measure time for comparison with compression
        long startTimeCompressed = System.nanoTime();
        boolean isRepeatedCompressed = compareWithCompression(newImage, listOfFiles, threshold);
        long endTimeCompressed = System.nanoTime();

        // Measure time for comparison without compression
        long startTimeUncompressed = System.nanoTime();
        boolean isRepeatedUncompressed = compareWithoutCompression(newImage, listOfFiles, threshold);
        long endTimeUncompressed = System.nanoTime();

        // Output the results
        if (isRepeatedCompressed) {
            IJ.showMessage("Resultado", "Repeated image (Compressed)");
        } else {
            IJ.save(newImage, folderPath + File.separator + "new_image_" + System.currentTimeMillis() + ".png");
            IJ.showMessage("Resultado", "New image (Compressed)");
        }

        if (isRepeatedUncompressed) {
            IJ.showMessage("Resultado", "Repeated image (Uncompressed)");
        } else {
            IJ.save(newImage, folderPath + File.separator + "new_image_" + System.currentTimeMillis() + "_uncompressed.png");
            IJ.showMessage("Resultado", "New image (Uncompressed)");
        }

        // Display timing results
        IJ.showMessage("Timing Results",
                "Comparison with compression: " + (endTimeCompressed - startTimeCompressed) / 1_000_000 + " ms\n" +
                "Comparison without compression: " + (endTimeUncompressed - startTimeUncompressed) / 1_000_000 + " ms");
    }

    private boolean compareWithCompression(ImagePlus newImage, File[] listOfFiles, int threshold) {
        // Compress the new image using RLE
        List<int[]> compressedNewImage = compressImage(newImage);
        boolean isRepeated = false;

        // Create a thread pool
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<ComparisonTask> tasks = new ArrayList<>();

        for (File file : listOfFiles) {
            tasks.add(new ComparisonTask(file, compressedNewImage, threshold, newImage.getWidth(), newImage.getHeight()));
        }

        try {
            isRepeated = executor.invokeAny(tasks);
        } catch (Exception e) {
            IJ.showMessage("Erro", "Erro ao comparar imagens: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        return isRepeated;
    }

    private boolean compareWithoutCompression(ImagePlus newImage, File[] listOfFiles, int threshold) {
        ImageProcessor newIp = newImage.getProcessor();
        int newWidth = newImage.getWidth();
        int newHeight = newImage.getHeight();
        boolean isRepeated = false;

        for (File file : listOfFiles) {
            ImagePlus imp = IJ.openImage(file.getAbsolutePath());
            if (imp == null) continue;

            if (newWidth != imp.getWidth() || newHeight != imp.getHeight()) {
                continue;
            }

            ImageProcessor ip = imp.getProcessor();
            int differentPixels = 0;

            outerLoop:
            for (int y = 0; y < newHeight; y++) {
                for (int x = 0; x < newWidth; x++) {
                    if (newIp.getPixel(x, y) != ip.getPixel(x, y)) {
                        differentPixels++;
                        if (differentPixels >= threshold) {
                            break outerLoop;
                        }
                    }
                }
            }

            if (differentPixels < threshold) {
                isRepeated = true;
                break;
            }
        }

        return isRepeated;
    }

    private List<int[]> compressImage(ImagePlus image) {
        ImageProcessor ip = image.getProcessor();
        List<int[]> compressed = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            int currentColor = ip.getPixel(0, y);
            int count = 1;
            for (int x = 1; x < width; x++) {
                int pixel = ip.getPixel(x, y);
                if (pixel == currentColor) {
                    count++;
                } else {
                    compressed.add(new int[]{count, currentColor});
                    currentColor = pixel;
                    count = 1;
                }
            }
            compressed.add(new int[]{count, currentColor});  // Add the last sequence
        }
        return compressed;
    }

    private class ComparisonTask implements Callable<Boolean> {
        private final File file;
        private final List<int[]> compressedNewImage;
        private final int threshold;
        private final int newImageWidth;
        private final int newImageHeight;

        public ComparisonTask(File file, List<int[]> compressedNewImage, int threshold, int newImageWidth, int newImageHeight) {
            this.file = file;
            this.compressedNewImage = compressedNewImage;
            this.threshold = threshold;
            this.newImageWidth = newImageWidth;
            this.newImageHeight = newImageHeight;
        }

        @Override
        public Boolean call() {
            try {
                File compressedFile = new File(file.getAbsolutePath() + "_compressed");
                List<int[]> compressedImage;

                if (compressedFile.exists()) {
                    // Load the compressed image from file
                    compressedImage = loadCompressedImage(compressedFile);
                } else {
                    // Compress the current image in the folder using RLE
                    ImagePlus imp = IJ.openImage(file.getAbsolutePath());
                    if (imp == null) return false;

                    if (newImageWidth != imp.getWidth() || newImageHeight != imp.getHeight()) {
                        return false;
                    }

                    compressedImage = compressImage(imp);

                    // Save the compressed image to file
                    saveCompressedImage(compressedImage, compressedFile);
                }

                return compareCompressedImages(compressedNewImage, compressedImage, threshold);
            } catch (Exception e) {
                IJ.showMessage("Erro", "Erro ao processar a imagem: " + e.getMessage());
                return false;
            }
        }
    }

    private boolean compareCompressedImages(List<int[]> img1, List<int[]> img2, int threshold) {
        int differentPixels = 0;
        int minLength = Math.min(img1.size(), img2.size());
        for (int i = 0; i < minLength; i++) {
            int[] run1 = img1.get(i);
            int[] run2 = img2.get(i);
            if (run1[1] != run2[1]) {  // Compare colors
                differentPixels += Math.min(run1[0], run2[0]);
                if (differentPixels >= threshold) {
                    return false;
                }
            }
        }
        return true;
    }

    private void saveCompressedImage(List<int[]> compressedImage, File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(compressedImage);
        } catch (IOException e) {
            IJ.showMessage("Erro", "Não foi possível salvar a imagem compactada: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<int[]> loadCompressedImage(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<int[]>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            IJ.showMessage("Erro", "Não foi possível carregar a imagem compactada: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
