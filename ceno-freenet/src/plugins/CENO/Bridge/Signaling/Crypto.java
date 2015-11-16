package plugins.CENO.Bridge.Signaling;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;

import freenet.support.Base64;
import freenet.support.IllegalBase64Exception;
import freenet.support.Logger;

public final class Crypto {

	private static final int KEY_SIZE = 4096;
	private static final String KEY_ALGORITHM = "RSA";
	private static final String CIPHER_TRANSFORMATION = "RSA/None/OAEPWithSHA1AndMGF1Padding";
	private static final String SECURITY_PROVIDER = "BC";

	private static final SecureRandom srng = new SecureRandom();

	private Crypto() {}

	public static KeyPair generateAsymKey() throws NoSuchAlgorithmException, NoSuchProviderException {
		KeyPairGenerator generator = null;
		generator = KeyPairGenerator.getInstance(KEY_ALGORITHM, SECURITY_PROVIDER);
		generator.initialize(KEY_SIZE, srng);

		Long startTime = System.currentTimeMillis();
		KeyPair pair = generator.generateKeyPair();
		Logger.normal(Crypto.class, "Generated new RSA key in " +
				TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) + " seconds");
		return pair;
	}

	public static PrivateKey loadPrivateKey(String key64) throws GeneralSecurityException, IllegalBase64Exception {
		byte[] clear = Base64.decodeStandard(key64);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
		KeyFactory fact = KeyFactory.getInstance(KEY_ALGORITHM, SECURITY_PROVIDER);
		PrivateKey priv = fact.generatePrivate(keySpec);
		Arrays.fill(clear, (byte) 0);
		return priv;
	}


	public static PublicKey loadPublicKey(String key64) throws GeneralSecurityException, IllegalBase64Exception {
		byte[] data = Base64.decodeStandard(key64);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
		KeyFactory fact = KeyFactory.getInstance(KEY_ALGORITHM, SECURITY_PROVIDER);
		return fact.generatePublic(spec);
	}

	public static String savePrivateKey(PrivateKey priv) throws GeneralSecurityException {
		KeyFactory fact = KeyFactory.getInstance(KEY_ALGORITHM, SECURITY_PROVIDER);
		PKCS8EncodedKeySpec spec = fact.getKeySpec(priv, PKCS8EncodedKeySpec.class);
		byte[] packed = spec.getEncoded();
		String key64 = Base64.encodeStandard(packed);

		Arrays.fill(packed, (byte) 0);
		return key64;
	}

	public static String savePublicKey(PublicKey publ) throws GeneralSecurityException {
		KeyFactory fact = KeyFactory.getInstance(KEY_ALGORITHM, SECURITY_PROVIDER);
		X509EncodedKeySpec spec = fact.getKeySpec(publ, X509EncodedKeySpec.class);
		return Base64.encodeStandard(spec.getEncoded());
	}

	public static byte[] encrypt(byte[] msg, String pubKey64) throws GeneralSecurityException, IllegalBase64Exception {
		if (msg.length > 254) {
			throw new GeneralSecurityException("Cannot encrypt payload of that size");
		}

		PublicKey pubKey = loadPublicKey(pubKey64);
		Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION, SECURITY_PROVIDER);
		cipher.init(Cipher.ENCRYPT_MODE, pubKey, srng);
		byte[] cipherText = cipher.doFinal(msg);
		return cipherText;
	}

	public static byte[] decrypt(byte[] msg, String privKey64) throws GeneralSecurityException, IllegalBase64Exception {
		PrivateKey privKey = loadPrivateKey(privKey64);
		Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION, SECURITY_PROVIDER);
		cipher.init(Cipher.DECRYPT_MODE, privKey);
		byte[] plainText = cipher.doFinal(msg);
		return plainText;
	}

	public static boolean isValidKeypair(KeyPair keyPair) throws GeneralSecurityException, IllegalBase64Exception, UnsupportedEncodingException {
		if (keyPair == null) {
			return false;
		}
		return isValidKeypair(savePublicKey(keyPair.getPublic()), savePrivateKey(keyPair.getPrivate()));
	}

	public static boolean isValidKeypair(String pubKey64, String privKey64) throws UnsupportedEncodingException, GeneralSecurityException, IllegalBase64Exception {
		if (pubKey64 == null  || privKey64 == null) {
			return false;
		}

		byte[] cipherText = encrypt("Hello".getBytes("UTF-8"), pubKey64);
		String msg = new String(decrypt(cipherText, privKey64), "UTF-8");
		if (msg.equals("Hello")) {
			return true;
		}
		return false;
	}

}
