/* Code from Stack Overflow Question:
 * http://stackoverflow.com/questions/2860943/suggestions-for-library-to-hash-passwords-in-java
 * Ouestion by Chris Dutrow:
 * http://stackoverflow.com/users/84131/chris-dutrow
 * Answer by Martin Konicek
 * http://stackoverflow.com/users/90998/martin-konicek
 */

package util;

import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Base64;

public class Password
{
	// The higher the number of iterations the more
	// expensive computing the hash is for us
	// and also for a brute force attack.
	private static final int iterations = 10*1024;
	private static final int saltLen = 32;
	private static final int desiredKeyLen = 256;

	/** Computes a salted PBKDF2 hash of given plaintext password
        suitable for storing in a database.
        Empty passwords are not supported. */
	public static String getSaltedHash(String password) throws Exception
	{
		byte[] salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(saltLen);
		// store the salt with the password
		return Base64.encodeBase64String(salt) + "$" + hash(password, salt);
	}

	/** Checks whether given plaintext password corresponds
        to a stored salted hash of the password. */
	public static boolean check(String password, String stored) throws Exception{
		String[] saltAndPass = stored.split("\\$");
		if (saltAndPass.length != 2)
			return false;
		String hashOfInput = hash(password, Base64.decodeBase64(saltAndPass[0]));
		return hashOfInput.equals(saltAndPass[1]);
	}

	// using PBKDF2 from Sun, an alternative is https://github.com/wg/scrypt
	// cf. http://www.unlimitednovelty.com/2012/03/dont-use-bcrypt.html
	public static String hash(String password, byte[] salt) throws Exception {
		if (password == null || password.length() == 0)
			throw new IllegalArgumentException("Empty passwords are not supported.");
		SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		SecretKey key = f.generateSecret(new PBEKeySpec(
				password.toCharArray(), salt, iterations, desiredKeyLen)
				);
		return Base64.encodeBase64String(key.getEncoded());
	}
}