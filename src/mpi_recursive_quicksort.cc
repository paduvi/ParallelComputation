#include <mpi.h>
#include <time.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <iostream>
#include <algorithm>

const int ROOT = 0;
MPI_Status status;

int doPartition(int *arr, int left, int right) {
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

void quickSort(int *arr, int left, int right) {
  int offset = doPartition(arr, left, right);
	if (left < offset - 1)
		quickSort(arr, left, offset - 1);
	if (offset < right)
		quickSort(arr, offset, right);
}

void parallelQuickSort(int *arr, int left, int right, int prRank, int nProcessors, int rankIndex) {
  int sharePr = prRank + (int) pow(2, rankIndex);
  rankIndex++;

  if (sharePr >= nProcessors) { // If no process to share sequentially
		quickSort(arr, left, right);
		return;
	}
	int offset = doPartition(arr, left, right);

  if (offset > right + 1 - offset) {
    int partition[right + 1 - offset];
    std::copy(arr + offset, arr + right + 1, partition);

		MPI_Send(&partition, right - offset + 1, MPI_INT, sharePr, offset, MPI_COMM_WORLD);
		parallelQuickSort(arr, left, offset - 1, prRank, nProcessors, rankIndex);
		MPI_Recv(&partition, right - offset + 1, MPI_INT, sharePr, offset, MPI_COMM_WORLD, &status);

    std::copy(partition, partition + right + 1 - offset, arr + offset);
	} else {
    int partition[offset];
    std::copy(arr, arr + offset, partition);

		MPI_Send(&partition, offset, MPI_INT, sharePr, offset, MPI_COMM_WORLD);
		parallelQuickSort(arr, offset, right, prRank, nProcessors, rankIndex);
		MPI_Recv(&partition, offset, MPI_INT, sharePr, offset, MPI_COMM_WORLD, &status);

    std::copy(partition, partition + offset, arr);
	}
}

int *generateRandomArray(int n, int max) {
  int *arr = new int[n];
  for (int i = 0; i < n; i++) {
		arr[i] = rand() % max;
	}
	return arr;
}

int main(int argc, char *argv[]) {
  srand(time(NULL));
	MPI_Init(&argc, &argv);

  int max,n;
	n = std::stoi(argv[1]);
	max = std::stoi(argv[2]);

  int nProcessors, myself;

	MPI_Comm_rank(MPI_COMM_WORLD, &myself);
	MPI_Comm_size(MPI_COMM_WORLD, &nProcessors);

  int *globalData;

  if (myself == ROOT) {
    globalData = generateRandomArray(n, max);
    double startTime = MPI_Wtime();
    parallelQuickSort(globalData, 0, n - 1, myself, nProcessors, 0);
    double endTime = MPI_Wtime();

    printf("From ROOT - Total Time: %.2f\n", (endTime - startTime));
  } else {
    int index_count = 0;
    while (pow(2, index_count) <= myself) {
      index_count++;
    }

    MPI_Probe(MPI_ANY_SOURCE, MPI_ANY_TAG, MPI_COMM_WORLD, &status);
		int localSize;
		MPI_Get_count(&status, MPI_INT, &localSize);
    int localData[localSize];
    MPI_Recv(&localData, localSize, MPI_INT, MPI_ANY_SOURCE, MPI_ANY_TAG, MPI_COMM_WORLD, &status);
    parallelQuickSort(localData, 0, localSize - 1, myself, nProcessors, index_count);
    MPI_Send(&localData, localSize, MPI_INT, status.MPI_SOURCE, status.MPI_TAG, MPI_COMM_WORLD);
  }

  MPI_Finalize();
  return 0;
}
