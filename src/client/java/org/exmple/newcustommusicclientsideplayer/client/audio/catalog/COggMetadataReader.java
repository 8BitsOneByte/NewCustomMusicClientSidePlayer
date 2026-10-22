package org.exmple.newcustommusicclientsideplayer.client.audio.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

final class COggMetadataReader {
    static final int INPUT_BUFFER_BYTES = 256 * 1024;
    static final int PAGE_BUFFER_BYTES = 65_307;
    static final int WORK_BUFFER_LIMIT_BYTES = 1024 * 1024;
    static final int WORK_BUFFER_BYTES = INPUT_BUFFER_BYTES + PAGE_BUFFER_BYTES + 30 + 256 * Integer.BYTES;
    private static final int[] CRC_TABLE = createCrcTable();
    private final byte[] inputBuffer = new byte[INPUT_BUFFER_BYTES];
    private final byte[] page = new byte[PAGE_BUFFER_BYTES];
    private final byte[] prefix = new byte[30];
    private InputStream input;
    private BooleanSupplier cancelled;
    private int inputPosition;
    private int inputLimit;

    COggMetadataReader() {
        if (WORK_BUFFER_BYTES > WORK_BUFFER_LIMIT_BYTES) {
            throw new IllegalStateException("Buffer budget");
        }
    }

    CAudioResourceInfo.Metadata read(InputStream stream, BooleanSupplier cancellation) throws IOException {
        input = stream;
        cancelled = cancellation;
        inputPosition = 0;
        inputLimit = 0;
        try {
            return readStream();
        } finally {
            input = null;
            cancelled = null;
        }
    }

    private CAudioResourceInfo.Metadata readStream() throws IOException {
        boolean first = true;
        boolean ended = false;
        boolean packetOpen = false;
        int serial = 0;
        int sequence = 0;
        int headers = 0;
        int prefixLength = 0;
        long packetLength = 0;
        long previousGranule = 0;
        long sampleCount = -1;
        int sampleRate = 0;
        int channels = 0;
        boolean audio = false;
        while (readPage()) {
            checkCancelled();
            int flags = page[5] & 0xff;
            int pageSerial = littleInt(page, 14);
            if (ended || (!first && pageSerial != serial)) {
                throw failure(CAudioResourceInfo.MetadataStatus.UNSUPPORTED);
            }
            if (first) {
                if ((flags & 3) != 2 || littleInt(page, 18) != 0) {
                    throw invalid();
                }
                serial = pageSerial;
                first = false;
            } else if ((flags & 2) != 0 || littleInt(page, 18) != sequence) {
                throw invalid();
            }
            sequence = littleInt(page, 18) + 1;
            if (((flags & 1) != 0) != packetOpen) {
                throw invalid();
            }
            int segments = page[26] & 0xff;
            int offset = 27 + segments;
            boolean finishedPacket = false;
            for (int i = 0; i < segments; i++) {
                int length = page[27 + i] & 0xff;
                int copy = Math.min(length, prefix.length - prefixLength);
                System.arraycopy(page, offset, prefix, prefixLength, copy);
                prefixLength += copy;
                packetLength += length;
                offset += length;
                packetOpen = length == 255;
                if (packetOpen) {
                    continue;
                }
                finishedPacket = true;
                if (headers < 3) {
                    if (prefixLength < 7 || prefix[0] != (byte) (1 + 2 * headers)
                            || prefix[1] != 'v' || prefix[2] != 'o' || prefix[3] != 'r'
                            || prefix[4] != 'b' || prefix[5] != 'i' || prefix[6] != 's') {
                        throw failure(headers == 0 ? CAudioResourceInfo.MetadataStatus.UNSUPPORTED
                                : CAudioResourceInfo.MetadataStatus.INVALID);
                    }
                    if (headers == 0) {
                        if (packetLength < 30 || littleInt(prefix, 7) != 0) {
                            throw invalid();
                        }
                        channels = prefix[11] & 0xff;
                        sampleRate = littleInt(prefix, 12);
                        int smallBlock = prefix[28] & 0xf;
                        int largeBlock = (prefix[28] & 0xff) >>> 4;
                        if (channels == 0 || sampleRate <= 0 || smallBlock < 6 || largeBlock > 13
                                || smallBlock > largeBlock || (prefix[29] & 1) == 0) {
                            throw invalid();
                        }
                    }
                    headers++;
                } else {
                    if (packetLength == 0 || (prefix[0] & 1) != 0) {
                        throw invalid();
                    }
                    audio = true;
                }
                prefixLength = 0;
                packetLength = 0;
            }
            long granule = littleLong(page, 6);
            if (granule < -1 || (granule >= 0 && granule < previousGranule)
                    || (!finishedPacket && granule != -1)) {
                throw invalid();
            }
            if (granule >= 0) {
                previousGranule = granule;
            }
            if ((flags & 4) != 0) {
                if (packetOpen || headers != 3 || !audio || granule < 0) {
                    throw invalid();
                }
                sampleCount = granule;
                ended = true;
            }
        }
        if (!ended) {
            throw invalid();
        }
        return new CAudioResourceInfo.Metadata(sampleCount, sampleRate, channels);
    }

    private boolean readPage() throws IOException {
        checkCancelled();
        int first = readByte();
        if (first < 0) {
            return false;
        }
        page[0] = (byte) first;
        readFully(page, 1, 26);
        if (page[0] != 'O' || page[1] != 'g' || page[2] != 'g' || page[3] != 'S'
                || page[4] != 0 || (page[5] & 0xf8) != 0) {
            throw invalid();
        }
        int segments = page[26] & 0xff;
        readFully(page, 27, segments);
        int bodyLength = 0;
        for (int i = 0; i < segments; i++) {
            bodyLength += page[27 + i] & 0xff;
        }
        int length = 27 + segments + bodyLength;
        readFully(page, 27 + segments, bodyLength);
        int expected = littleInt(page, 22);
        int crc = 0;
        for (int i = 0; i < length; i++) {
            int value = i >= 22 && i < 26 ? 0 : page[i] & 0xff;
            crc = (crc << 8) ^ CRC_TABLE[((crc >>> 24) ^ value) & 0xff];
        }
        if (crc != expected) {
            throw invalid();
        }
        return true;
    }

    private int readByte() throws IOException {
        if (inputPosition == inputLimit && !refill()) {
            return -1;
        }
        return inputBuffer[inputPosition++] & 0xff;
    }

    private void readFully(byte[] target, int offset, int length) throws IOException {
        while (length > 0) {
            if (inputPosition == inputLimit && !refill()) {
                throw invalid();
            }
            int copy = Math.min(length, inputLimit - inputPosition);
            System.arraycopy(inputBuffer, inputPosition, target, offset, copy);
            inputPosition += copy;
            offset += copy;
            length -= copy;
        }
    }

    private boolean refill() throws IOException {
        checkCancelled();
        inputPosition = 0;
        inputLimit = input.read(inputBuffer);
        if (inputLimit == 0) {
            int value = input.read();
            inputLimit = value < 0 ? -1 : 1;
            inputBuffer[0] = (byte) value;
        }
        return inputLimit > 0;
    }

    private void checkCancelled() {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new CancellationException();
        }
    }

    private static int littleInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16) | ((bytes[offset + 3] & 0xff) << 24);
    }

    private static long littleLong(byte[] bytes, int offset) {
        return Integer.toUnsignedLong(littleInt(bytes, offset))
                | ((long) littleInt(bytes, offset + 4) << 32);
    }

    private static int[] createCrcTable() {
        int[] table = new int[256];
        for (int i = 0; i < table.length; i++) {
            int value = i << 24;
            for (int bit = 0; bit < 8; bit++) {
                value = (value << 1) ^ ((value & 0x80000000) != 0 ? 0x04c11db7 : 0);
            }
            table[i] = value;
        }
        return table;
    }

    private static ReadException invalid() {
        return failure(CAudioResourceInfo.MetadataStatus.INVALID);
    }

    private static ReadException failure(CAudioResourceInfo.MetadataStatus status) {
        return new ReadException(status);
    }

    static final class ReadException extends IOException {
        final CAudioResourceInfo.MetadataStatus status;

        ReadException(CAudioResourceInfo.MetadataStatus status) {
            super(status.name());
            this.status = status;
        }
    }
}
