package mikenakis;

// from https://stackoverflow.com/a/32718506/773113
// Requires -agentlib:D:\Personal\ForceGarbageCollection\ForceGarbageCollection
// Might get better results by using other heap-related methods, such as https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#IterateThroughHeap
public class Garbager
{
	static void garbageMemory()
	{
		int jvmtiError = forceGarbageCollection();
		assert jvmtiError == 0;
	}

	private static native int forceGarbageCollection();
}
