import mpi.MPI;
import mpi.MPIException;

public class MpiHelloWorld {

	public static void main(String[] args) throws MPIException {
		// Initialize the MPI environment
		MPI.Init(args);

		int worldSize = MPI.COMM_WORLD.getSize(); // Get the number of processes
		int worldRank = MPI.COMM_WORLD.getRank(); // Get the rank of the process
		String processorName = MPI.getProcessorName(); // Get the name of the
														// processor

		// Print off a hello world message
		System.out.printf("Hello world from processor %s, rank %d out of %d processors\n", processorName, worldRank,
				worldSize);

		// Finalize the MPI environment.
		MPI.Finalize();
	}
}
