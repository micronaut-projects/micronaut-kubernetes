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
import jakarta.inject.Singleton;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.util.Base64;

/**
 * Default implementation of the private key loader which contains code copied from
 * <a href="https://github.com/kubernetes-client/java/blob/v21.0.1/util/src/main/java/io/kubernetes/client/util/SSLUtils.java">SSLUtils</a>.
 */
@Singleton
@BootstrapContextCompatible
class DefaultPrivateKeyLoader implements KubernetesPrivateKeyLoader {

    @Override
    public PrivateKey loadPrivateKey(byte[] clientKey) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String clientKeyAlgo = recognizePrivateKeyAlgo(clientKey);
        ByteArrayInputStream keyInputStream = new ByteArrayInputStream(clientKey);

        // Try PKCS7 / EC
        if (clientKeyAlgo.equals("EC")) {
            PEMParser pemParser = new PEMParser(new InputStreamReader(keyInputStream));
            Object pemObject;
            while ((pemObject = pemParser.readObject()) != null) {
                if (pemObject instanceof PEMKeyPair) {
                    return new JcaPEMKeyConverter().getKeyPair(((PEMKeyPair) pemObject)).getPrivate();
                }
            }
        }

        byte[] keyBytes = decodePem(keyInputStream);

        // Try PKCS1 / RSA
        if (clientKeyAlgo.equals("RSA")) {
            RSAPrivateCrtKeySpec keySpec = decodePKCS1(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        }

        // Try PKCS8
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (InvalidKeySpecException ex) {
            // ignore if it's not RSA
        }
        try {
            return KeyFactory.getInstance("ECDSA").generatePrivate(spec);
        } catch (InvalidKeySpecException ex) {
            // ignore if it's not DSA
        }
        throw new InvalidKeySpecException("Unknown type of PKCS8 Private Key, tried RSA and ECDSA");
    }

    public static String recognizePrivateKeyAlgo(byte[] privateKeyBytes) {
        String dataString = new String(privateKeyBytes);
        String algo = ""; // PKCS#8
        if (dataString.contains("BEGIN EC PRIVATE KEY")) {
            algo = "EC"; // PKCS#1 - EC
        }
        if (dataString.contains("BEGIN RSA PRIVATE KEY")) {
            algo = "RSA"; // PKCS#1 - RSA
        }
        return algo;
    }

    private static byte[] decodePem(InputStream keyInputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(keyInputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("-----BEGIN ")) {
                    return readBytes(reader, line.trim().replace("BEGIN", "END"));
                }
            }
            throw new IOException("PEM is invalid: no begin marker");
        }
    }

    private static byte[] readBytes(BufferedReader reader, String endMarker) throws IOException {
        String line;
        StringBuilder buf = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            if (line.indexOf(endMarker) != -1) {
                return Base64.getDecoder().decode(buf.toString());
            }
            buf.append(line.trim());
        }
        throw new IOException("PEM is invalid : No end marker");
    }

    public static RSAPrivateCrtKeySpec decodePKCS1(byte[] keyBytes) throws IOException {
        DerParser parser = new DerParser(keyBytes);
        Asn1Object sequence = parser.read();
        sequence.validateSequence();
        parser = new DerParser(sequence.getValue());
        parser.read();

        return new RSAPrivateCrtKeySpec(
            next(parser),
            next(parser),
            next(parser),
            next(parser),
            next(parser),
            next(parser),
            next(parser),
            next(parser));
    }

    private static BigInteger next(DerParser parser) throws IOException {
        return parser.read().getInteger();
    }

    static class DerParser {

        private InputStream in;

        DerParser(byte[] bytes) {
            this.in = new ByteArrayInputStream(bytes);
        }

        Asn1Object read() throws IOException {
            int tag = in.read();

            if (tag == -1) {
                throw new IOException("Invalid DER: stream too short, missing tag");
            }

            int length = getLength();
            byte[] value = new byte[length];
            if (in.read(value) < length) {
                throw new IOException("Invalid DER: stream too short, missing value");
            }

            return new Asn1Object(tag, value);
        }

        private int getLength() throws IOException {
            int i = in.read();
            if (i == -1) {
                throw new IOException("Invalid DER: length missing");
            }

            if ((i & ~0x7F) == 0) {
                return i;
            }

            int num = i & 0x7F;
            if (i >= 0xFF || num > 4) {
                throw new IOException("Invalid DER: length field too big (" + i + ")");
            }

            byte[] bytes = new byte[num];
            if (in.read(bytes) < num) {
                throw new IOException("Invalid DER: length too short");
            }

            return new BigInteger(1, bytes).intValue();
        }
    }

    static class Asn1Object {

        private final int type;
        private final byte[] value;
        private final int tag;

        public Asn1Object(int tag, byte[] value) {
            this.tag = tag;
            this.type = tag & 0x1F;
            this.value = value;
        }

        public byte[] getValue() {
            return value;
        }

        BigInteger getInteger() throws IOException {
            if (type != 0x02) {
                throw new IOException("Invalid DER: object is not integer"); // $NON-NLS-1$
            }
            return new BigInteger(value);
        }

        void validateSequence() throws IOException {
            if (type != 0x10) {
                throw new IOException("Invalid DER: not a sequence");
            }
            if ((tag & 0x20) != 0x20) {
                throw new IOException("Invalid DER: can't parse primitive entity");
            }
        }
    }
}
