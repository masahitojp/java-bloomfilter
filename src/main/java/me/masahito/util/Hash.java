package me.masahito.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Hash {
    public static MessageDigest getSha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
