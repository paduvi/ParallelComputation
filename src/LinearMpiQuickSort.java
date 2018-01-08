import java.util.Arrays;
import java.util.Random;

import mpi.MPI;
import mpi.MPIException;

public class LinearMpiQuickSort {

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

	final static int ROOT = 0;
	final static int DEFAULT_INPUT_SIZE = 20;

	public static void main(String[] args) throws MPIException {

		MPI.Init(args);
		int[] globalData = null;
		double startTime = 0, endTime = 0;
		int n = DEFAULT_INPUT_SIZE;
		int max = 0;
		try {
			n = Integer.valueOf(args[0]);
			max = Integer.valueOf(args[1]);
		} catch (Exception e) {
			max = 5 * n;
		}

		int myself = MPI.COMM_WORLD.getRank();
		int nProcessors = MPI.COMM_WORLD.getSize();
		if (nProcessors > n) {
			System.out.println("SIZE of array is less than number of processors!");
			System.exit(1);
		}

		if (myself == ROOT) {
			globalData = generateRandomArray(n, max);
			if (n < 100) {
				System.out.println("From ROOT - Input array: " + Arrays.toString(globalData));
			}
			startTime = MPI.wtime();
		}

		int localSize = n / nProcessors;
		if (myself == nProcessors - 1) {
			localSize += n % nProcessors;
		}

		int[] localData = new int[localSize];
		MPI.COMM_WORLD.scatter(globalData, localSize, MPI.INT, localData, localSize, MPI.INT, ROOT);

		quickSort(localData, 0, localSize - 1);
		if (localSize < 100) {
			System.out.println("\nFrom Processor " + myself + " - Local Result: " + Arrays.toString(localData));
		}

		MPI.COMM_WORLD.gather(localData, localSize, MPI.INT, globalData, localSize, MPI.INT, ROOT);
		if (myself == ROOT) {
			quickSort(globalData, 0, n - 1);
			endTime = MPI.wtime();
			System.out.printf("\nFrom ROOT - Total Time: %.2f\n", (endTime - startTime));
			if (n < 100) {
				System.out.println("\nFrom ROOT - Result: " + Arrays.toString(globalData));
			}

			if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
				int[] copyArr = Arrays.copyOf(globalData, n);
				Arrays.sort(copyArr);
				System.out.println("\nFrom ROOT - Compare Result: " + Arrays.equals(globalData, copyArr));
			}
		}

		MPI.Finalize();
	}
}
