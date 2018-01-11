import java.util.Random;

import mpi.MPI;
import mpi.MPIException;
import mpi.Status;

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

public class MpiBoyerMoore {

	public static final int ROOT = 0;
	public static final int DEFAULT_TEXT_SIZE = 20;
	public static final int DEFAULT_PATTERN_SIZE = 5;

	public final static int COMPUTE_TAG = 0;
	public final static int RIGHT_TAG = 1;
	public final static int END_TAG = 2;

	public final static int R = 256; // the radix
	private int[] right; // the bad-character skip array

	private String pat; // or as a string
	private int nProcessors;

	/**
	 * Pre-process the pattern string.
	 *
	 * @param pat
	 *            the pattern string
	 * @throws MPIException
	 */
	public MpiBoyerMoore(String pat, int nProcessors) throws MPIException {
		this.pat = pat;
		this.nProcessors = nProcessors;

		// position of rightmost occurrence of c in the pattern
		right = new int[R];
		for (int c = 0; c < R; c++)
			right[c] = -1;
		for (int j = 0; j < pat.length(); j++)
			right[pat.charAt(j)] = j;
		for (int i = 1; i < nProcessors; i++) {
			MPI.COMM_WORLD.send(right, R, MPI.INT, i, RIGHT_TAG);
		}
	}

	/**
	 * Returns the index of the first occurrence of the pattern string in the
	 * text string.
	 *
	 * @param txt
	 *            the text string
	 * @return the index of the first occurrence of the pattern string in the
	 *         text string; -1 if no such match
	 * @throws MPIException
	 */
	public int search(String txt) throws MPIException {
		int temp = this.nProcessors;
		while (temp-- > 0) {
			int result = search(txt, temp);
			if (result != -1)
				return result;
		}
		return -1;
	}

	public int search(String txt, int nProcessors) throws MPIException {
		int m = pat.length();
		int n = txt.length();
		int skip;
		for (int i = 0; i <= n - m; i += skip) {
			int localSize = m / nProcessors;
			for (int j = 1; j < nProcessors; j++) {
				StringBuilder builder = new StringBuilder();
				builder.append(txt.substring(i + (j - 1) * localSize, i + j * localSize));
				builder.append(pat.substring((j - 1) * localSize, j * localSize));
				MPI.COMM_WORLD.send(builder.toString().toCharArray(), localSize * 2, MPI.CHAR, j, COMPUTE_TAG);
			}

			skip = 0;

			for (int j = m - 1; j >= (nProcessors - 1) * localSize; j--) {
				if (pat.charAt(j) != txt.charAt(i + j)) {
					skip = Math.max(1, j - right[txt.charAt(i + j)]);
					break;
				}
			}

			for (int j = 1; j < nProcessors; j++) {
				int[] temp = new int[1];
				MPI.COMM_WORLD.recv(temp, 1, MPI.INT, j, COMPUTE_TAG);
				if (temp[0] != 0) {
					skip = temp[0];
				}
			}

			if (skip == 0)
				return i; // found
		}
		return -1; // not found
	}

	static int skipChar(String txt, String pat, int prRank, int[] right) {
		int m = pat.length();
		int offset = prRank * m;
		for (int j = m - 1; j >= 0; j--) {
			if (pat.charAt(j) != txt.charAt(j)) {
				return Math.max(1, j + offset - right[txt.charAt(j)]);
			}
		}
		return 0;
	}

	static String generateRandomString(int len) {
		String chars = "abcdefghijklmnopqrstuvwxyz";
		String nums = "0123456789";
		String symbols = "        ";
		String passSymbols = chars + nums + symbols;
		Random rnd = new Random();
		char[] password = new char[len];

		for (int i = 0; i < len; i++) {
			password[i] = passSymbols.charAt(rnd.nextInt(passSymbols.length()));
		}
		return String.valueOf(password);
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
	 * @throws MPIException
	 */
	public static void main(String[] args) throws MPIException {

		MPI.Init(args);

		int n = DEFAULT_TEXT_SIZE;
		int m = DEFAULT_PATTERN_SIZE;
		try {
			n = Integer.parseInt(args[0]);
			m = Integer.parseInt(args[1]);
			assert n >= m;
		} catch (Exception e) {
			m = Math.min(m, n);
		}

		int myself = MPI.COMM_WORLD.getRank();
		if (myself == ROOT) {
			int nProcessors = Math.min(MPI.COMM_WORLD.getSize(), m);

			String txt = generateRandomString(n);
			String pat = generateRandomSubstring(txt, m);

			double startTime = MPI.wtime();
			MpiBoyerMoore boyermoore = new MpiBoyerMoore(pat, nProcessors);
			int offset = boyermoore.search(txt);
			double endTime = MPI.wtime();

			// close all slave process
			for (int i = 1; i < nProcessors; i++) {
				MPI.COMM_WORLD.send(new byte[1], 1, MPI.BYTE, i, END_TAG);
			}

			if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
				// print results
				System.out.println("\nFrom ROOT - text:    " + txt);
			}

			if (offset == -1) {
				if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
					System.out.println("\nFrom ROOT - pattern mismatched: " + pat);
				}
			} else {
				if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
					System.out.print("From ROOT - pattern: ");
					for (int i = 0; i < offset; i++)
						System.out.print(" ");
					System.out.println(pat);
				}
				System.out.printf("\nFrom ROOT - Total Time: %.2f\n", (endTime - startTime));
			}
		} else {
			int[] right = new int[R];
			while (true) {
				Status status = MPI.COMM_WORLD.probe(ROOT, MPI.ANY_TAG);
				if (status.getTag() == END_TAG) {
					break;
				}

				if (status.getTag() == RIGHT_TAG) {
					MPI.COMM_WORLD.recv(right, R, MPI.INT, ROOT, RIGHT_TAG);
					continue;
				}

				int localSize = status.getCount(MPI.CHAR) / 2;
				char[] buffer = new char[localSize * 2];
				MPI.COMM_WORLD.recv(buffer, localSize * 2, MPI.CHAR, ROOT, COMPUTE_TAG);

				String temp = String.valueOf(buffer);
				int offset = skipChar(temp.substring(0, localSize), temp.substring(localSize), myself, right);

				MPI.COMM_WORLD.send(new int[] { offset }, 1, MPI.INT, ROOT, COMPUTE_TAG);
			}
		}

		MPI.Finalize();
	}
}