/*******************************************************************************
 * Copyright (c) 2012, 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial generation with CodePro tools
 *   Alexandre Montplaisir - Clean up, consolidate redundant tests
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.tests.ctfadaptor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.linuxtools.ctf.core.event.io.BitBuffer;
import org.eclipse.linuxtools.ctf.core.event.types.ArrayDeclaration;
import org.eclipse.linuxtools.ctf.core.event.types.Definition;
import org.eclipse.linuxtools.ctf.core.event.types.Encoding;
import org.eclipse.linuxtools.ctf.core.event.types.EnumDeclaration;
import org.eclipse.linuxtools.ctf.core.event.types.FloatDeclaration;
import org.eclipse.linuxtools.ctf.core.event.types.FloatDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.IntegerDeclaration;
import org.eclipse.linuxtools.ctf.core.event.types.SequenceDeclaration;
import org.eclipse.linuxtools.ctf.core.event.types.StringDeclaration;
import org.eclipse.linuxtools.ctf.core.event.types.StructDeclaration;
import org.eclipse.linuxtools.ctf.core.event.types.StructDefinition;
import org.eclipse.linuxtools.ctf.core.event.types.VariantDeclaration;
import org.eclipse.linuxtools.ctf.core.trace.CTFReaderException;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEventField;
import org.junit.Before;
import org.junit.Test;

/**
 * The class <code>CtfTmfEventFieldTest</code> contains tests for the class
 * <code>{@link CtfTmfEventField}</code>.
 *
 * @author ematkho
 * @version 1.0
 */
public class CtfTmfEventFieldTest {

    private static final String ROOT = "root";
    private static final String SEQ = "seq";
    private static final String ARRAY = "array";
    private static final String STR = "str";
    private static final String FLOAT = "float";
    private static final String LEN = "len";
    private static final String INT = "int";
    private static final String NAME = "test";
    private static final String STRUCT = "struct";
    private static final String VARIANT = "variant";
    private static final String ENUM = "enum";

    private static final byte TEST_NUMBER = 2;
    private static final String TEST_STRING = "two";

    private StructDefinition fixture;

    /**
     * Perform pre-test initialization.
     *
     * @throws UnsupportedEncodingException
     *             Thrown when UTF-8 encoding is not available.
     * @throws CTFReaderException
     *             error
     */
    @Before
    public void setUp() throws UnsupportedEncodingException, CTFReaderException {
        final byte[] testStringBytes = TEST_STRING.getBytes("UTF-8");

        int capacity = 2048;
        ByteBuffer bb = ByteBuffer.allocateDirect(capacity);

        StructDeclaration sDec = new StructDeclaration(1l);
        StringDeclaration strDec = new StringDeclaration();
        IntegerDeclaration intDec = new IntegerDeclaration(8, false, 8,
                ByteOrder.BIG_ENDIAN, Encoding.NONE, null, 8);
        FloatDeclaration flDec = new FloatDeclaration(8, 24,
                ByteOrder.BIG_ENDIAN, 8);
        ArrayDeclaration arrDec = new ArrayDeclaration(2, intDec);
        SequenceDeclaration seqDec = new SequenceDeclaration(LEN, intDec);
        StructDeclaration structDec = new StructDeclaration(8);
        EnumDeclaration enumDec = new EnumDeclaration(intDec);
        VariantDeclaration varDec = new VariantDeclaration();

        sDec.addField(INT, intDec);
        bb.put(TEST_NUMBER);

        sDec.addField(LEN, intDec);
        bb.put(TEST_NUMBER);

        sDec.addField(FLOAT, flDec);
        bb.putFloat(TEST_NUMBER);

        sDec.addField(STR, strDec);
        bb.put(testStringBytes);
        bb.put((byte) 0);

        sDec.addField(ARRAY, arrDec);
        bb.put(TEST_NUMBER);
        bb.put(TEST_NUMBER);

        sDec.addField(SEQ, seqDec);
        bb.put(TEST_NUMBER);
        bb.put(TEST_NUMBER);

        structDec.addField(STR, strDec);
        structDec.addField(INT, intDec);
        sDec.addField(STRUCT, structDec);
        bb.put(testStringBytes);
        bb.put((byte) 0);
        bb.put(TEST_NUMBER);

        enumDec.add(0, 1, LEN);
        enumDec.add(2, 3, FLOAT);
        sDec.addField(ENUM, enumDec);
        bb.put(TEST_NUMBER);

        varDec.addField(LEN, intDec);
        varDec.addField(FLOAT, flDec);
        varDec.setTag(ENUM);
        sDec.addField(VARIANT, varDec);
        bb.putFloat(TEST_NUMBER);

        fixture = sDec.createDefinition(fixture, ROOT);

        bb.position(0);
        fixture.read(new BitBuffer(bb));
    }

    /**
     * Run the CtfTmfEventField parseField(Definition,String) method test.
     */
    @Test
    public void testParseField_float() {
        FloatDefinition fieldDef = (FloatDefinition) fixture.lookupDefinition(FLOAT);
        CtfTmfEventField result = CtfTmfEventField.parseField(fieldDef, "_" + NAME);
        assertEquals("test=2.0", result.toString());
    }

    /**
     * Run the CtfTmfEventField parseField(Definition,String) method test.
     */
    @Test
    public void testParseField_array() {
        Definition fieldDef = fixture.lookupArray(ARRAY);
        CtfTmfEventField result = CtfTmfEventField.parseField(fieldDef, NAME);
        assertEquals("test=[02, 02]", result.toString());
    }

    /**
     * Run the CtfTmfEventField parseField(Definition,String) method test.
     */
    @Test
    public void testParseField_int() {
        Definition fieldDef = fixture.lookupDefinition(INT);
        CtfTmfEventField result = CtfTmfEventField.parseField(fieldDef, NAME);
        assertEquals("test=02", result.toString());
    }

    /**
     * Run the CtfTmfEventField parseField(Definition,String) method test.
     */
    @Test
    public void testParseField_sequence() {
        Definition fieldDef = fixture.lookupDefinition(SEQ);
        CtfTmfEventField result = CtfTmfEventField.parseField(fieldDef, NAME);
        assertEquals("test=[02, 02]", result.toString());
    }

    /**
     * Run the CtfTmfEventField parseField(Definition,String) method test.
     */
    @Test
    public void testParseField_sequence_value() {
        Definition fieldDef = fixture.lookupDefinition(SEQ);
        CtfTmfEventField result = CtfTmfEventField.parseField(fieldDef, NAME);
        long[] values = (long[]) result.getValue();
        long[] expected = new long[] { 2, 2 };
        assertArrayEquals(expected, values);
    }

    /**
     * Run the CtfTmfEventField parseField(Definition,String) method test.
     */
    @Test
    public void testParseField_string() {
        Definition fieldDef = fixture.lookupDefinition(STR);
        CtfTmfEventField result = CtfTmfEventField.parseField(fieldDef, NAME);
        assertEquals("test=two", result.toString());
    }

    /**
     * Run the CtfTmfEventField parseField(Definition,String) method test.
     */
    @Test
    public void testParseField_struct() {
        Definition fieldDef = fixture.lookupDefinition(STRUCT);
        CtfTmfEventField result = CtfTmfEventField.parseField(fieldDef, NAME);
        assertEquals("test=[str=two, int=02]", result.toString());
    }

    /**
     * Run the CtfTmfEventField parseField(Definition,String) method test.
     */
    @Test
    public void testParseField_enum() {
        Definition fieldDef = fixture.lookupDefinition(ENUM);
        CtfTmfEventField result = CtfTmfEventField.parseField(fieldDef, NAME);
        assertEquals("test=float", result.toString());
    }

    /**
     * Run the CtfTmfEventField parseField(Definition,String) method test.
     */
    @Test
    public void testParseField_variant() {
        Definition fieldDef = fixture.lookupDefinition(VARIANT);
        CtfTmfEventField result = CtfTmfEventField.parseField(fieldDef, NAME);
        assertEquals("test=float=2.0", result.toString());
    }
}
