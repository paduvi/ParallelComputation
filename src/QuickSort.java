import java.util.Arrays;
import java.util.Random;

public class QuickSort {

	public final static int DEFAULT_INPUT_SIZE = 20;

	static int doPartition(int arr[], int left, int right) {
		int i = left, j = right;
		int tmp;
		int pivot = arr[(left + right) / 2];

		while (i <= j) {
			while (arr[i] < pivot)
				i++;
			while (arr[j] > pivot)
				j--;
			if (i <= j) {
				tmp = arr[i];
				arr[i] = arr[j];
				arr[j] = tmp;
				i++;
				j--;
			}
		}

		return i;
	}

	static void quickSort(int arr[], int left, int right) {
		int offset = doPartition(arr, left, right);
		if (left < offset - 1)
			quickSort(arr, left, offset - 1);
		if (offset < right)
			quickSort(arr, offset, right);
	}

	static int[] generateRandomArray(int n, int max) {
		int[] arr = new int[n];
		Random random = new Random();

		for (int i = 0; i < n; i++) {
			arr[i] = random.nextInt(max);
		}
		return arr;
	}

	public static void main(String[] args) {
		int n = DEFAULT_INPUT_SIZE;
		int max = 0;
		try {
			n = Integer.parseInt(args[0]);
			max = Integer.parseInt(args[1]);
		} catch (Exception e) {
			max = 5 * n;
		}
		int[] globalData = generateRandomArray(n, max);

		if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
			System.out.println("Input: " + Arrays.toString(globalData));
		}

		double startTime = System.currentTimeMillis();
		quickSort(globalData, 0, n - 1);
		double endTime = System.currentTimeMillis();

		System.out.printf("Total Time: %.2f\n", (endTime - startTime));

		if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
			System.out.println("Result: " + Arrays.toString(globalData));
			int[] copyArr = Arrays.copyOf(globalData, n);
			Arrays.sort(copyArr);
			System.out.println("\nFrom ROOT - Compare Result: " + Arrays.equals(globalData, copyArr));
		}
	}
}
