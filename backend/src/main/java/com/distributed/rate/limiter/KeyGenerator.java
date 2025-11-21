package com.distributed.rate.limiter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class KeyGenerator
{
    private static final String ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String PRIVATE_KEY_FILE = "private.key";
    private static final String PUBLIC_KEY_FILE = "public.key";
    private static final String PRIVATE_KEY_BEGIN_BOUNDARY = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_KEY_END_BOUNDARY = "-----END PRIVATE KEY-----";
    private static final String PUBLIC_KEY_BEGIN_BOUNDARY = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_KEY_END_BOUNDARY = "-----END PUBLIC KEY-----";

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());

        return keyPairGenerator.generateKeyPair();
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException
    {
        KeyPair keyPair = generateKeyPair();
        String publicKey = Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getMimeEncoder().encodeToString(keyPair.getPrivate().getEncoded());
        Path privateKeyFilePath = Paths.get(PRIVATE_KEY_FILE);
        Path publicKeyFilePath = Paths.get(PUBLIC_KEY_FILE);

        Files.writeString(privateKeyFilePath,
            PRIVATE_KEY_BEGIN_BOUNDARY + "\n" + privateKey + "\n" + PRIVATE_KEY_END_BOUNDARY);
        Files.writeString(publicKeyFilePath,
            PUBLIC_KEY_BEGIN_BOUNDARY + "\n" + publicKey + "\n" + PUBLIC_KEY_END_BOUNDARY);

        System.out.println("Private key generated at location: " + privateKeyFilePath.toAbsolutePath());
        System.out.println("Public key generated at location: " + publicKeyFilePath.toAbsolutePath());
        // You would then typically store these keys securely, e.g., in files or a vault.
    }
}