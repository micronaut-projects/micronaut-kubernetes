/*
 * Copyright 2017-2024 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.kubernetes.client.openapi.ssl;

import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Base64;

/**
 * Default implementation of the private key loader which supports PKCS#1, PKCS#8 and SEC 1 private key standards.
 */
@Singleton
@BootstrapContextCompatible
@Internal
public class DefaultPrivateKeyLoader implements KubernetesPrivateKeyLoader {

    @Override
    public PrivateKey loadPrivateKey(byte[] clientKey) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PemData pemData = parsePem(clientKey);
        if ("RSA".equals(pemData.keyAlgorithm)) {
            // load PKCS#1 key
            RSAPrivateCrtKeySpec keySpec = PKCS1Util.decodePKCS1(pemData.key);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } else if ("EC".equals(pemData.keyAlgorithm)) {
            // load SEC 1 key
            return KeyFactory.getInstance("EC").generatePrivate(PKCS1Util.getECKeySpec(pemData.key));
        } else {
            // load PKCS#8 key, first try RSA and then EC
            try {
                return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pemData.key));
            } catch (InvalidKeySpecException ex) {
                return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(pemData.key));
            }
        }
    }

    private PemData parsePem(byte[] clientKey) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(clientKey)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("-----BEGIN ")) {
                    String keyAlgorithm = "";
                    if (line.contains("BEGIN EC PRIVATE KEY")) {
                        keyAlgorithm = "EC";
                    } else if (line.contains("BEGIN RSA PRIVATE KEY")) {
                        keyAlgorithm = "RSA";
                    }
                    String endMarker = line.trim().replace("BEGIN", "END");
                    byte[] key = readBytes(reader, endMarker);
                    return new PemData(keyAlgorithm, key);
                }
            }
            throw new IOException("PEM is invalid: no begin marker");
        }
    }

    private byte[] readBytes(BufferedReader reader, String endMarker) throws IOException {
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(endMarker)) {
                return Base64.getDecoder().decode(builder.toString());
            }
            builder.append(line.trim());
        }
        throw new IOException("PEM is invalid : No end marker");
    }

    private record PemData(
        String keyAlgorithm,
        byte[] key
    ) {
    }
}
