import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import java.awt.Color;

public class ImageComparator implements PlugIn {

    @Override
    public void run(String arg) {
        // Pede ao usuário para escolher a primeira imagem
        ImagePlus image1 = IJ.openImage();
        if (image1 == null) {
            IJ.showMessage("Erro", "Nenhuma imagem foi selecionada.");
            return;
        }

        // Pede ao usuário para escolher a segunda imagem
        ImagePlus image2 = IJ.openImage();
        if (image2 == null) {
            IJ.showMessage("Erro", "Nenhuma imagem foi selecionada.");
            return;
        }

        // Verifica se as imagens têm as mesmas dimensões
        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            IJ.showMessage("Erro", "As imagens devem ter as mesmas dimensões.");
            return;
        }

        int nx = image1.getWidth();
        int ny = image1.getHeight();

        // Cria uma nova imagem para armazenar a comparação
        ImagePlus comparisonImage = IJ.createImage("Comparison", "RGB", nx, ny, 1);
        ImageProcessor ip1 = image1.getProcessor();
        ImageProcessor ip2 = image2.getProcessor();
        ImageProcessor ipComparison = comparisonImage.getProcessor();

        // Compara as imagens
        for (int x = 0; x < nx; x++) {
            for (int y = 0; y < ny; y++) {
                int[] pixel1 = ip1.getPixel(x, y, null);
                int[] pixel2 = ip2.getPixel(x, y, null);

                int rDiff = Math.abs(pixel1[0] - pixel2[0]);
                int gDiff = Math.abs(pixel1[1] - pixel2[1]);
                int bDiff = Math.abs(pixel1[2] - pixel2[2]);

                Color diffColor = new Color(rDiff, gDiff, bDiff);
                ipComparison.putPixel(x, y, diffColor.getRGB());
            }
        }

        // Exibe a imagem de comparação
        comparisonImage.show();
    }
}
