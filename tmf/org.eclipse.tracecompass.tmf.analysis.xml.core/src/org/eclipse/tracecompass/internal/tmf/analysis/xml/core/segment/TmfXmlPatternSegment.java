/*******************************************************************************
 * Copyright (c) 2016 Ecole Polytechnique de Montreal, Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.analysis.xml.core.segment;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.datastore.core.interval.IHTIntervalReader;
import org.eclipse.tracecompass.datastore.core.serialization.ISafeByteBufferWriter;
import org.eclipse.tracecompass.datastore.core.serialization.SafeByteBufferFactory;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.Activator;
import org.eclipse.tracecompass.segmentstore.core.IContentSegment;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.segment.interfaces.INamedSegment;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.statesystem.core.statevalue.TmfStateValue;

/**
 * This class implements an XML Pattern Segment. This type of segment has
 * content and a default timestamp, which is the start time of the segment.
 *
 * @author Jean-Christian Kouame
 */
public class TmfXmlPatternSegment implements INamedSegment, IContentSegment {

    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 3556323761465412078L;

    /* 'Byte' equivalent for state values types */
    private static final byte TYPE_NULL = -1;
    private static final byte TYPE_INTEGER = 0;
    private static final byte TYPE_STRING = 1;
    private static final byte TYPE_LONG = 2;

    private final long fStart;
    private final long fEnd;
    private final @NonNull String fSegmentName;
    private transient @NonNull Map<@NonNull String, @NonNull ITmfStateValue> fContent;

    /**
     * The reader for this segment class
     */
    public static final @NonNull IHTIntervalReader<@NonNull ISegment> READER = buffer -> {

        long start = buffer.getLong();
        long end = buffer.getLong();
        // Reading the now unused scale
        buffer.getInt();
        String segmentName = buffer.getString();
        int contentSize = buffer.getInt();

        final Map<@NonNull String, @NonNull ITmfStateValue> content = new HashMap<>();
        for (int i = 0; i < contentSize; i++) {
            String name = buffer.getString().intern();

            Byte type = buffer.get();
            ITmfStateValue value;
            switch (type) {
            case TYPE_NULL:
                value = TmfStateValue.nullValue();
                break;
            case TYPE_INTEGER:
                value = TmfStateValue.newValueInt(buffer.getInt());
                break;
            case TYPE_LONG:
                value = TmfStateValue.newValueLong(buffer.getLong());
                break;
            case TYPE_STRING:
                value = TmfStateValue.newValueString(buffer.getString().intern());
                break;
            default:
                value = TmfStateValue.nullValue();
                Activator.logError("Read segment failed : Invalid data, value will be set to null"); //$NON-NLS-1$
            }
            content.put(name, value);
        }
        return new TmfXmlPatternSegment(start, end, segmentName, content);
    };

    /**
     * Constructs an XML pattern segment
     *
     * @param start
     *            Start time of the pattern segment
     * @param end
     *            End time of the pattern segment
     * @param scale
     *            Scale of the pattern segment
     * @param segmentName
     *            Name of the pattern segment
     * @param fields
     *            Fields of the pattern segment
     */
    public TmfXmlPatternSegment(long start, long end, String segmentName, @NonNull Map<@NonNull String, @NonNull ITmfStateValue> fields) {
        fStart = start;
        fEnd = end;
        fSegmentName = String.valueOf(segmentName);
        fContent = Collections.unmodifiableMap(fields);
    }

    /**
     * Get the content of the pattern segment
     *
     * @return The content
     */
    @Override
    public Map<@NonNull String, @NonNull ITmfStateValue> getContent() {
        return fContent;
    }

    @Override
    public String getName() {
        return fSegmentName;
    }

    @Override
    public int compareTo(@NonNull ISegment o) {
        int ret = INamedSegment.super.compareTo(o);
        if (ret != 0) {
            return ret;
        }
        ret = IContentSegment.super.compareTo(o);
        if (ret != 0) {
            return ret;
        }
        return toString().compareTo(o.toString());
    }

    @Override
    public long getStart() {
        return fStart;
    }

    @Override
    public long getEnd() {
        return fEnd;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append(", [fTimestampStart=").append(getStart()) //$NON-NLS-1$
                .append(", fTimestampEnd=").append(getEnd()) //$NON-NLS-1$
                .append(", duration= ").append(getLength()) //$NON-NLS-1$
                .append(", fName=").append(getName()) //$NON-NLS-1$
                .append(", fContent=").append(getContent()) //$NON-NLS-1$
                .append("]").toString(); //$NON-NLS-1$
    }

    @Override
    public void writeSegment(@NonNull ISafeByteBufferWriter buffer) {

        buffer.putLong(fStart);
        buffer.putLong(fEnd);
        // Legacy support, was scale
        // FIXME: Ideally, we should version the writer and remove this value
        buffer.putInt(0);
        buffer.putString(fSegmentName);
        // Write the number of fields
        buffer.putInt(fContent.size());

        // Write the fields
        for (Map.Entry<String, ITmfStateValue> entry : fContent.entrySet()) {
            buffer.putString(entry.getKey());
            final ITmfStateValue value = entry.getValue();
            final byte type = getByteFromType(value.getType());
            buffer.put(type);
            switch (type) {
            case TYPE_NULL:
                break;
            case TYPE_INTEGER:
                buffer.putInt(value.unboxInt());
                break;
            case TYPE_LONG:
                buffer.putLong(value.unboxLong());
                break;
            case TYPE_STRING:
                final @NonNull String string = value.unboxStr();
                buffer.putString(string);
                break;
            default:
                Activator.logError("Write object failed : Invalid data"); //$NON-NLS-1$
            }
        }
    }

    @Override
    public int getSizeOnDisk() {
        int size = 2 * Long.BYTES + Integer.BYTES + SafeByteBufferFactory.getStringSizeInBuffer(fSegmentName) + Integer.BYTES;
        for (Map.Entry<String, ITmfStateValue> entry : fContent.entrySet()) {
            size += SafeByteBufferFactory.getStringSizeInBuffer(entry.getKey());
            final ITmfStateValue value = entry.getValue();
            final byte type = getByteFromType(value.getType());
            size += Byte.BYTES;
            switch (type) {
            case TYPE_NULL:
                break;
            case TYPE_INTEGER:
                size += Integer.BYTES;
                break;
            case TYPE_LONG:
                size += Long.BYTES;
                break;
            case TYPE_STRING:
                final @NonNull String string = value.unboxStr();
                size += SafeByteBufferFactory.getStringSizeInBuffer(string);
                break;
            default:
                Activator.logError("get segment size on disk failed : Invalid data"); //$NON-NLS-1$
            }
        }
        return size;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        // Write the number of fields
        out.writeInt(fContent.size());

        // Write the fields
        for (Map.Entry<String, ITmfStateValue> entry : fContent.entrySet()) {
            out.writeInt(entry.getKey().length());
            out.writeBytes(entry.getKey());
            final ITmfStateValue value = entry.getValue();
            final byte type = getByteFromType(value.getType());
            out.writeByte(type);
            switch (type) {
            case TYPE_NULL:
                break;
            case TYPE_INTEGER:
                out.writeInt(value.unboxInt());
                break;
            case TYPE_LONG:
                out.writeLong(value.unboxLong());
                break;
            case TYPE_STRING:
                final @NonNull String string = value.unboxStr();
                out.writeInt(string.length());
                out.writeBytes(string);
                break;
            default:
                throw new IOException("Write object failed : Invalid data"); //$NON-NLS-1$
            }
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int contentSize = in.readInt();

        final Map<@NonNull String, @NonNull ITmfStateValue> content = new HashMap<>();
        for (int i = 0; i < contentSize; i++) {
            int length = in.readInt();
            byte[] bytes = new byte[length];
            in.read(bytes, 0, length);
            String name = new String(bytes).intern();

            Byte type = in.readByte();
            ITmfStateValue value;
            switch (type) {
            case TYPE_NULL:
                value = TmfStateValue.nullValue();
                break;
            case TYPE_INTEGER:
                value = TmfStateValue.newValueInt(in.readInt());
                break;
            case TYPE_LONG:
                value = TmfStateValue.newValueLong(in.readLong());
                break;
            case TYPE_STRING:
                length = in.readInt();
                bytes = new byte[length];
                in.read(bytes, 0, length);
                value = TmfStateValue.newValueString(new String(bytes).intern());
                break;
            default:
                throw new IOException("Read object failed : Invalid data"); //$NON-NLS-1$
            }
            content.put(name, value);
        }
        fContent = content;
    }

    /**
     * Here we determine how state values "types" are written in the 8-bit field
     * that indicates the value type in the file.
     */
    private static byte getByteFromType(ITmfStateValue.Type type) {
        switch (type) {
        case NULL:
            return TYPE_NULL;
        case INTEGER:
            return TYPE_INTEGER;
        case STRING:
            return TYPE_STRING;
        case LONG:
            return TYPE_LONG;
        case DOUBLE:
        case CUSTOM:
        default:
            /* Should not happen if the switch is fully covered */
            throw new IllegalStateException("Data type " + type + " not supported"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }
}
