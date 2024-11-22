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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;

/**
 * This code has been copied from
 * <a href="https://github.com/fabric8io/kubernetes-client/blob/main/kubernetes-client-api/src/main/java/io/fabric8/kubernetes/client/internal/PKCS1Util.java">PKCS1Util</a>.
 */
final class PKCS1Util {

  private PKCS1Util() {
  }

  public static RSAPrivateCrtKeySpec decodePKCS1(byte[] keyBytes) throws IOException {
    DerParser parser = new DerParser(keyBytes);
    Asn1Object sequence = parser.read();
    sequence.validateSequence();
    parser = new DerParser(sequence.getValue());
    parser.read();

    return new RSAPrivateCrtKeySpec(next(parser), next(parser),
        next(parser), next(parser),
        next(parser), next(parser),
        next(parser), next(parser));
  }

  // ==========================================================================================

  private static BigInteger next(DerParser parser) throws IOException {
    return parser.read().getInteger();
  }

  // adapted from io.vertx.core.net.impl.pkcs1.PrivateKeyParser

  public static ECPrivateKeySpec getECKeySpec(byte[] keyBytes) throws IOException {
    DerParser parser = new DerParser(keyBytes);

    Asn1Object sequence = parser.read();
    if (sequence.type != DerParser.SEQUENCE) {
      throw new RuntimeException("Invalid DER: not a sequence");
    }

    // Parse inside the sequence
    parser = new DerParser(sequence.value);

    Asn1Object version = parser.read();
    if (version.type != DerParser.INTEGER) {
      throw new RuntimeException(String.format(
          "Invalid DER: 'version' field must be of type INTEGER (2) but found type `%d`",
          version.type));
    } else if (version.getInteger().intValue() != 1) {
      throw new RuntimeException(String.format(
          "Invalid DER: expected 'version' field to have value '1' but found '%d'",
          version.getInteger().intValue()));
    }
    byte[] privateValue = parser.read().getValue();
    parser = new DerParser(parser.read().getValue());
    Asn1Object params = parser.read();
    // ECParameters are mandatory according to RFC 5915, Section 3
    if (params.type != DerParser.OBJECT_IDENTIFIER) {
      throw new RuntimeException(String.format(
          "Invalid DER: expected to find an OBJECT_IDENTIFIER (6) in 'parameters' but found type '%d'",
          params.type));
    }
    byte[] namedCurveOid = params.getValue();
    ECParameterSpec spec = getECParameterSpec(oidToString(namedCurveOid));
    return new ECPrivateKeySpec(new BigInteger(1, privateValue), spec);
  }

  private static ECParameterSpec getECParameterSpec(String curveName) {
    Provider[] providers = Security.getProviders();
    GeneralSecurityException ex = null;
    // scan through the providers to see if anyone supports this
    for (Provider provider : providers) {
      try {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", provider);
        keyPairGenerator.initialize(new ECGenParameterSpec(curveName));
        ECPublicKey publicKey = (ECPublicKey) keyPairGenerator.generateKeyPair().getPublic();
        return publicKey.getParams();
      } catch (GeneralSecurityException e) {
        ex = e;
      }
    }
    boolean bcProvider = Security.getProvider("BC") != null || Security.getProvider("BCFIPS") != null;
    throw new RuntimeException("Cannot determine EC parameter spec for curve name/OID" + (bcProvider ? ""
        : ". A BouncyCastle provider is not installed, it may be needed for this EC algorithm."), ex);
  }

  private static String oidToString(byte[] oid) {
    StringBuilder result = new StringBuilder();
    int value = oid[0] & 0xff;
    result.append(value / 40).append(".").append(value % 40);
    for (int index = 1; index < oid.length; ++index) {
      byte bValue = oid[index];
      if (bValue < 0) {
        value = (bValue & 0b01111111);
        ++index;
        if (index == oid.length) {
          throw new IllegalArgumentException("Invalid OID");
        }
        value <<= 7;
        value |= (oid[index] & 0b01111111);
        result.append(".").append(value);
      } else {
        result.append(".").append(bValue);
      }
    }
    return result.toString();
  }


    static class DerParser {

        private static final int SEQUENCE = 0x10;
        private static final int INTEGER = 0x02;
        private static final int OBJECT_IDENTIFIER = 0x06;
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
                throw new IOException("Invalid DER: length field too big ("
                    + i + ")");
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
                throw new IOException("Invalid DER: object is not integer"); //$NON-NLS-1$
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
