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

	/**
	 * Pre-process the pattern string.
	 *
	 * @param pat
	 *            the pattern string
	 * @throws MPIException
	 */
	public MpiBoyerMoore(String pat, int nProcessors) throws MPIException {
		this.pat = pat;

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

	public static int search(String txt, String pat, int[] right) throws MPIException {
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
		String symbols = "   ";
		String passSymbols = chars + nums + symbols;
		Random rnd = new Random();
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < len; i++) {
			builder.append(passSymbols.charAt(rnd.nextInt(passSymbols.length())));
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

		int nProcessors = Math.min(MPI.COMM_WORLD.getSize(), n);
		int myself = MPI.COMM_WORLD.getRank();

		if (myself == ROOT) {
			String txt = generateRandomString(n);
			String pat = generateRandomSubstring(txt, m);

			int localSize = n / nProcessors;

			if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
				// print input
				System.out.println("\nFrom ROOT - text:    " + txt);
			}

			double startTime = MPI.wtime();
			MpiBoyerMoore boyermoore = new MpiBoyerMoore(pat, nProcessors);

			for (int i = 1; i < nProcessors; i++) {
				int start = Math.max(i * localSize - m, 0);
				int end = (i + 1) * localSize;
				if (i == nProcessors - 1) {
					end += n % nProcessors;
				}
				MPI.COMM_WORLD.send((pat + txt.substring(start, end)).toCharArray(), end - start + m, MPI.CHAR, i,
						COMPUTE_TAG);
			}

			int offset = boyermoore.search(txt.substring(0, localSize));

			if (offset == -1) {
				for (int i = 1; i < nProcessors; i++) {
					int[] temp = new int[1];
					MPI.COMM_WORLD.recv(temp, 1, MPI.INT, i, COMPUTE_TAG);
					if (temp[0] != -1) {
						offset = temp[0];
						break;
					}
				}
			}
			double endTime = MPI.wtime();

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
			MPI.COMM_WORLD.recv(right, R, MPI.INT, ROOT, RIGHT_TAG);
			Status status = MPI.COMM_WORLD.probe(ROOT, COMPUTE_TAG);

			int bufferSize = status.getCount(MPI.CHAR);
			char[] buffer = new char[bufferSize];
			MPI.COMM_WORLD.recv(buffer, bufferSize, MPI.CHAR, ROOT, COMPUTE_TAG);

			String temp = String.valueOf(buffer);
			int offset = MpiBoyerMoore.search(temp.substring(m), temp.substring(0, m), right);

			int localSize = n / nProcessors;
			int start = Math.max(myself * localSize - m, 0);
			if (offset == -1) {
				MPI.COMM_WORLD.send(new int[] { offset }, 1, MPI.INT, ROOT, COMPUTE_TAG);
			} else {
				MPI.COMM_WORLD.send(new int[] { offset + start }, 1, MPI.INT, ROOT, COMPUTE_TAG);
			}
		}

		MPI.Finalize();
	}
}