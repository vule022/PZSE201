package rs.medikarton.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public final class LozinkaServis {

    private static final String ALGORITAM = "PBKDF2WithHmacSHA256";
    private static final int ITERACIJA = 120_000;
    private static final int DUZINA_KLJUCA_BITA = 256;
    private static final int DUZINA_SOLI_BAJTOVA = 16;

    private static final SecureRandom NASUMICNI = new SecureRandom();

    private LozinkaServis() {
        throw new AssertionError("LozinkaServis se ne instancira.");
    }

    //generisanje SALT za sifru
    public static String generisiSo() {
        byte[] so = new byte[DUZINA_SOLI_BAJTOVA];
        NASUMICNI.nextBytes(so);
        return Base64.getEncoder().encodeToString(so);
    }

    public static String hash(String lozinka, String soBase64) {
        if (lozinka == null || lozinka.isEmpty()) {
            throw new IllegalArgumentException("Lozinka ne sme biti prazna.");
        }
        if (soBase64 == null || soBase64.isBlank()) {
            throw new IllegalArgumentException("So ne sme biti prazna.");
        }
        byte[] so = Base64.getDecoder().decode(soBase64);
        PBEKeySpec spec = new PBEKeySpec(lozinka.toCharArray(), so, ITERACIJA, DUZINA_KLJUCA_BITA);
        try {
            SecretKeyFactory fabrika = SecretKeyFactory.getInstance(ALGORITAM);
            byte[] izvod = fabrika.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(izvod);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Kriptografski algoritam nije dostupan: " + ALGORITAM, e);
        } finally {
            spec.clearPassword();
        }
    }

    //provera lozinke
    public static boolean proveri(String lozinka, String soBase64, String ocekivaniHash) {
        if (lozinka == null || soBase64 == null || ocekivaniHash == null) {
            return false;
        }
        String dobijeni = hash(lozinka, soBase64);
        return MessageDigest.isEqual(
                dobijeni.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                ocekivaniHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static int brojIteracija() {
        return ITERACIJA;
    }
}
