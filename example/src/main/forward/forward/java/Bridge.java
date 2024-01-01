// SPDX-License-Identifier: MIT

package forward.java;

import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

// TODO: Remove bridge once all features are in place
public final class Bridge {
    private Bridge() {
    }

    public static InetSocketAddress createInetSocketAddress(int port) {
        return new InetSocketAddress("localhost", port);
    }

    public static HttpHandler createHandler(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getDeclaredConstructor().newInstance();

            if (instance instanceof HttpHandler handler) {
                return handler;
            } else {
                System.err.println("bridge: failed to create handler: " + className + " is not a HttpHandler");
                return null;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            System.err.println("bridge: failed to create handler: " + className + ": " + exception(e));
            return null;
        }
    }

    public static ResourceData readResource(String path) {
        try (InputStream is = Bridge.class.getResourceAsStream(path)) {
            if (is == null) {
                return null;
            }
            return new ResourceData(is.readAllBytes());
        } catch (IOException e) {
            System.err.println("bridge: failed to read: " + path + ": " + exception(e));
            return null;
        }
    }

    public static String asString(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    public static int byteLength(String str) {
        return str.getBytes(StandardCharsets.UTF_8).length;
    }

    public static long asLong(int i) {
        return i;
    }

    private static String exception(Throwable t) {
        String str = t.getClass().getCanonicalName() + " " + t.getMessage();
        Throwable cause = t.getCause();

        if (cause != null) {
            str += " (" + cause.getClass().getCanonicalName() + " " + cause.getMessage() + ")";
        }
        return str;
    }
}
