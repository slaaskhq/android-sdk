package com.slaask.sdk;

import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SlaaskIdentity {
    private HashMap<String, String> attributes;

    public SlaaskIdentity() {
        this.attributes = new HashMap<>();
        setName("Unknown");
        setCustomAttribute("kind", "lead");
    }

    public SlaaskIdentity setId(String id) {
        try {
            setCustomAttribute("user_hash", generateHashWithHmac256(id, Slaask.getInstance().getSecretKey()));
            setCustomAttribute("id", id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public SlaaskIdentity setId(String id, String secretKey) {
        try {
            setCustomAttribute("user_hash", generateHashWithHmac256(id, secretKey));
            setCustomAttribute("id", id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public SlaaskIdentity setName(String name) {
        return setCustomAttribute("name", name);
    }

    public SlaaskIdentity setEmail(String email) {
        return setCustomAttribute("email", email);
    }

    public SlaaskIdentity setAvatar(String avatar) {
        return setCustomAttribute("avatar", avatar);
    }

    public SlaaskIdentity setRegisteredAt(String registeredAt) {
        return setCustomAttribute("registered_at", registeredAt);
    }

    public SlaaskIdentity setCustomAttribute(String key, String value) {
        this.attributes.put(key, value);
        return this;
    }

    public String build() {
        return new JSONObject(attributes).toString();
    }

    private String generateHashWithHmac256(String message, String key) {
        try {
            final String hashingAlgorithm = "HmacSHA256";
            byte[] bytes = hmac(hashingAlgorithm, key.getBytes(), message.getBytes());
            return bytesToHex(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static byte[] hmac(String algorithm, byte[] key, byte[] message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(message);
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0, v; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}