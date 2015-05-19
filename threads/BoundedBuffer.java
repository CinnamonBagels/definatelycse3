package nachos.threads;

public class BoundedBuffer {

	private char[] buffer;
	private int count, head, tail, capacity;
	private Lock lock;
	private Condition2 notEmpty, notFull;
	
	public BoundedBuffer(int size) {
		capacity = size;
		buffer = new char[capacity];
		count = 0;
		head = 0;
		tail = 0;
		lock = new Lock();
		notEmpty = new Condition2(lock);
		notFull = new Condition2(lock);
	}
	
	public void put(char c) {
		lock.acquire();
		// can't put while buffer is full, 
		// so sleep until the buffer is "not full"
		while(count == capacity) { 
			notFull.sleep();
		}
		count++;
		buffer[head] = c;
		head++;
		if (head == capacity){
			head = 0;
		}
		notEmpty.wake(); // wake up a thread waiting for not-empty buffer
		lock.release();
	}
	
	public char get(){
		lock.acquire();
		// can't get while buffer is empty,
		// so sleep until buffer is  "not empty"
		while (count == 0) {
			notEmpty.sleep();
		}
		count--;
		char c = buffer[tail];
		tail++;
		if (tail == capacity) {
			tail = 0;
		}
		notFull.wake(); // wake up a thread waiting for not-full buffer
		lock.release();
		return c;
	}
	
	
	 public static void selfTest() {
	    	BoundedBuffer b = new BoundedBuffer(2);
	    	KThread thread3 = new KThread(new Runnable(){
	    		public void run(){
				
						b.put('a');
						b.get();
					
			
				}
	    	});
	    	
	    	thread3.fork();
	    	thread3.join();
	    	KThread thread2 = new KThread(new Runnable(){
	    		public void run(){
				
						b.put('a');
						b.get();
					
			
				}
	    	});
	    	
	    	thread2.fork();
	    	thread2.join();
	    	KThread thread1 = new KThread(new Runnable(){
				public void run(){
					
						b.put('a');
						b.get();
				}
			});	
	    	
	    	thread1.fork();
	    	thread1.join();
	    	System.out.println("boundedtest TEST #1: End");
	    	
	    }
	
	
}