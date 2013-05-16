package contestWebsite;

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
			answer = y * (x + z);
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
}