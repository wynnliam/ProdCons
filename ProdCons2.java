// Liam Wynn, 1/26/2021, CS515p: Assignment 1

/*
  In this program, I implement a Producer-Consumer
  model with multiple consumers. This number of which
  is supplied by the user (default value is 1).

  An important note: Since multiple consumers will
  seek to gather resources, I have to change the
  logic from ProdCons1. In ProdCons1, the producer will
  only add resources if the buffer is empty. So it will
  only add one thing. Now, since there is one consumer,
  this is sufficient. The consumer will take the resource,
  see that the buffer is empty, and notify the producer.

  Since there are multiple consumers, adding only one item
  would be insufficient. So instead, the producer will
  add until the buffer is full
*/

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

class ProdCons2 {
  public static final int BUFSIZE = 20;
  public static final int NUMITEMS = 100;
  public static final int TERMINATE = -1;

  public static final Lock queueLock = new ReentrantLock();
  public static final Condition queueNotFull  = queueLock.newCondition();
  public static final Condition queueNotEmpty = queueLock.newCondition();

  public static final Object consumerStatsLck = new Object();

  public static void main(String[] args) {
    int numConsumers = 1;

    Producer producerThread;
    Consumer[] consumerThreads;

    Queue<Integer> queue = new LinkedList<>();
    int[] consumerStats;

    try {
      if(args.length > 0)
        numConsumers = Integer.parseInt(args[0]);
    } catch(Exception e) {
      numConsumers = 1;
    }

    producerThread = new Producer(numConsumers, queue);
    consumerThreads = new Consumer[numConsumers];
    consumerStats = new int[numConsumers];
    for(int i = 0; i < numConsumers; i++) {
      consumerThreads[i] = new Consumer(i, queue, consumerStats);
    }

    try {
      producerThread.start();
      for(int i = 0; i < numConsumers; i++)
        consumerThreads[i].start();

      producerThread.join();
      for(int i = 0; i < numConsumers; i++) {
        consumerThreads[i].join();
      }

      int total = 0;
      System.out.print("consumer stats: [");
      for(int i = 0; i < numConsumers; i++) {
         total += consumerStats[i];
         System.out.print(consumerStats[i] + ",");
      }
      System.out.println("], total = " + total);
  
    } catch(Exception e) {
      System.out.println("Error running threads");
    }
  }
}

class Producer extends java.lang.Thread {
  // Need this to know how many TERMINATES to add to the queue.
  private int numConsumers;
  private Queue<Integer> queue;

  public Producer(int numConsumers, Queue<Integer> queue) {
    this.numConsumers = numConsumers;
    this.queue = queue;
  }

  public void run() {
    // When i > NUMITEMS, we put TERMINATE on the queue
    // numConsumers number of times to kill the consumer threads.
    for(int i = 1; i <= ProdCons2.NUMITEMS + numConsumers; i++) {
      ProdCons2.queueLock.lock();

      try {
		while(queue.size() == ProdCons2.BUFSIZE)
          ProdCons2.queueNotFull.await();

        // We are done adding items - we must now
        // notify a consumer to end
        if(i > ProdCons2.NUMITEMS)
          queue.add(ProdCons2.TERMINATE);
        else {
          queue.add(i);
          System.out.println("producer: added " + i);
        }

        ProdCons2.queueNotEmpty.signal();

      } catch(Exception e) {
        System.out.println(e.getMessage());
      } finally {
        ProdCons2.queueLock.unlock();
      }
    }

    System.out.println("producer ending");
  }
}

class Consumer extends java.lang.Thread {
  private int id;
  private int count;

  private Queue<Integer> queue;
  private int[] stats;

  public Consumer(int id, Queue<Integer> queue, int[] stats) {
    this.id = id;
    this.count = 0;

    this.queue = queue;
    this.stats = stats;
  }

  public void run() {
    for(int i = 1; i < ProdCons2.NUMITEMS; i++) {
      ProdCons2.queueLock.lock();

      try {
        while(queue.size() == 0)
          ProdCons2.queueNotEmpty.await();

        int p = queue.remove();
        if(p == ProdCons2.TERMINATE) {
          ProdCons2.queueNotFull.signal();
          ProdCons2.queueLock.unlock();
          break;
        } else {
          count++;
          System.out.println("consumer[" + id + "] rem'd " + p + " (qsz: " + queue.size() + ")");
          ProdCons2.queueNotFull.signal();
        }
      } catch(Exception e) {
        ProdCons2.queueLock.unlock();
        System.out.println(e.getMessage());
      }

      ProdCons2.queueLock.unlock();
    }

    System.out.println("consumer[" + id + "] ending");

    synchronized(ProdCons2.consumerStatsLck) {
      stats[id] = count;
    }
  }
}
