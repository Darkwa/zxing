package patchTTest;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.oned.PatchCodeReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class testPatchT {
  public static void main(String[] args) {

    Reader reader = new PatchCodeReader();

    List<File> files = new ArrayList<>();
    for(int i = 1; i < 16; i++) {
      String path = "D:\\numerisationserver_temp\\scan\\20180615173729_00" + String.format("%02d", i) + ".png";
      System.out.println(path);
      files.add(new File(path));
    }

    List<Boolean> result = new ArrayList<>();

    for(File file : files) {

      try {

        result.add(isPatch(file));

      } catch (IOException e) {
        e.printStackTrace();
      } catch (FormatException e) {
        e.printStackTrace();
      } catch (ChecksumException e) {
        e.printStackTrace();
      }

    }

    for(Boolean b : result) {
      System.out.println("isPatch : " + b);
    }

  }


  public static boolean isPatch(File file) throws IOException, FormatException, ChecksumException {

    BufferedImage image = ImageIO.read(file);
    //Date now = Date.
    List<BufferedImage> subImages = new ArrayList<>();
    subImages.add(image.getSubimage(0,0,image.getWidth()/2,image.getHeight()));
    subImages.add(image.getSubimage(image.getWidth()/2,0, image.getWidth()/2, image.getHeight()));
    subImages.add(image.getSubimage(0, 0, image.getWidth(), image.getHeight()/2));
    subImages.add(image.getSubimage(0, image.getHeight()/2, image.getWidth(), image.getHeight()/2));

    Reader reader = new PatchCodeReader();

    Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);
    HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

    for(BufferedImage subImage : subImages) {

      LuminanceSource source = new BufferedImageLuminanceSource(subImage);
      BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));

      try {
        reader.decode(bitmap);
      } catch (NotFoundException e) {
        return false;
      }
    }
    return true;
  }
}
