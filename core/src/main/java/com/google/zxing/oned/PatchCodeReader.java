package com.google.zxing.oned;

import com.google.zxing.*;
import com.google.zxing.common.BitArray;

import java.util.Arrays;
import java.util.Map;

public class PatchCodeReader extends OneDReader {


  // These values are critical for determining how permissive the decoding
  // will be. All stripe sizes must be within the window these define, as
  // compared to the average stripe size.
  private static final float MAX_ACCEPTABLE = 2.0f;
  private static final float PADDING = 1.5f;

  /**
   * These represent the encodings patchcodes, as patterns of wide and narrow bars. The 7 least-significant bits of
   * each int correspond to the pattern of wide and narrow, with 1s representing "wide" and 0s representing narrow.
   */
  static final int[] CHARACTER_ENCODINGS = {
    0b10001 // PATCH-T
  };

  // Keep some instance variables to avoid reallocations
  private final StringBuilder decodeRowResult;
  private int[] counters;
  private int counterLength;

  public PatchCodeReader() {
    decodeRowResult = new StringBuilder(20);
    counters = new int[80];
    counterLength = 0;
  }

  @Override
  public Result decodeRow(int rowNumber, BitArray row, Map<DecodeHintType, ?> hints) throws NotFoundException, ChecksumException, FormatException {

    Arrays.fill(counters, 0); // Nettoyage du counter via settage à 0,0,...
    setCounters(row); // Initialisation du counter -> chaque entrée représente le nombre de bits identiques à la suite

    int nextStart = 1;

    while(nextStart < counterLength) {
      int charOffset = toNarrowWidePattern(nextStart);
      if(charOffset != -1) {
        // Look for whitespace before start pattern, >= 50% of width of start pattern
        // We make an exception if the whitespace is the first element.
        int patternSize = 0;
        for (int j = nextStart; j < nextStart + 7; j++) {
          patternSize += counters[j];
        }
        if (nextStart == 1 || counters[nextStart - 1] >= patternSize / 2) {
          // We found a matching pattern
          break;
        }
      }

      nextStart += 2;
    }

    // Look for whitespace after pattern:
    // Ici on mesure la taille effective du dernier charactère (somme des counters(n)
    int trailingWhitespace = counters[nextStart + 7];
    int lastPatternSize = 0;
    for (int i = -0; i < 7; i++) {
      lastPatternSize += counters[nextStart + i];
    }

    // We need to see whitespace equal to 50% of the last pattern size,
    // otherwise this is probably a false positive. The exception is if we are
    // at the end of the row. (I.e. the barcode barely fits.)
    if (nextStart < counterLength && trailingWhitespace < lastPatternSize / 2) {
      throw NotFoundException.getNotFoundInstance();
    }

    int runningCount = 0;
    for (int i = 0; i < nextStart; i++) {
      runningCount += counters[i];
    }
    float left = runningCount;
    for(int i = nextStart; i < nextStart + 7; i++) {
      runningCount += counters[i];
    }
    float right = runningCount;
    return new Result(
      decodeRowResult.toString(),
      null,
      new ResultPoint[]{
        new ResultPoint(left, rowNumber),
        new ResultPoint(right, rowNumber)},
      BarcodeFormat.PATCH_CODE);
  }

  /**
   * Records the size of all runs of white and black pixels, starting with white.
   * This is just like recordPattern, except it records all the counters, and
   * uses our builtin "counters" member for storage.
   * @param row row to count from
   */
  private void setCounters(BitArray row) throws NotFoundException {
    counterLength = 0;
    // Start from the first white bit.
    int i = row.getNextUnset(0); /** Get la position premier bit à 0 */
    int end = row.getSize(); /** Get la taille de la row */
    if (i >= end) { /** Exception si pas de bit à zero de trouvé */
      throw NotFoundException.getNotFoundInstance();
    }
    boolean isWhite = true;
    int count = 0;
    while (i < end) { /** Boucle sur la row */
      if (row.get(i) != isWhite) {
        count++;
      } else {
        counterAppend(count);
        count = 1;
        isWhite = !isWhite;
      }
      i++;
    }
    counterAppend(count);
  }

  private void counterAppend(int e) {
    counters[counterLength] = e;
    counterLength++;
    if (counterLength >= counters.length) {
      int[] temp = new int[counterLength * 2];
      System.arraycopy(counters, 0, temp, 0, counterLength);
      counters = temp;
    }
  }

  // Assumes that counters[position] is a bar.
  private int toNarrowWidePattern(int position) {
    int end = position + 7;
    if (end >= counterLength) {
      return -1;
    }

    int[] theCounters = counters;

    // Calculate threshold taking both bars and spaces
    // into account, since they must be the same width
    int max = 0;
    int min = Integer.MAX_VALUE;
    for(int j = position; j < end; j++ ) {
      int currentCounter = theCounters[j];
      if(currentCounter < min) {
        min = currentCounter;
      }
      if(currentCounter > max) {
        max = currentCounter;
      }
    }
    int threshold = (min + max) /2;

    int bitmask = 1 << 7; // 1000 0000
    int pattern = 0;
    for (int i = 0; i < 7; i++) {
      bitmask >>= 1;
      if (theCounters[position + i] > threshold) {
        pattern |= bitmask;
      }
    }

    for (int i = 0; i < CHARACTER_ENCODINGS.length; i++) {
      if (CHARACTER_ENCODINGS[i] == pattern) {
        return i;
      }
    }
    return -1;
  }
}
