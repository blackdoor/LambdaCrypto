package lambdaCrypto;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;

import lambdaCrypto.Crypto.OpMode;


public class Demo {

	public static void main(String[] args) {
		Demo tester = new Demo();
		// Get Run-Mode
		if (args.length > 0) {
			// HELP CMD
			if (args[0].equals("-help")) {
				tester.printOut("./src/testpack/help.txt");
				System.exit(0);
			} else if (args[0].equals("-about")) {
				tester.printOut("./src/testpack/about.txt");
				System.exit(0);
			} else if (args[0].equals("-d")) { // DECRYPT CMD
				tester.setMode(RunMode.DECRYPT);
				tester.Run(args);
				System.exit(0);
			} else if (args[0].equals("-e")) { // ENCRYPT CMD
				tester.setMode(RunMode.ENCRYPT);
				tester.Run(args);
				System.exit(0);
			} else {
				System.err.println("invalid argument:" + args[0]);
				System.exit(1);
			}
		}
		// RUNDEFAULT
		tester.Run(null);
	}

	public enum RunMode {
		DEFAULT(), ENCRYPT(), DECRYPT();

		private final String description;

		private RunMode() {
			this.description = "";
		}

		private RunMode(String a) {
			this.description = a;
		}

		@Override
		public String toString() {
			return description;
		}
	}

	private RunMode runmode;
	private String infile = "src/testpack/in.txt";
	private String firstoutfile = "src/testpack/first_out.txt";
	private String finaloutfile = "src/testpack/final_out.txt";
	private String cipher = "SHE";
	// TODO
	private String blockmode = "OFB";
	private final int BLOCKSIZE = 32;
	private byte[] IV = new byte[BLOCKSIZE];
	private byte[] key = new byte[BLOCKSIZE];
	private byte[] plaintext = new byte[100];
	private byte[] ciphertext = new byte[100];
	private int msglength;

	public Demo() {
		runmode = RunMode.DEFAULT;
	}

	private boolean setMode(RunMode mode) {
		runmode = mode;
		return false;
	}

	private void Run(String[] args) {
		try {
			if (args != null) {
				setFiles(args);
				if (runmode == RunMode.ENCRYPT) {
					ciphertext = runEncrypt(infile);
					writeByteArrayToFile(firstoutfile, ciphertext);
				} else if (runmode == RunMode.DECRYPT) {
					plaintext = runDecrypt(firstoutfile);
					writeByteArrayToFile(finaloutfile, plaintext);
				}
			} else {
				ciphertext = runEncrypt(infile);

				writeByteArrayToFile(firstoutfile, ciphertext);
				plaintext = runDecrypt(ciphertext);
				writeByteArrayToFile(finaloutfile, plaintext);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Major Problems in RUN. Some stuff went down");
		}
	}

	private byte[] runEncrypt(String filename) {
		byte[] _plaintext = fileToByteArray(new File(filename));
		// writeByteArrayToFile("src/testpack/testtest.txt", _plaintext);
		Crypto crypto = new Crypto(OpMode.ENCRYPT);

		IV = crypto.init(chooseCipher(), chooseBlockMode(crypto), key);
		return runCipher(crypto, _plaintext);
	}

	private byte[] runDecrypt(String filename) {
		byte[] _ciphertext = fileToByteArray(new File(filename));
		Crypto crypto = new Crypto(Crypto.OpMode.DECRYPT);
		crypto.init(chooseCipher(), chooseBlockMode(crypto), IV, key);
		return runCipher(crypto, _ciphertext);
	}

	private byte[] runDecrypt(byte[] barray) {
		Crypto crypto = new Crypto(Crypto.OpMode.DECRYPT);
		crypto.init(chooseCipher(), chooseBlockMode(crypto), IV, key);
		return runCipher(crypto, barray);
	}

	private CipherAlgorithm chooseCipher() {
		CipherAlgorithm choice = null;
		switch (cipher) {
		case "SHE":
			choice = (byte[] key, byte[] inputtext) -> {
				MessageDigest mD = null;
				try {
					mD = MessageDigest.getInstance("SHA-256");
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
				
				key = mD.digest(key);
				
				if(key.length != inputtext.length)
					throw new RuntimeException("Parameters are not same length for xor.");
				
				for(int i = 0; i < key.length; i++)
					key[i] = (byte) (key[i]^inputtext[i]);
				
				return key;
			};
			break;
		case "NULL":
			choice = (byte[] key, byte[] inputtext) -> {return inputtext;
			};
			break;
		}
		return choice;
	}

	private BlockCipherMode chooseBlockMode(Crypto crypto) {
		BlockCipherMode choice = null;
		if (crypto.getRunMode() == OpMode.ENCRYPT)
			switch (blockmode) {
			case "OFB":
				choice = Modes.getOFB();
				break;
			case "CBC":
				choice = Modes.getCBCEncrypt();
				break;
			}
		else {
			switch (blockmode) {
			case "OFB":
				choice = Modes.getOFB();
				break;
			case "CBC":
				choice = Modes.getCBCDecrypt();
				break;
			}
		}
		return choice;
	}

	private byte[] runCipher(Crypto crypto, byte[] inputtext) {
		byte[] final_text = crypto.doFinal(Arrays.copyOf(inputtext,
				inputtext.length));
		return final_text;
	}

	private void setFiles(String[] args) {
		// TODO
		if (args[0] != "-x") {
			if (args.length == 4) {
				cipher = args[1];
				blockmode = args[2];
				infile = args[3];
				firstoutfile = args[4] + ".txt";
				finaloutfile = args[4] + "_final.txt";
			} else if (args.length == 4) {
				cipher = args[1];
				blockmode = args[2];
				infile = args[3];
			} else if (args.length == 3) {
				cipher = args[1];
				blockmode = args[2];
			} else if (args.length == 2) {
				cipher = args[1];
			}
		} else {
			if (args.length == 4) {
				infile = args[2];
				firstoutfile = args[3] + ".txt";
				finaloutfile = args[3] + "_final.txt";
			} else if (args.length == 3) {
				infile = args[2];
			}
		}
	}

	private void printOut(String filename) {
		File file = new File(filename);
		try {
			Scanner in = new Scanner(file);
			while (in.hasNextLine()) {
				System.out.println(in.nextLine());
			}
			in.close();
		} catch (FileNotFoundException e) {
			System.out.println("Failure to open file to be displayed");
			e.printStackTrace();
		}
	}

	private byte[] fileToByteArray(File file) {
		try {
			byte[] out = Files.readAllBytes(file.toPath());
			return out;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Error with file in!! ahhhhhh!!");
		}
		return null;
	}

	private void writeByteArrayToFile(String filename, byte[] text) {
		BufferedOutputStream bos = null;
		try {
			System.out.println("printing...");
			FileOutputStream fos = new FileOutputStream(new File(filename));

			bos = new BufferedOutputStream(fos);

			bos.write(text);
			bos.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Write out didnt work!");
		}

	}
}
