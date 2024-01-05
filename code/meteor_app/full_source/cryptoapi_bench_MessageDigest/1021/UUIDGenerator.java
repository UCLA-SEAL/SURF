package com.baeldung.uuid;

import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class UUIDGenerator {

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();


    public static UUID generateType5UUID(String name) {

        try {

            byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            byte[] hash = md.digest(bytes);

            long msb = peekLong(hash, 0, ByteOrder.BIG_ENDIAN);
            long lsb = peekLong(hash, 8, ByteOrder.BIG_ENDIAN);
            // Set the version field
            msb &= ~(0xfL << 12);
            msb |= ((long) 5) << 12;
            // Set the variant field to 2
            lsb &= ~(0x3L << 62);
            lsb |= 2L << 62;
            return new UUID(msb, lsb);

        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

}