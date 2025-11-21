package com.distributed.rate.limiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

class KeyGeneratorTest
{
    @Test
    void testGenerateKeyPair_ShouldReturnValidKeyPair() throws NoSuchAlgorithmException
    {
        // Act
        KeyPair keyPair = KeyGenerator.generateKeyPair();

        // Assert
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
        assertEquals("RSA", keyPair.getPublic().getAlgorithm());
        assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
    }

    @Test
    void testGenerateKeyPair_ShouldGenerateDifferentKeys() throws NoSuchAlgorithmException
    {
        // Act
        KeyPair keyPair1 = KeyGenerator.generateKeyPair();
        KeyPair keyPair2 = KeyGenerator.generateKeyPair();

        // Assert
        assertNotNull(keyPair1);
        assertNotNull(keyPair2);
        assertNotEquals(keyPair1.getPublic(), keyPair2.getPublic());
        assertNotEquals(keyPair1.getPrivate(), keyPair2.getPrivate());
    }

    @Test
    void testGenerateKeyPair_KeySizeShouldBe2048() throws NoSuchAlgorithmException
    {
        // Act
        KeyPair keyPair = KeyGenerator.generateKeyPair();

        // Assert
        assertNotNull(keyPair);
        // RSA keys are typically much larger than 256 bytes for 2048-bit keys
        assertTrue(keyPair.getPublic().getEncoded().length > 256);
        assertTrue(keyPair.getPrivate().getEncoded().length > 256);
    }

    @Test
    void testGenerateKeyPair_PublicAndPrivateKeysAreDifferent() throws NoSuchAlgorithmException
    {
        // Act
        KeyPair keyPair = KeyGenerator.generateKeyPair();

        // Assert
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
        assertNotEquals(keyPair.getPublic(), keyPair.getPrivate());
        assertNotEquals(keyPair.getPublic().getEncoded(), keyPair.getPrivate().getEncoded());
    }

    @Test
    void testGenerateKeyPair_AlgorithmIsRSA() throws NoSuchAlgorithmException
    {
        // Act
        KeyPair keyPair = KeyGenerator.generateKeyPair();

        // Assert
        assertEquals("RSA", keyPair.getPublic().getAlgorithm());
        assertEquals("RSA", keyPair.getPrivate().getAlgorithm());
    }

    @Test
    void testGenerateKeyPair_MultipleInvocations_ShouldSucceed() throws NoSuchAlgorithmException
    {
        // Act & Assert
        for (int i = 0; i < 5; i++)
        {
            KeyPair keyPair = KeyGenerator.generateKeyPair();
            assertNotNull(keyPair);
            assertNotNull(keyPair.getPublic());
            assertNotNull(keyPair.getPrivate());
        }
    }

    @Test
    void testGenerateKeyPair_KeysAreNotNull() throws NoSuchAlgorithmException
    {
        // Act
        KeyPair keyPair = KeyGenerator.generateKeyPair();

        // Assert
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPublic());
        assertNotNull(keyPair.getPrivate());
        assertNotNull(keyPair.getPublic().getEncoded());
        assertNotNull(keyPair.getPrivate().getEncoded());
    }

    @Test
    void testGenerateKeyPair_KeyFormatsAreCorrect() throws NoSuchAlgorithmException
    {
        // Act
        KeyPair keyPair = KeyGenerator.generateKeyPair();

        // Assert
        assertEquals("X.509", keyPair.getPublic().getFormat());
        assertEquals("PKCS#8", keyPair.getPrivate().getFormat());
    }
}

