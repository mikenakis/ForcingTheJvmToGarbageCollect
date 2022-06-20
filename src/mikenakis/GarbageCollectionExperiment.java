package mikenakis;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.Cleaner;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

//It looks like there is no way to force garbage collection unless we invoke the jvmti ForceGarbageCollection function, but this involves writing native code:
//See https://stackoverflow.com/a/32718506/773113
public final class GarbageCollectionExperiment
{
	private static final Cleaner cleaner = Cleaner.create(); //PEARL: this must be static, or there will be a huge performance penalty! See https://stackoverflow.com/q/46697432/773113

	private GarbageCollectionExperiment()
	{
	}

	public static void main( String[] args )
	{
		System.out.println( "directory: " + System.getProperty( "user.dir" ) );
		GarbageCollectionExperiment instance = new GarbageCollectionExperiment();
		instance.run();
	}

	private static final long interval = 700;

	private final AtomicLong currentAllocationCount = new AtomicLong();
	private long lastAllocationCount = 0;
	private long nextTime = System.currentTimeMillis() + interval;
	@SuppressWarnings( "MismatchedReadAndWriteOfArray" ) private final Object[] objects = new Object[10000];

	private void run()
	{
		GarbageCollector garbageCollector = new GarbageCollector19();

		for( int i = 0; i < objects.length; i++ )
			objects[i] = allocate();

		//noinspection InfiniteLoopStatement
		for( int i = 0; ; i = ++i % objects.length )
		{
			Thread.yield();
			objects[i] = allocate();
			long time = System.currentTimeMillis();
			if( time >= nextTime )
			{
				nextTime = time + interval;
				doGarbageCollection( garbageCollector );
			}
		}
	}

	private Object allocate()
	{
		Object object = new byte[1024];
		cleaner.register( object, () -> currentAllocationCount.decrementAndGet() );
		currentAllocationCount.incrementAndGet();
		return object;
	}

	private void doGarbageCollection( GarbageCollector garbageCollector )
	{
		System.out.print( "gc()... " );
		double seconds = garbageCollector.timeGarbageCollection();
		long newAllocationCount = currentAllocationCount.get();
		long delta = newAllocationCount - lastAllocationCount;
		lastAllocationCount = newAllocationCount;
		System.out.printf( "%5.3f seconds; allocations: %d (%+d)%n", seconds, newAllocationCount, delta );
	}

	private abstract static class GarbageCollector
	{
		double timeGarbageCollection()
		{
			long startTime = System.currentTimeMillis();
			run();
			long endTime = System.currentTimeMillis();
			return (endTime - startTime) / 1000.0;
		}

		protected abstract void run();
	}

	private static final class GarbageCollector1 extends GarbageCollector
	{
		@Override protected void run()
		{
		}
	}

	private static final class GarbageCollector2 extends GarbageCollector
	{
		@Override protected void run()
		{
			Thread.yield();
		}
	}

	private static final class GarbageCollector3 extends GarbageCollector
	{
		@Override protected void run()
		{
			sleep( 30 );
		}
	}

	private static final class GarbageCollector4 extends GarbageCollector
	{
		@Override protected void run()
		{
			System.gc();
			Runtime.getRuntime().runFinalization();
		}
	}

	private static final class GarbageCollector5 extends GarbageCollector
	{
		@Override protected void run()
		{
			System.gc();
			Runtime.getRuntime().runFinalization();
			Thread.yield();
		}
	}

	private static final class GarbageCollector6 extends GarbageCollector
	{
		@Override protected void run()
		{
			System.gc();
			Runtime.getRuntime().runFinalization();
			sleep( 30 );
		}
	}

	private static final class GarbageCollector7 extends GarbageCollector
	{
		private static WeakReference<Object> allocateWeakReference()
		{
			return new WeakReference<>( new Object() );
		}

		@Override protected void run()
		{
			WeakReference<Object> ref = allocateWeakReference();
			Runtime.getRuntime().gc();
			Runtime.getRuntime().runFinalization();
			for( ; ; )
			{
				Thread.yield();
				if( ref.get() == null )
					break;
			}
		}
	}

	private static final class GarbageCollector8 extends GarbageCollector
	{
		private static WeakReference<Object> allocateWeakReference()
		{
			return new WeakReference<>( new Object() );
		}

		@Override protected void run()
		{
			WeakReference<Object> ref = allocateWeakReference();
			Runtime.getRuntime().gc();
			Runtime.getRuntime().runFinalization();
			for( ; ; )
			{
				sleep( 30 );
				if( ref.get() == null )
					break;
			}
		}
	}

	private static final class GarbageCollector9 extends GarbageCollector
	{
		private final AtomicReference<Object> oldObjectRef = new AtomicReference<>( new Object() );

		private WeakReference<Object> allocateWeakReference()
		{
			Object oldObject = oldObjectRef.getAndSet( new Object() );
			return new WeakReference<>( oldObject );
		}

		@Override protected void run()
		{
			WeakReference<Object> ref = allocateWeakReference();
			Runtime.getRuntime().gc();
			Runtime.getRuntime().runFinalization();
			for( ; ; )
			{
				Thread.yield();
				if( ref.get() == null )
					break;
			}
		}
	}

	private static final class GarbageCollector10 extends GarbageCollector
	{
		private final AtomicReference<Object> oldObjectRef = new AtomicReference<>( new Object() );

		private WeakReference<Object> allocateWeakReference()
		{
			Object oldObject = oldObjectRef.getAndSet( new Object() );
			return new WeakReference<>( oldObject );
		}

		@Override protected void run()
		{
			WeakReference<Object> ref = allocateWeakReference();
			Runtime.getRuntime().gc();
			Runtime.getRuntime().runFinalization();
			for( ; ; )
			{
				sleep( 30 );
				if( ref.get() == null )
					break;
			}
		}
	}

	private static final class GarbageCollector11 extends GarbageCollector
	{
		private final AtomicReference<Object> oldObjectRef = new AtomicReference<>( new Object() );

		private Object allocateObject0()
		{
			return oldObjectRef.getAndSet( new Object() );
		}

		private void allocateObject( Runnable onClean )
		{
			Object object = allocateObject0();
			cleaner.register( object, onClean );
		}

		@Override protected void run()
		{
			int n = 10;
			CountDownLatch countDownLatch = new CountDownLatch( n );
			for( int i = 0; i < n; i++ )
				allocateObject( () -> countDownLatch.countDown() );
			Runtime.getRuntime().gc();
			Runtime.getRuntime().runFinalization();
			try
			{
				countDownLatch.await();
			}
			catch( InterruptedException e )
			{
				throw new RuntimeException( e );
			}
		}
	}

	private static final class GarbageCollector12 extends GarbageCollector
	{
		private final ConcurrentLinkedQueue<Object> queue = prepareQueue();

		private static ConcurrentLinkedQueue<Object> prepareQueue()
		{
			ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<>();
			for( int i = 0; i < 1000; i++ )
				queue.add( new Object() );
			return queue;
		}

		private Object allocateObject0()
		{
			queue.add( new Object() );
			return queue.poll();
		}

		private void allocateObject( Runnable onClean )
		{
			Object object = allocateObject0();
			cleaner.register( object, onClean );
		}

		@Override protected void run()
		{
			int n = 100;
			CountDownLatch countDownLatch = new CountDownLatch( n );
			for( int i = 0; i < n; i++ )
				allocateObject( () -> countDownLatch.countDown() );
			Runtime.getRuntime().gc();
			Runtime.getRuntime().runFinalization();
			try
			{
				countDownLatch.await();
			}
			catch( InterruptedException e )
			{
				throw new RuntimeException( e );
			}
		}
	}

	private static final class GarbageCollector13 extends GarbageCollector
	{
		private static WeakReference<Object> allocateWeakReference()
		{
			return new WeakReference<>( new Object() );
		}

		@Override protected void run()
		{
			WeakReference<Object> weakReference = allocateWeakReference();
			Runtime.getRuntime().gc();
			Runtime.getRuntime().runFinalization();
			for( ; ; )
			{
				Thread.yield();
				if( weakReference.get() == null )
					break;
			}
			long freeMemory = Runtime.getRuntime().freeMemory();
			for( ; ; )
			{
				sleep( 30 );
				long newFreeMemory = Runtime.getRuntime().freeMemory();
				if( newFreeMemory == freeMemory )
					break;
				freeMemory = newFreeMemory;
			}
		}
	}

	private static final class GarbageCollector14 extends GarbageCollector
	{
		private static WeakReference<Object> allocateWeakReference()
		{
			return new WeakReference<>( new Object() );
		}

		@Override protected void run()
		{
			WeakReference<Object> weakReference = allocateWeakReference();
			Runtime.getRuntime().gc();
			Runtime.getRuntime().runFinalization();
			for( ; ; )
			{
				Thread.yield();
				if( weakReference.get() == null )
					break;
			}
			long freeMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
			for( ; ; )
			{
				sleep( 30 );
				long newFreeMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
				if( newFreeMemory == freeMemory )
					break;
				freeMemory = newFreeMemory;
			}
		}
	}

	private static final class GarbageCollector15 extends GarbageCollector
	{
		@Override protected void run()
		{
			String pid = ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0];
			try
			{
				Runtime.getRuntime().exec( String.format( "jmap -histo:live,file=/dev/nul %s", pid ) ).waitFor();
			}
			catch( IOException | InterruptedException e )
			{
				throw new RuntimeException( e );
			}
		}
	}

	private static final class GarbageCollector16 extends GarbageCollector
	{
		@Override protected void run()
		{
			String pid = ManagementFactory.getRuntimeMXBean().getName().split( "@" )[0];
			try
			{
				Runtime.getRuntime().exec( String.format( "jcmd %s GC.run", pid ) ).waitFor();
			}
			catch( IOException | InterruptedException e )
			{
				throw new RuntimeException( e );
			}
		}
	}

	private static final class GarbageCollector17 extends GarbageCollector
	{
		@Override protected void run()
		{
			Garbager.garbageMemory();
		}
	}

	private static final class GarbageCollector18 extends GarbageCollector
	{
		@Override protected void run()
		{
			long freeMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
			for( ; ; )
			{
				Garbager.garbageMemory();
				long newFreeMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
				if( newFreeMemory == freeMemory )
					break;
				freeMemory = newFreeMemory;
				sleep( 10 );
			}
		}
	}

	private static final class GarbageCollector19 extends GarbageCollector
	{
		@Override protected void run()
		{
			long freeMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
			for( ; ; )
			{
				Runtime.getRuntime().gc();
				Runtime.getRuntime().runFinalization();
				long newFreeMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
				if( newFreeMemory == freeMemory )
					break;
				freeMemory = newFreeMemory;
				sleep( 10 );
			}
		}
	}

	private static void sleep( int milliseconds )
	{
		try
		{
			Thread.sleep( milliseconds );
		}
		catch( InterruptedException e )
		{
			throw new RuntimeException( e );
		}
	}
}
