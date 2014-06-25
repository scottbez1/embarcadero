package com.scottbezek.embarcadero.app.util;

import android.os.Looper;

public class Asserts {


    public static void assertMainThreadOnly() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new AssertionError("Must be called on the main thread");
        }
    }

    public static void assertNotMainThread() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new AssertionError("Cannot be called on the main thread");
        }
    }

    /**
     * Assert that expected and actual are equal, otherwise throws an
     * {@link AssertionError}.
     */
    public static void assertEqual(Object expected, Object actual, String message) {
        if (expected == null && actual == null) {
            return;
        } else if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(message);
        }
    }

    public static void assertEqual(Object expected, Object actual) {
        assertEqual(expected, actual, "Expected " + expected + " but got " + actual);
    }

    public static void assertTrue(boolean value, String message) {
        if (value == false) {
            throw new AssertionError(message);
        }
    }

    public static void assertTrue(boolean value) {
        assertTrue(value, "Expected true");
    }

    public static void assertFalse(boolean value, String message) {
        if (value == true) {
            throw new AssertionError(message);
        }
    }

    public static void assertFalse(boolean value) {
        assertFalse(value, "Expected False");
    }

    public static <T> T assertNotNull(T o) {
        assertNotNull(o, "Shouldn't be null");
        return o;
    }

    public static void assertNotNull(Object o, String message) {
        if (o == null) {
            throw new AssertionError(message);
        }
    }

    public static void assertNull(Object o) {
        if (o != null) {
            throw new AssertionError("Should be null");
        }
    }

    public static RuntimeException fail() {
        throw new AssertionError("This shouldn't happen");
    }

    public static void assertAllEqual(int firstValue, int... values) {
        boolean fail = false;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != firstValue) {
                fail = true;
                break;
            }
        }

        if (fail) {
            StringBuilder sb = new StringBuilder("Expected all the same value: [");
            sb.append(firstValue);
            for (int i = 0; i < values.length; i++) {
                sb.append(", ");
                sb.append(values[i]);
            }
            sb.append("]");
            throw new AssertionError(sb.toString());
        }
    }

    public static void assertAllEqual(boolean firstValue, boolean... values) {
        boolean fail = false;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != firstValue) {
                fail = true;
                break;
            }
        }

        if (fail) {
            StringBuilder sb = new StringBuilder("Expected all the same value: [");
            sb.append(firstValue);
            for (int i = 0; i < values.length; i++) {
                sb.append(", ");
                sb.append(values[i]);
            }
            sb.append("]");
            throw new AssertionError(sb.toString());
        }
    }
}
