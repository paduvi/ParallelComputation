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
		int np = 16;
		int pow = 7;

		int times = 5;

		double[] normalResult = new double[pow];

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
				double result = 0;
				int success = 0;
				for (int k = 0; k < times; k++) {
					String command = "mpirun -np " + i + " java MpiBoyerMoore " + n + " " + m;
					System.out.println("\n=============");
					System.out.println(command);
					ProcessBuilder builder = new ProcessBuilder(command.split(" "));
					builder.redirectErrorStream(true);
					Process pr = builder.start();
					double tempResult = watch(pr);
					if (tempResult != -1) {
						result += tempResult;
						success++;
					}
				}

				if (success != 0) {
					temp += "\t" + result / success;
				} else {
					temp += "\t-1";
				}

				Files.write(Paths.get("report-boyer.txt"), (temp + "\n").getBytes(), StandardOpenOption.CREATE,
						StandardOpenOption.APPEND);
			}
		}

		System.out.println("saved to file!");
	}
}
