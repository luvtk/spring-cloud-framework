package xyz.luvtk.auth.utils;

import org.bouncycastle.asn1.ASN1Sequence;
import org.springframework.security.jwt.codec.Codecs;

import java.security.*;
import java.security.spec.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tank Zheng
 * @since 20190918
 * JWT token构造器配置
 */
public final class JWTTokenBuilder {

	/**
	 * 密钥匹配格式
	 */
	private static final Pattern PEM_DATA = Pattern.compile("-----BEGIN (.*)-----(.*)-----END (.*)-----", Pattern.DOTALL);

	public static KeyPair parseKeyPair(String pemData) {
		Matcher m = PEM_DATA.matcher(pemData.trim());

		if (!m.matches()) {
			throw new IllegalArgumentException("String is not PEM encoded data");
		}

		String type = m.group(1);
		final byte[] content = Codecs.b64Decode(Codecs.utf8Encode(m.group(2)));

		PublicKey publicKey;
		PrivateKey privateKey = null;

		try {
			KeyFactory fact = KeyFactory.getInstance("RSA");
			if (type.equals("RSA PRIVATE KEY")) {
				ASN1Sequence seq = ASN1Sequence.getInstance(content);
				if (seq.size() != 9) {
					throw new IllegalArgumentException("Invalid RSA Private Key ASN1 sequence.");
				}
				org.bouncycastle.asn1.pkcs.RSAPrivateKey key = org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(seq);
				RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent());
				RSAPrivateCrtKeySpec privSpec = new RSAPrivateCrtKeySpec(key.getModulus(), key.getPublicExponent(),
					key.getPrivateExponent(), key.getPrime1(), key.getPrime2(), key.getExponent1(), key.getExponent2(),
					key.getCoefficient());
				publicKey = fact.generatePublic(pubSpec);
				privateKey = fact.generatePrivate(privSpec);
			} else if (type.equals("PUBLIC KEY")) {
				KeySpec keySpec = new X509EncodedKeySpec(content);
				publicKey = fact.generatePublic(keySpec);
			} else if (type.equals("RSA PUBLIC KEY")) {
				ASN1Sequence seq = ASN1Sequence.getInstance(content);
				org.bouncycastle.asn1.pkcs.RSAPublicKey key = org.bouncycastle.asn1.pkcs.RSAPublicKey.getInstance(seq);
				RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(key.getModulus(), key.getPublicExponent());
				publicKey = fact.generatePublic(pubSpec);
			} else {
				throw new IllegalArgumentException(type + " is not a supported format");
			}

			return new KeyPair(publicKey, privateKey);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
