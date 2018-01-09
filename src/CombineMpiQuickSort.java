import java.util.Arrays;
import java.util.Random;

import mpi.MPI;
import mpi.MPIException;
import mpi.Status;

public class CombineMpiQuickSort {

	public final static int ROOT = 0;
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

	static void parallelQuickSort(int arr[], int left, int right, int prRank, int nProcessors, int rankIndex)
			throws MPIException {
		/* Calculate the rank of sharing process */
		int sharePr = prRank + (int) Math.pow(2, rankIndex);
		rankIndex++; // Increment the count index

		if (sharePr >= nProcessors) { // If no process to share sequentially
			quickSort(arr, left, right);
			return;
		}
		int offset = doPartition(arr, left, right);

		/*
		 * Send partition based on size, sort the remaining partitions, receive
		 * sorted partition
		 */
		if (offset > arr.length - offset) {
			int[] partition = Arrays.copyOfRange(arr, offset, right + 1);
			MPI.COMM_WORLD.send(partition, right - offset + 1, MPI.INT, sharePr, offset);
			parallelQuickSort(arr, left, offset - 1, prRank, nProcessors, rankIndex);
			MPI.COMM_WORLD.recv(partition, right - offset + 1, MPI.INT, sharePr, offset);
			System.arraycopy(partition, 0, arr, offset, right - offset + 1);
		} else {
			int[] partition = Arrays.copyOfRange(arr, 0, offset);
			MPI.COMM_WORLD.send(partition, offset, MPI.INT, sharePr, offset);
			parallelQuickSort(arr, offset, right, prRank, nProcessors, rankIndex);
			MPI.COMM_WORLD.recv(partition, offset, MPI.INT, sharePr, offset);
			System.arraycopy(partition, 0, arr, 0, offset);
		}
	}

	static int[] generateRandomArray(int n, int max) {
		int[] arr = new int[n];
		Random random = new Random();

		for (int i = 0; i < n; i++) {
			arr[i] = random.nextInt(max);
		}
		return arr;
	}

	public static void main(String[] args) throws MPIException {

		MPI.Init(args);
		int[] globalData = null;
		double startTime = 0;
		int n = DEFAULT_INPUT_SIZE;
		int max = 0;
		try {
			n = Integer.parseInt(args[0]);
			max = Integer.parseInt(args[1]);
		} catch (Exception e) {
			max = 5 * n;
		}

		int myself = MPI.COMM_WORLD.getRank();
		int nProcessors = Math.min(MPI.COMM_WORLD.getSize(), n);

		if (myself == ROOT) {
			globalData = generateRandomArray(n, max);
			if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
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

		MPI.COMM_WORLD.gather(localData, localSize, MPI.INT, globalData, localSize, MPI.INT, ROOT);

		if (myself == ROOT) {

			globalData = generateRandomArray(n, max);

			if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
				System.out.println("From ROOT - Input array: " + Arrays.toString(globalData));
			}

			parallelQuickSort(globalData, 0, globalData.length - 1, myself, nProcessors, 0);
			double endTime = MPI.wtime();

			System.out.printf("\nFrom ROOT - Total Time: %.2f\n", (endTime - startTime));

			if (Boolean.parseBoolean(System.getenv("DEBUG"))) {
				System.out.println("\nFrom ROOT - Result: " + Arrays.toString(globalData));
				int[] copyArr = Arrays.copyOf(globalData, n);
				Arrays.sort(copyArr);
				System.out.println("\nFrom ROOT - Compare Result: " + Arrays.equals(globalData, copyArr));
			}

		} else {

			int index_count = 0;
			while (Math.pow(2, index_count) <= myself)
				index_count++;

			Status status = MPI.COMM_WORLD.probe(MPI.ANY_SOURCE, MPI.ANY_TAG);
			localSize = status.getCount(MPI.INT);
			localData = new int[localSize];
			MPI.COMM_WORLD.recv(localData, localSize, MPI.INT, MPI.ANY_SOURCE, MPI.ANY_TAG);
			parallelQuickSort(localData, 0, localSize - 1, myself, nProcessors, index_count);
			MPI.COMM_WORLD.send(localData, localSize, MPI.INT, status.getSource(), status.getTag());

		}

		MPI.Finalize();
	}
}
