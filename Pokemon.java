import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

        boolean isRepeated = false;

        for (File file : listOfFiles) {
            ImagePlus imp = IJ.openImage(file.getAbsolutePath());
            if (imp == null) continue;

            if (newImage.getWidth() != imp.getWidth() || newImage.getHeight() != imp.getHeight()) {
                continue;
            }

            ImageProcessor ip1 = newImage.getProcessor();
            ImageProcessor ip2 = imp.getProcessor();

            int width = newImage.getWidth();
            int height = newImage.getHeight();
            int differentPixels = 0;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel1 = ip1.getPixel(x, y);
                    int pixel2 = ip2.getPixel(x, y);
                    if (pixel1 != pixel2) {
                        differentPixels++;
                        if (differentPixels < threshold) {
                            isRepeated = true;
                            break;
                        }
                    }
                }
                if (isRepeated) break;
            }

            if (isRepeated) break;
        }

        if (isRepeated) {
            IJ.showMessage("Resultado", "Repeated image");
        } else {
            IJ.save(newImage, folderPath + File.separator + "new_image_" + System.currentTimeMillis() + ".png");
            IJ.showMessage("Resultado", "New image");
        }
    }
}
