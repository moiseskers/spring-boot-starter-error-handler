package com.generic.springbootstartererrorhandler.helpers;

import org.apache.commons.lang.ArrayUtils;

import java.util.UUID;

public class UUIDHelper {

    public static UUID generateUniqueCodeHelper(String code, String message) {
        var codeBytes = code.getBytes();
        var messageBytes = message.getBytes();
        var bothBytes = ArrayUtils.addAll(codeBytes, messageBytes);
        return UUID.nameUUIDFromBytes(bothBytes);
    }
}
