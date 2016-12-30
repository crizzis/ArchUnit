package com.tngtech.archunit.core;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteStreams;
import com.tngtech.archunit.ArchConfiguration;

/**
 * Contains information about an imported class, i.e. the URI from where the class was imported and an md5 sum
 * to compare different versions of the same class file at the same location.
 * <p>
 * <b>NOTE</b>: Since the generation of md5 sums has a performance impact, it is disabled by default.<br/>
 * To enable it, add
 * <br/><br/><code>
 * {@value ArchConfiguration#ENABLE_MD5_IN_CLASS_SOURCES}=true
 * </code><br/><br/>
 * to your <code>{@value ArchConfiguration#ARCHUNIT_PROPERTIES_RESOURCE_NAME}</code>.
 * </p>
 */
public class Source {
    private final URI uri;
    private final Md5sum md5sum;

    Source(URI uri) {
        this.uri = uri;
        md5sum = Md5sum.of(uri);
    }

    public URI getUri() {
        return uri;
    }

    public Md5sum getMd5sum() {
        return md5sum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, md5sum);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Source other = (Source) obj;
        return Objects.equals(this.uri, other.uri)
                && Objects.equals(this.md5sum, other.md5sum);
    }

    @Override
    public String toString() {
        return uri + " [md5='" + md5sum + "']";
    }

    public static class Md5sum {
        /**
         * We can't determine the md5 sum, because the platform is missing the digest algorithm
         */
        static final Md5sum NOT_SUPPORTED = new Md5sum("NOT_SUPPORTED");
        /**
         * We can't determine the md5 sum, due to an error while digesting the source
         */
        static final Md5sum UNDETERMINED = new Md5sum("UNDETERMINED");
        /**
         * The calculation of md5 sums is disabled via {@link ArchConfiguration}
         */
        static final Md5sum DISABLED = new Md5sum("DISABLED");

        private static final MessageDigest MD5_DIGEST = getMd5Digest();

        private final byte[] md5Bytes;
        private final String text;

        private Md5sum(String text) {
            this.md5Bytes = new byte[0];
            this.text = text;
        }

        private Md5sum(byte[] input, MessageDigest md5Digest) {
            this.md5Bytes = md5Digest.digest(input);
            text = HashCode.fromBytes(md5Bytes).toString();
        }

        public byte[] asBytes() {
            return md5Bytes;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(md5Bytes) + 31 * text.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Md5sum other = (Md5sum) obj;
            return Arrays.equals(this.md5Bytes, other.md5Bytes)
                    && Objects.equals(this.text, other.text);
        }

        @Override
        public String toString() {
            return text;
        }

        private static MessageDigest getMd5Digest() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                return null;
            }
        }

        static Md5sum of(byte[] input) {
            if (MD5_DIGEST == null) {
                return NOT_SUPPORTED;
            }

            return ArchConfiguration.get().md5InClassSourcesEnabled() ? new Md5sum(input, MD5_DIGEST) : DISABLED;
        }

        private static Md5sum of(URI uri) {
            if (!ArchConfiguration.get().md5InClassSourcesEnabled()) {
                return DISABLED;
            }

            Optional<byte[]> bytesFromUri = read(uri);
            return bytesFromUri.isPresent() ? Md5sum.of(bytesFromUri.get()) : UNDETERMINED;
        }

        private static Optional<byte[]> read(URI uri) {
            try {
                return Optional.of(ByteStreams.toByteArray(uri.toURL().openStream()));
            } catch (Exception e) {
                return Optional.absent();
            }
        }
    }
}
