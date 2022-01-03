// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2013, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package io.legado.app.lib.icu4j;

/**
 * This class matches UTF-16 and UTF-32, both big- and little-endian. The
 * BOM will be used if it is present.
 */
abstract class CharsetRecog_Unicode extends CharsetRecognizer {

    /* (non-Javadoc)
     * @see com.ibm.icu.text.CharsetRecognizer#getName()
     */
    @Override
    abstract String getName();

    /* (non-Javadoc)
     * @see com.ibm.icu.text.CharsetRecognizer#match(com.ibm.icu.text.CharsetDetector)
     */
    @Override
    abstract CharsetMatch match(CharsetDetector det);

    static int codeUnit16FromBytes(byte hi, byte lo) {
        return ((hi & 0xff) << 8) | (lo & 0xff);
    }

    // UTF-16 confidence calculation. Very simple minded, but better than nothing.
    //   Any 8 bit non-control characters bump the confidence up. These have a zero high byte,
    //     and are very likely to be UTF-16, although they could also be part of a UTF-32 code.
    //   NULs are a contra-indication, they will appear commonly if the actual encoding is UTF-32.
    //   NULs should be rare in actual text.
    static int adjustConfidence(int codeUnit, int confidence) {
        if (codeUnit == 0) {
            confidence -= 10;
        } else if ((codeUnit >= 0x20 && codeUnit <= 0xff) || codeUnit == 0x0a) {
            confidence += 10;
        }
        if (confidence < 0) {
            confidence = 0;
        } else if (confidence > 100) {
            confidence = 100;
        }
        return confidence;
    }

    static class CharsetRecog_UTF_16_BE extends CharsetRecog_Unicode {
        @Override
        String getName() {
            return "UTF-16BE";
        }

        @Override
        CharsetMatch match(CharsetDetector det) {
            byte[] input = det.fRawInput;
            int confidence = 10;

            int bytesToCheck = Math.min(input.length, 30);
            for (int charIndex = 0; charIndex < bytesToCheck - 1; charIndex += 2) {
                int codeUnit = codeUnit16FromBytes(input[charIndex], input[charIndex + 1]);
                if (charIndex == 0 && codeUnit == 0xFEFF) {
                    confidence = 100;
                    break;
                }
                confidence = adjustConfidence(codeUnit, confidence);
                if (confidence == 0 || confidence == 100) {
                    break;
                }
            }
            if (bytesToCheck < 4 && confidence < 100) {
                confidence = 0;
            }
            if (confidence > 0) {
                return new CharsetMatch(det, this, confidence);
            }
            return null;
        }
    }

    static class CharsetRecog_UTF_16_LE extends CharsetRecog_Unicode {
        @Override
        String getName() {
            return "UTF-16LE";
        }

        @Override
        CharsetMatch match(CharsetDetector det) {
            byte[] input = det.fRawInput;
            int confidence = 10;

            int bytesToCheck = Math.min(input.length, 30);
            for (int charIndex = 0; charIndex < bytesToCheck - 1; charIndex += 2) {
                int codeUnit = codeUnit16FromBytes(input[charIndex + 1], input[charIndex]);
                if (charIndex == 0 && codeUnit == 0xFEFF) {
                    confidence = 100;
                    break;
                }
                confidence = adjustConfidence(codeUnit, confidence);
                if (confidence == 0 || confidence == 100) {
                    break;
                }
            }
            if (bytesToCheck < 4 && confidence < 100) {
                confidence = 0;
            }
            if (confidence > 0) {
                return new CharsetMatch(det, this, confidence);
            }
            return null;
        }
    }

    static abstract class CharsetRecog_UTF_32 extends CharsetRecog_Unicode {
        abstract int getChar(byte[] input, int index);

        @Override
        abstract String getName();

        @Override
        CharsetMatch match(CharsetDetector det) {
            byte[] input = det.fRawInput;
            int limit = (det.fRawLength / 4) * 4;
            int numValid = 0;
            int numInvalid = 0;
            boolean hasBOM = false;
            int confidence = 0;

            if (limit == 0) {
                return null;
            }
            if (getChar(input, 0) == 0x0000FEFF) {
                hasBOM = true;
            }

            for (int i = 0; i < limit; i += 4) {
                int ch = getChar(input, i);

                if (ch < 0 || ch >= 0x10FFFF || (ch >= 0xD800 && ch <= 0xDFFF)) {
                    numInvalid += 1;
                } else {
                    numValid += 1;
                }
            }


            // Cook up some sort of confidence score, based on presence of a BOM
            //    and the existence of valid and/or invalid multi-byte sequences.
            if (hasBOM && numInvalid == 0) {
                confidence = 100;
            } else if (hasBOM && numValid > numInvalid * 10) {
                confidence = 80;
            } else if (numValid > 3 && numInvalid == 0) {
                confidence = 100;
            } else if (numValid > 0 && numInvalid == 0) {
                confidence = 80;
            } else if (numValid > numInvalid * 10) {
                // Probably corrupt UTF-32BE data.  Valid sequences aren't likely by chance.
                confidence = 25;
            }

            return confidence == 0 ? null : new CharsetMatch(det, this, confidence);
        }
    }

    static class CharsetRecog_UTF_32_BE extends CharsetRecog_UTF_32 {
        @Override
        int getChar(byte[] input, int index) {
            return (input[index + 0] & 0xFF) << 24 | (input[index + 1] & 0xFF) << 16 |
                    (input[index + 2] & 0xFF) << 8 | (input[index + 3] & 0xFF);
        }

        @Override
        String getName() {
            return "UTF-32BE";
        }
    }


    static class CharsetRecog_UTF_32_LE extends CharsetRecog_UTF_32 {
        @Override
        int getChar(byte[] input, int index) {
            return (input[index + 3] & 0xFF) << 24 | (input[index + 2] & 0xFF) << 16 |
                    (input[index + 1] & 0xFF) << 8 | (input[index + 0] & 0xFF);
        }

        @Override
        String getName() {
            return "UTF-32LE";
        }
    }
}
