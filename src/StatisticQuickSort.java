import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatisticQuickSort {

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
		int n = 9;

		int times = 10;
		String[] classes = new String[] { "LinearMpiQuickSort", "RecursiveMpiQuickSort", "CombineMpiQuickSort" };

		double[] normalResult = new double[n];
		for (int i = 1; i <= n; i++) {
			int m = (int) Math.pow(10, i);
			String command = "java QuickSort " + m;
			System.out.println("\n=============");
			System.out.println(command);
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(command);
			normalResult[i - 1] = watch(pr);
		}

		StringBuilder builder = new StringBuilder();

		for (int i = 1; i <= np; i++) {
			for (int j = 1; j <= n; j++) {
				int m = (int) Math.pow(10, j);
				String temp = i + "\t" + m + "\t" + normalResult[j - 1];
				for (String clazz : classes) {
					double result = 0;
					for (int k = 0; k < times; k++) {
						String command = "mpirun -np " + i + " java " + clazz + " " + m;
						System.out.println("\n=============");
						System.out.println(command);
						Runtime rt = Runtime.getRuntime();
						Process pr = rt.exec(command);
						result += watch(pr);
					}

					temp += "\t" + result / times;
				}
				builder.append(temp).append("\n");
			}
		}

		try {
			Files.write(Paths.get("report-qsort.txt"), builder.toString().getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("saved to file!");
	}
}
