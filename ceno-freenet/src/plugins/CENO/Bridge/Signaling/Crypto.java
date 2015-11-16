package plugins.CENO.Bridge.Signaling;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;

import freenet.support.Logger;

public final class Crypto {

	private static final int ASYM_KEY_MODULUS_LENGTH = 4096;
	private static final BigInteger ASYM_KEY_EXPONENT = new BigInteger("17", 10);
	private static final int ASYM_KEY_CERTAINTY = 80;

	private static final SecureRandom srng = new SecureRandom();

	private Crypto() {}

	public static AsymmetricCipherKeyPair generateAsymKey() {
		RSAKeyGenerationParameters kparams = new RSAKeyGenerationParameters(ASYM_KEY_EXPONENT, Crypto.srng, ASYM_KEY_MODULUS_LENGTH, ASYM_KEY_CERTAINTY);

		RSAKeyPairGenerator kpg = new RSAKeyPairGenerator();
		kpg.init(kparams);

		Long timeStart = System.currentTimeMillis();
		AsymmetricCipherKeyPair keypair = kpg.generateKeyPair();
		Logger.normal(Crypto.class, "Generated new RSA keypair in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - timeStart) + " seconds");

		return keypair;
	}

	public static byte[] encryptMessage(byte[] signedMessage, String keyModulus, String keyExponent) {
		//Make a new symmetric key for the message
		byte[] aesKeyAndIV = new byte[32 + 16];
		Crypto.srng.nextBytes(aesKeyAndIV);

		//Encrypt the message with the new symmetric key
		PaddedBufferedBlockCipher aesCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
		KeyParameter aesKeyParameters = new KeyParameter(aesKeyAndIV, 16, 32);
		ParametersWithIV aesParameters = new ParametersWithIV(aesKeyParameters, aesKeyAndIV, 0, 16);
		aesCipher.init(true, aesParameters);

		byte[] encryptedMessage = new byte[aesCipher.getOutputSize(signedMessage.length)];
		int offset = aesCipher.processBytes(signedMessage, 0, signedMessage.length, encryptedMessage, 0);

		try {
			aesCipher.doFinal(encryptedMessage, offset);
		} catch(InvalidCipherTextException e) {
			Logger.error(Crypto.class, "Failed to perform symmetric encryption on RTS data: " + e.getMessage(), e);
			return null;
		}

		RSAKeyParameters recipientPublicKey = new RSAKeyParameters(false, new BigInteger(keyModulus, 32), new BigInteger(keyExponent, 32));
		AsymmetricBlockCipher keyCipher = new RSAEngine();
		keyCipher.init(true, recipientPublicKey);
		byte[] encryptedAesParameters = null;
		try {
			encryptedAesParameters = keyCipher.processBlock(aesKeyAndIV, 0, aesKeyAndIV.length);
		} catch(InvalidCipherTextException e) {
			Logger.error(Crypto.class, "Failed to perform asymmetric encryption on RTS symmetric key: " + e.getMessage(), e);
			return null;
		}

		//Assemble the final message
		byte[] rtsMessage = new byte[encryptedAesParameters.length + encryptedMessage.length];
		System.arraycopy(encryptedAesParameters, 0, rtsMessage, 0, encryptedAesParameters.length);
		System.arraycopy(encryptedMessage, 0, rtsMessage, encryptedAesParameters.length, encryptedMessage.length);

		return rtsMessage;
	}

	public static byte[] decryptMessage(byte[] rtsmessage, RSAKeyParameters privKey) throws IOException, InvalidCipherTextException {
		if (!privKey.isPrivate()) {
			return null;
		}
		
		// initialise our ciphers
		AsymmetricBlockCipher deccipher = new RSAEngine();
		deccipher.init(false, privKey);

		PaddedBufferedBlockCipher aescipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new PKCS7Padding());

		// first n bytes will be an encrypted RSA block containting the
		// AES IV and Key. Read that.
		byte[] encrypted_params = new byte[deccipher.getInputBlockSize()];
		if(rtsmessage.length < encrypted_params.length) {
			throw new InvalidCipherTextException("Message too short");
		}

		System.arraycopy(rtsmessage, 0, encrypted_params, 0, encrypted_params.length);

		byte[] aes_iv_and_key = deccipher.processBlock(encrypted_params, 0, encrypted_params.length);

		KeyParameter kp = new KeyParameter(aes_iv_and_key, aescipher.getBlockSize(), aes_iv_and_key.length - aescipher.getBlockSize());
		ParametersWithIV kpiv = new ParametersWithIV(kp, aes_iv_and_key, 0, aescipher.getBlockSize());
		try {
			aescipher.init(false, kpiv);
		} catch (IllegalArgumentException iae) {
			throw new InvalidCipherTextException(iae.getMessage());
		}

		byte[] plaintext = new byte[aescipher.getOutputSize(rtsmessage.length - encrypted_params.length)];

		//aescipher.processBytes(rtsmessage, rtsmessage.length, rtsmessage.length - encrypted_params.length, plaintext, 0);
		aescipher.doFinal(plaintext, rtsmessage.length - encrypted_params.length);

		return plaintext;
	}
	
	private static byte[] signRtsMessage(byte[] rtsMessageBytes, RSAKeyParameters ourPrivateKey) {
		SHA256Digest sha256 = new SHA256Digest();
		sha256.update(rtsMessageBytes, 0, rtsMessageBytes.length);
		byte[] hash = new byte[sha256.getDigestSize()];
		sha256.doFinal(hash, 0);

		AsymmetricBlockCipher signatureCipher = new RSAEngine();
		signatureCipher.init(true, ourPrivateKey);
		byte[] signature = null;
		try {
			signature = signatureCipher.processBlock(hash, 0, hash.length);
		} catch(InvalidCipherTextException e) {
			Logger.error(Crypto.class, "Failed to RSA encrypt hash: " + e.getMessage(), e);
			return null;
		}

		byte[] signedMessage = new byte[rtsMessageBytes.length + signature.length];
		System.arraycopy(rtsMessageBytes, 0, signedMessage, 0, rtsMessageBytes.length);
		System.arraycopy(signature, 0, signedMessage, rtsMessageBytes.length, signature.length);

		return signedMessage;
	}

	public static boolean isValidKeypair(String privKey, String pubKey, String modulus) {
		if (privKey == null || pubKey == null || modulus == null) {
			return false;
		}
		
		AsymmetricCipherKeyPair asymKeyPair = Crypto.generateAsymKey();

		try {
			String privMsg = new String(encryptMessage(signRtsMessage("Hello".getBytes("UTF-8"), ((RSAKeyParameters)asymKeyPair.getPrivate())), ((RSAKeyParameters)asymKeyPair.getPublic()).getModulus().toString(32), ((RSAKeyParameters)asymKeyPair.getPublic()).getExponent().toString(32)));
			String decMsg = decryptMessage(privMsg.getBytes(), ((RSAKeyParameters)asymKeyPair.getPrivate())).toString();
			System.out.println(decMsg);
			return true;
		} catch (Exception e) {
			Logger.warning(Crypto.class, "Invalid RSA keypair: " + e.getMessage());
			return false;
		}
	}

}
