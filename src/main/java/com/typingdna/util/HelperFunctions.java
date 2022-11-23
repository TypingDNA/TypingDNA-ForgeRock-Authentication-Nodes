/*
  Copyright 2020 TypingDNA Inc. (https://www.typingdna.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/


package com.typingdna.util;

import org.forgerock.json.JsonValue;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HelperFunctions {
    public static String trimUrl(String url) {
        if (url == null || url.equals("")) {
            return url;
        }

        String trimmedUrl = url.trim();
        while (trimmedUrl.length() > 1 && trimmedUrl.endsWith("/")) {
            trimmedUrl = trimmedUrl.substring(0, trimmedUrl.length() - 1);
        }

        return trimmedUrl;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String hashText(String text, String salt, HashAlgorithm algorithm) {
        String hashedText = "";
        try {
            MessageDigest hash = MessageDigest.getInstance(algorithm.toString());
            hash.update(String.format("%s%s", salt, text).getBytes());
            hashedText = HelperFunctions.bytesToHex(hash.digest());
        } catch (NoSuchAlgorithmException e) {
        }

        return hashedText;
    }

    public static String fnv1a32(String text) {
        if (text == null) {
            return "";
        }

        String data = text.toLowerCase();
        BigInteger hash = new BigInteger("721b5ad4", 16);

        for (byte b : data.getBytes()) {
            hash = hash.xor(BigInteger.valueOf((int) b & 0xff));
            hash = hash.multiply(new BigInteger("01000193", 16)).mod(new BigInteger("2").pow(32));
        }
        return hash.toString();
    }

    public static <T> T getValueFromJson(JsonValue json, String key, T defaultValue) {
        if (json == null) {
            return defaultValue;
        }

        Object value = json.get(key).getObject();
        if (value == null) {
            return defaultValue;
        }

        return (T) value;
    }
}
