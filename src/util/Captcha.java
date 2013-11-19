/* Component of GAE Project for Dulles TMSCA Contest Automation
 * Copyright (C) 2013 Sushain Cherivirala
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]. 
 */

package util;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Random;


public class Captcha
{
	private Random rand;
	private final String[] captchas =
		{"{0} is {1} Planck times old. {2}, his godfather, will be {3} times as old as {0} in {4} Planck times. How many Planck times old is {2} currently?",
			"{0} builds {1} Very Large Hadron Colliders; he gives {2} Very Large Hadron Colliders to his godfather, {3}, and loses {4} Very Large Hadron Colliders. How many Very Large Hadron Colliders does {0} have left to give {5}?",
			"{0} is racing his friend, {1}, across a circular track of circumference {2} light years to impress {3}. {0} runs at {4} light years per second. How long in seconds does {0} take to go around the entire track?"
		};
	private String text = "";
	private String hashtext = "";
	private String salt = "";

	public String getHashedAnswer() { return hashtext; }
	public String getSalt() { return salt; }
	public String getQuestion() { return text; }

	public Captcha() throws IOException, NoSuchAlgorithmException
	{
		rand = new Random();
		ArrayList<String> args = new ArrayList<String>();
		int captcha = rand.nextInt(captchas.length);
		MessageFormat form = new MessageFormat(captchas[captcha]);
		NameGenerator gen = new NameGenerator("names");
		int answer = 0;
		if(captcha == 0)
		{
			int x = rand.nextInt(20)+10;
			int y = rand.nextInt(5)+3;
			int z = rand.nextInt(20)+10;
			args.add(gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2));
			args.add(String.valueOf(x));
			args.add(gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2));
			args.add(String.valueOf(y));
			args.add(String.valueOf(z));
			answer = y * (x + z) - z;
		}
		else if(captcha == 1)
		{
			int x = rand.nextInt(20)+10;
			int y = rand.nextInt(20)+10;
			int z = x + y + rand.nextInt(10)+10;
			args.add(gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2));
			args.add(String.valueOf(z));
			args.add(String.valueOf(x));
			args.add(gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2));
			args.add(String.valueOf(y));
			args.add(gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2));
			answer = z - x - y;
		}
		else
		{
			int x = rand.nextInt(30)+10;
			int y = x * (rand.nextInt(5)+2);
			args.add(gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2));
			args.add(gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2));
			args.add(String.valueOf(y));
			args.add(gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2) + " " + gen.compose(rand.nextInt(2)+2));
			args.add(String.valueOf(x));
			answer = y / x;
		}
		text = form.format(args.toArray());
		createSalt();
		computeHash(answer);
	}

	private void computeHash(int answer) throws NoSuchAlgorithmException
	{
		MessageDigest m = MessageDigest.getInstance("MD5");
		m.reset();
		m.update((salt + String.valueOf(answer)).getBytes());
		byte[] digest = m.digest();
		BigInteger bigInt = new BigInteger(1,digest);
		hashtext = bigInt.toString(16);
		while(hashtext.length() < 32)
			hashtext = "0" + hashtext;
	}

	private void createSalt()
	{
		SecureRandom random = new SecureRandom();
		salt = new BigInteger(130, random).toString(32);
	}
	
	public static boolean authCaptcha(String salt, String captcha, String hash) throws NoSuchAlgorithmException
	{
		MessageDigest m = MessageDigest.getInstance("MD5");
		String plaintext = salt + captcha;
		m.reset();
		m.update(plaintext.getBytes());
		byte[] digest = m.digest();
		BigInteger bigInt = new BigInteger(1,digest);
		String answer = bigInt.toString(16);
		while(answer.length() < 32)
			answer = "0" + answer;
		return answer.equals(hash);
	}
}