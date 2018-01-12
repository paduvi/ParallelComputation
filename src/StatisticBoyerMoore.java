import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatisticBoyerMoore {

	private static double watch(final Process process) {
		BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		try {
			while ((line = input.readLine()) != null) {
				System.out.println(line);
				Matcher m = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?").matcher(line);
				if (line.toLowerCase().contains("time") && m.find()) {
					return Double.parseDouble(m.group());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public static void main(String[] args) throws IOException {
		int fraction = 20;
		int np = 16;
		int pow = 8;

		double[] normalResult = new double[pow];

		for (int i = 1; i <= pow; i++) {
			int m = (int) Math.pow(10, i);
			int n = m * fraction;

			String command = "java BoyerMoore " + n + " " + m;
			System.out.println("\n=============");
			System.out.println(command);
			ProcessBuilder builder = new ProcessBuilder(command.split(" "));
			builder.redirectErrorStream(true);
			Process pr = builder.start();
			normalResult[i - 1] = watch(pr);
		}

		for (int i = 1; i <= np; i++) {
			for (int j = 1; j <= pow; j++) {
				int m = (int) Math.pow(10, j);
				int n = m * fraction;
				String temp = i + "\t" + n + "\t" + m + "\t" + normalResult[j - 1];

				String command = "mpirun -np " + i + " java MpiBoyerMoore " + n + " " + m;
				System.out.println("\n=============");
				System.out.println(command);
				ProcessBuilder builder = new ProcessBuilder(command.split(" "));
				builder.redirectErrorStream(true);
				Process pr = builder.start();

				temp += "\t" + watch(pr);

				Files.write(Paths.get("report-boyer.txt"), (temp + "\n").getBytes(), StandardOpenOption.CREATE,
						StandardOpenOption.APPEND);
			}
		}

		int m = 10;

		for (int i = 2; i <= pow; i++) {
			int n = (int) Math.pow(10, i);

			String command = "java BoyerMoore " + n + " " + m;
			System.out.println("\n=============");
			System.out.println(command);
			ProcessBuilder builder = new ProcessBuilder(command.split(" "));
			builder.redirectErrorStream(true);
			Process pr = builder.start();
			normalResult[i - 1] = watch(pr);
		}

		for (int i = 1; i <= np; i++) {
			for (int j = 2; j <= pow; j++) {
				int n = (int) Math.pow(10, j);
				String temp = i + "\t" + n + "\t" + m + "\t" + normalResult[j - 1];

				String command = "mpirun -np " + i + " java MpiBoyerMoore " + n + " " + m;
				System.out.println("\n=============");
				System.out.println(command);
				ProcessBuilder builder = new ProcessBuilder(command.split(" "));
				builder.redirectErrorStream(true);
				Process pr = builder.start();
				temp += "\t" + watch(pr);

				Files.write(Paths.get("report-boyer.txt"), (temp + "\n").getBytes(), StandardOpenOption.CREATE,
						StandardOpenOption.APPEND);
			}
		}

		for (int i = 1; i <= np; i++) {
			for (int j = 2; j <= pow; j++) {
				int n = (int) Math.pow(10, j);

				for (int k = 0; k < 5; k++) {
					String temp = i + "\t" + n;
					String command = "mpirun -np " + i + " java MpiBoyerMoore " + n + " " + m;
					System.out.println("\n=============");
					System.out.println(command);
					ProcessBuilder builder = new ProcessBuilder(command.split(" "));
					builder.redirectErrorStream(true);
					Process pr = builder.start();
					temp += "\t" + watch(pr);

					Files.write(Paths.get("report-boyer.txt"), (temp + "\n").getBytes(), StandardOpenOption.CREATE,
							StandardOpenOption.APPEND);
				}
			}
		}

		System.out.println("saved to file!");
	}
}
