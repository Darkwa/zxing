package patchTTest;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class testPatchT {
  public static void main(String[] args) {

    Reader reader = new PatchCodeReader();

    File file = new File("/home/pierre/Documents/Patch_T_US-Letter.tif");
    BufferedImage image = null;
    Result result = null;
    try {
      image = ImageIO.read(file);
      LuminanceSource source = new BufferedImageLuminanceSource(image);
      BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
      Collection<Result> results = new ArrayList<>(1);

      result = reader.decode(bitmap);

    } catch (IOException e) {
      e.printStackTrace();
    } catch (FormatException e) {
      e.printStackTrace();
    } catch (ChecksumException e) {
      e.printStackTrace();
    } catch (NotFoundException e) {
      e.printStackTrace();
    }


  }
}
