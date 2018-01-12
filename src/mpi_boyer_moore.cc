#include <mpi.h>
#include <time.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include <iostream>
#include <algorithm>

const int ROOT = 0;
const int R = 256;

const int COMPUTE_TAG = 0;
const int RIGHT_TAG = 1;

MPI_Status status;



class MpiBoyerMoore {
public:
	int right[R];
	std::string pat;
	MpiBoyerMoore(std::string pat, int nProcessors) {
		this->pat = pat;

		for (int c = 0; c < R; c++)
			right[c] = -1;
		for (int j = 0; j < pat.length(); j++)
			right[pat[j]] = j;
		for (int i = 1; i < nProcessors; i++) {
			MPI_Send(&right, R, MPI_INT, i, RIGHT_TAG, MPI_COMM_WORLD);
		}
	}
	int search(std::string txt) {
		int m = pat.length();
		int n = txt.length();
		int skip;
		for (int i = 0; i <= n - m; i += skip) {
			skip = 0;
			for (int j = m - 1; j >= 0; j--) {
				if (pat[j] != txt[i + j]) {
					skip = std::max(1, j - right[txt[i + j]]);
					break;
				}
			}
			if (skip == 0)
				return i; // found
		}
		return -1;
	}
	static int search(std::string txt, std::string pat, int right[R]) {
		int m = pat.length();
		int n = txt.length();
		int skip;
		for (int i = 0; i <= n - m; i += skip) {
			skip = 0;
			for (int j = m - 1; j >= 0; j--) {
				if (pat[j] != txt[i + j]) {
					skip = std::max(1, j - right[txt[i + j]]);
					break;
				}
			}
			if (skip == 0)
				return i; // found
		}
		return -1; // not found
	}
};

std::string generateRandomString(int len) {
	std::string chars = "abcdefghijklmnopqrstuvwxyz";
	std::string nums = "0123456789";
	std::string symbols = "   ";
	std::string passSymbols = chars + nums + symbols;

	std::string result = "";
	for (int i = 0; i < len; i++) {
		result += passSymbols[rand() % passSymbols.length()];
	}
	return result;
}

std::string generateRandomSubstring(std::string txt, int len) {
	int index = rand() % (txt.length() - len);
	return txt.substr(index, len);
}

int main(int argc, char* argv[]) {
	srand(time(NULL));
	MPI_Init(&argc, &argv);

	int m,n;
	n = std::stoi(argv[1]);
	m = std::stoi(argv[2]);

	int nProcessors, myself;

	MPI_Comm_rank(MPI_COMM_WORLD, &myself);
	MPI_Comm_size(MPI_COMM_WORLD, &nProcessors);

	if (myself == ROOT) {
		std::string txt = generateRandomString(n);
		std::string pat = generateRandomSubstring(txt, m);

		int localSize = n / nProcessors;

		double startTime = MPI_Wtime();
		MpiBoyerMoore boyermoore(pat, nProcessors);

		for (int i = 1; i < nProcessors; i++) {
				int start = std::max(i * localSize - m, 0);
				int end = (i + 1) * localSize;
				if (i == nProcessors - 1) {
					end += n % nProcessors;
				}
				MPI_Send((pat + txt.substr(start, end-start)).c_str(), end - start + m, MPI_CHAR, i,
						COMPUTE_TAG, MPI_COMM_WORLD);
			}

			int offset = boyermoore.search(txt.substr(0, localSize));
			if (offset == -1) {
				for (int i = 1; i < nProcessors; i++) {
					int temp;
					MPI_Recv(&temp, 1, MPI_INT, i, COMPUTE_TAG, MPI_COMM_WORLD, &status);
					if (temp != -1) {
						offset = temp;
						break;
					}
				}
			}
			double endTime = MPI_Wtime();

			if (offset == -1) {
				std::cout << "Pattern mismatched " << pat;
			} else {
				printf("From ROOT - Total Time: %.2f\n", (endTime - startTime));
			}
	} else {
		int right[R];
		MPI_Recv(&right, R, MPI_INT, ROOT, RIGHT_TAG, MPI_COMM_WORLD, &status);
		MPI_Probe(ROOT, COMPUTE_TAG, MPI_COMM_WORLD, &status);
		int bufferSize;
		MPI_Get_count(&status, MPI_CHAR, &bufferSize);
		char buffer[bufferSize];
		MPI_Recv(&buffer, bufferSize, MPI_CHAR, ROOT, COMPUTE_TAG, MPI_COMM_WORLD, &status);

		std::string temp(buffer);

		int offset = MpiBoyerMoore::search(temp.substr(m, temp.length()-m), temp.substr(0,m), right);

		int localSize = n / nProcessors;
		int start = std::max(myself * localSize - m, 0);
		if (offset == -1) {
			MPI_Send(&offset, 1, MPI_INT, ROOT, COMPUTE_TAG, MPI_COMM_WORLD);
		} else {
			offset += start;
			MPI_Send(&offset, 1, MPI_INT, ROOT, COMPUTE_TAG, MPI_COMM_WORLD);
		}


	}
	MPI_Finalize();
}
