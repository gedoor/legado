// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 * ******************************************************************************
 * Copyright (C) 2005-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 * ******************************************************************************
 */
package io.legado.app.lib.icu4j;

/**
 * Abstract class for recognizing a single charset.
 * Part of the implementation of ICU's CharsetDetector.
 * <p>
 * Each specific charset that can be recognized will have an instance
 * of some subclass of this class.  All interaction between the overall
 * CharsetDetector and the stuff specific to an individual charset happens
 * via the interface provided here.
 * <p>
 * Instances of CharsetDetector DO NOT have or maintain
 * state pertaining to a specific match or detect operation.
 * The WILL be shared by multiple instances of CharsetDetector.
 * They encapsulate const charset-specific information.
 */
abstract class CharsetRecognizer {
    /**
     * Get the IANA name of this charset.
     *
     * @return the charset name.
     */
    abstract String getName();

    /**
     * Get the ISO language code for this charset.
     *
     * @return the language code, or <code>null</code> if the language cannot be determined.
     */
    public String getLanguage() {
        return null;
    }

    /**
     * Test the match of this charset with the input text data
     * which is obtained via the CharsetDetector object.
     *
     * @param det The CharsetDetector, which contains the input text
     *            to be checked for being in this charset.
     * @return A CharsetMatch object containing details of match
     * with this charset, or null if there was no match.
     */
    abstract CharsetMatch match(CharsetDetector det);

}
