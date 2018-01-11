import java.util.Random;

/**
 * The {@code BoyerMoore} class finds the first occurrence of a pattern string
 * in a text string.
 * <p>
 * This implementation uses the Boyer-Moore algorithm (with the bad-character
 * rule, but not the strong good suffix rule).
 * <p>
 * For additional documentation, see
 * <a href="https://algs4.cs.princeton.edu/53substring">Section 5.3</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 */
public class BoyerMoore {

	public final static int DEFAULT_TEXT_SIZE = 20;
	public final static int DEFAULT_PATTERN_SIZE = 5;

	private final int R; // the radix
	private int[] right; // the bad-character skip array

	private String pat; // or as a string

	/**
	 * Pre-process the pattern string.
	 *
	 * @param pat
	 *            the pattern string
	 */
	public BoyerMoore(String pat) {
		this.R = 256;
		this.pat = pat;

		// position of rightmost occurrence of c in the pattern
		right = new int[R];
		for (int c = 0; c < R; c++)
			right[c] = -1;
		for (int j = 0; j < pat.length(); j++)
			right[pat.charAt(j)] = j;
	}

	/**
	 * Returns the index of the first occurrence of the pattern string in the
	 * text string.
	 *
	 * @param txt
	 *            the text string
	 * @return the index of the first occurrence of the pattern string in the
	 *         text string; n if no such match
	 */
	public int search(String txt) {
		int m = pat.length();
		int n = txt.length();
		int skip;
		for (int i = 0; i <= n - m; i += skip) {
			skip = 0;
			for (int j = m - 1; j >= 0; j--) {
				if (pat.charAt(j) != txt.charAt(i + j)) {
					skip = Math.max(1, j - right[txt.charAt(i + j)]);
					break;
				}
			}
			if (skip == 0)
				return i; // found
		}
		return -1; // not found
	}

	static String generateRandomString(int len) {
		String chars = "abcdefghijklmnopqrstuvwxyz";
		String nums = "0123456789";
		String symbols = "        ";
		String passSymbols = chars + nums + symbols;
		Random rnd = new Random();
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < len; i++) {
			builder.append((char) rnd.nextInt(passSymbols.length()));
		}
		return String.valueOf(builder.toString());
	}

	static String generateRandomSubstring(String txt, int len) {
		Random rnd = new Random();
		int index = rnd.nextInt(txt.length() - len);
		return txt.substring(index, index + len);
	}

	/**
	 * Takes a pattern string and an input string as command-line arguments;
	 * searches for the pattern string in the text string; and prints the first
	 * occurrence of the pattern string in the text string.
	 *
	 * @param args
	 *            the command-line arguments
	 */
	public static void main(String[] args) {
		int n = DEFAULT_TEXT_SIZE;
		int m = DEFAULT_PATTERN_SIZE;
		try {
			n = Integer.parseInt(args[0]);
			m = Integer.parseInt(args[1]);
			assert n >= m;
		} catch (Exception e) {
			m = Math.min(m, n);
		}

		String txt = generateRandomString(n);
		String pat = generateRandomSubstring(txt, m);

		double startTime = System.currentTimeMillis();
		BoyerMoore boyermoore = new BoyerMoore(pat);
		int offset = boyermoore.search(txt);
		double endTime = System.currentTimeMillis();

		if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
			// print results
			System.out.println("text:    " + txt);
		}

		if (offset == -1) {
			if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
				System.out.println("pattern mismatched: " + pat);
			}
		} else {
			if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
				System.out.print("pattern: ");
				for (int i = 0; i < offset; i++)
					System.out.print(" ");
				System.out.println(pat);
			}

			System.out.printf("\nTotal Time: %.2f\n", (endTime - startTime));
		}
	}
}