package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	private Lock lock;
    private Condition speaker;
    private Condition listener;
    private boolean toggle;
    private int message;
    private boolean done;
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	lock = new Lock();
    	speaker = new Condition(lock);
    	listener = new Condition(lock);
    	toggle = false;
    	message = 0;

    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	lock.acquire();
    	
    	while (toggle){
    	    speaker.sleep();
    	}
    	toggle=true;
    	message=word;
    	listener.wake();
    	lock.release();
    	
    	
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	lock.acquire();
    	while(!toggle){
    	    listener.sleep();
    	}
    	toggle=false;
    	int word = message;
    	speaker.wake();	
    	System.out.println("Received Message:" + word);
    	lock.release();
    	return word;
    }  
    
 // Place this function inside Communicator. And make sure Communicator.selfTest() is called inside ThreadedKernel.selfTest() method.

    public static void selfTest(){
        Communicator com = new Communicator();
        long times[] = new long[4];
        int words[] = new int[2];
        KThread speaker1 = new KThread( new Runnable () {
            public void run() {
                com.speak(4);
                times[0] = Machine.timer().getTime();
                System.out.println("times[0]:" + times[0]);
            }
        });
        speaker1.setName("S1");
        KThread speaker2 = new KThread( new Runnable () {
            public void run() {
                com.speak(7);
                times[1] = Machine.timer().getTime();
            }
        });
        speaker1.setName("S2");
        KThread listener1 = new KThread( new Runnable () {
            public void run() {
                words[0] = com.listen();
                System.out.println("Word[0]:" + words[0]);
                times[2] = Machine.timer().getTime();
                System.out.println("times[2]:" + times[2]);

            }
        });
        listener1.setName("L1");
        KThread listener2 = new KThread( new Runnable () {
            public void run() {
                words[1] = com.listen();
                System.out.println("Word[1]:" + words[1]);
                times[3] = Machine.timer().getTime();
            }
        });
        listener2.setName("L2");
        speaker1.fork(); speaker2.fork(); listener1.fork(); listener2.fork();
        speaker1.join(); speaker2.join(); listener1.join(); listener2.join();
        Lib.assertTrue(words[0] == 4, "Didn't listen back spoken word."); 
        Lib.assertTrue(words[1] == 7, "Didn't listen back spoken word.");
        Lib.assertTrue(times[0] < times[2], "speak returned before listen.");
        Lib.assertTrue(times[1] < times[3], "speak returned before listen.");
    }
    
}
