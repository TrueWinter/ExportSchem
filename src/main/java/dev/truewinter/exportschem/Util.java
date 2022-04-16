package dev.truewinter.exportschem;

import java.util.UUID;

public class Util {
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
