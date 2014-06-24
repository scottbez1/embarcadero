package com.scottbezek.embarcadero.app.util;

public class Asserts {


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
