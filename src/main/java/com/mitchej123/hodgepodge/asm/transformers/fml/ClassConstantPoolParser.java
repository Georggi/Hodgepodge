package com.mitchej123.hodgepodge.asm.transformers.fml;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.objectweb.asm.Opcodes;

/**
 * Using this class to search for a (single) String reference is > 40 times faster than parsing a class with a
 * ClassReader + ClassNode while using way less RAM
 */
class ClassConstantPoolParser {

    private static final int UTF8 = 1;
    private static final int INT = 3;
    private static final int FLOAT = 4;
    private static final int LONG = 5;
    private static final int DOUBLE = 6;
    private static final int FIELD = 9;
    private static final int METH = 10;
    private static final int IMETH = 11;
    private static final int NAME_TYPE = 12;
    private static final int HANDLE = 15;
    private static final int INDY = 18;

    private final ArrayList<byte[]> BYTES_TO_SEARCH;

    public ClassConstantPoolParser(String... strings) {
        BYTES_TO_SEARCH = new ArrayList<>(strings.length);
        for (int i = 0; i < strings.length; i++) {
            BYTES_TO_SEARCH.add(i, strings[i].getBytes(StandardCharsets.UTF_8));
        }
    }

    public void addString(String string) {
        BYTES_TO_SEARCH.add(string.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns true if the constant pool of the class represented by this byte array contains one of the Strings we are
     * looking for
     */
    public boolean find(byte[] basicClass) {
        return find(basicClass, false);
    }

    /**
     * Returns true if the constant pool of the class represented by this byte array contains one of the Strings we are
     * looking for.
     *
     * @param prefixes If true, it is enough for a constant pool entry to <i>start</i> with one of our Strings to count
     *                 as a match - otherwise, the entire String has to match.
     */
    public boolean find(byte[] basicClass, boolean prefixes) {
        if (basicClass == null || basicClass.length == 0) {
            return false;
        }
        // checks the class version
        if (readShort(6, basicClass) > Opcodes.V1_8) {
            return false;
        }
        // parses the constant pool
        final int n = readUnsignedShort(8, basicClass);
        int index = 10;
        for (int i = 1; i < n; ++i) {
            final int size;
            switch (basicClass[index]) {
                case FIELD:
                case METH:
                case IMETH:
                case INT:
                case FLOAT:
                case NAME_TYPE:
                case INDY:
                    size = 5;
                    break;
                case LONG:
                case DOUBLE:
                    size = 9;
                    ++i;
                    break;
                case UTF8:
                    final int strLen = readUnsignedShort(index + 1, basicClass);
                    size = 3 + strLen;
                    for (byte[] bytes : BYTES_TO_SEARCH) {
                        if (prefixes ? strLen >= bytes.length : strLen == bytes.length) {
                            boolean found = true;
                            for (int j = index + 3; j < index + 3 + bytes.length; j++) {
                                if (basicClass[j] != bytes[j - (index + 3)]) {
                                    found = false;
                                    break;
                                }
                            }
                            if (found) {
                                return true;
                            }
                        }
                    }
                    break;
                case HANDLE:
                    size = 4;
                    break;
                default:
                    size = 3;
                    break;
            }
            index += size;
        }
        return false;
    }

    private static short readShort(final int index, byte[] basicClass) {
        return (short) (((basicClass[index] & 0xFF) << 8) | (basicClass[index + 1] & 0xFF));
    }

    private static int readUnsignedShort(final int index, byte[] basicClass) {
        return ((basicClass[index] & 0xFF) << 8) | (basicClass[index + 1] & 0xFF);
    }

}
