// Liam Wynn, 1/26/2021, CS515p: Assignment 1

/*
  This is the Java version of prodcons.cpp.
*/

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

class ProdCons3 {
  public static final int BUFSIZE = 20;
  public static final int NUMITEMS = 100;
  public static final int TERMINATE = -1;

  public static final Lock queueLock = new ReentrantLock();
  public static final Condition queueNotFull  = queueLock.newCondition();
  public static final Condition queueNotEmpty = queueLock.newCondition();

  public static final Object consumerStatsLck = new Object();

  public static int numProducers;
  public static int itemsPerProducer;
  public static int itemsRemainder;

  // A value each producer will increment when they
  // finish. Used to identify the last producer, who
  // will add the TERMINATE values to the queue.
  public static int producerFinishedCount;
  public static final Object prodFinLck = new Object();

  public static void main(String[] args) {
    int numConsumers = 1;

    Producer[] producerThreads;
    Consumer[] consumerThreads;

    Queue<Integer> queue = new LinkedList<>();
    int[] consumerStats;

    try {
      if(args.length >= 2) {
        numConsumers = Integer.parseInt(args[0]);
        numProducers = Integer.parseInt(args[1]);
      } else if(args.length == 1)
        numConsumers = Integer.parseInt(args[0]);
    } catch(Exception e) {
      numConsumers = 1;
      numProducers = 1;
    }

    numConsumers = numConsumers <= 0 ? 1 : numConsumers;
    numProducers = numProducers <= 0 ? 1 : numProducers;

    itemsPerProducer = NUMITEMS / numProducers;
    itemsRemainder = NUMITEMS - (itemsPerProducer * numProducers);

    producerThreads = new Producer[numProducers];
    for(int i = 0; i < numProducers; i++) {
      if(i == numProducers - 1)
        producerThreads[i] = new Producer(i, numConsumers, queue);
      else
        producerThreads[i] = new Producer(i, numConsumers, queue);
    }

    consumerThreads = new Consumer[numConsumers];
    consumerStats = new int[numConsumers];
    for(int i = 0; i < numConsumers; i++) {
      consumerThreads[i] = new Consumer(i, queue, consumerStats);
    }

    try {
      for(int i = 0; i < numProducers; i++)
        producerThreads[i].start();
      for(int i = 0; i < numConsumers; i++)
        consumerThreads[i].start();

      for(int i = 0; i < numProducers; i++)
        producerThreads[i].join();
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
  int id;
  // Need this to know how many TERMINATES to add to the queue.
  private int numConsumers;
  private Queue<Integer> queue;

  public Producer(int id, int numConsumers, Queue<Integer> queue) {
    this.id = id;
    this.numConsumers = numConsumers;
    this.queue = queue;
  }

  public void run() {
    int start = id * ProdCons3.itemsPerProducer + 1;
    int end = start + ProdCons3.itemsPerProducer - 1;

    if(id == ProdCons3.numProducers - 1)
      end += ProdCons3.itemsRemainder;

    System.out.println("producer[" + id + "] for segment [" + start + ", " + end + "] is running");

    for(int i = start; i <= end; i++) {
      ProdCons3.queueLock.lock();

      try {
		while(queue.size() == ProdCons3.BUFSIZE)
          ProdCons3.queueNotFull.await();

        queue.add(i);
        System.out.println("producer[" + id + "] added " + i + "(qsz: " + queue.size() + ")");

        ProdCons3.queueNotEmpty.signal();

      } catch(Exception e) {
        System.out.println(e.getMessage());
      } finally {
        ProdCons3.queueLock.unlock();
      }
    }

    System.out.println("producer[" + id + "] ending");
    synchronized(ProdCons3.prodFinLck) {
      ProdCons3.producerFinishedCount++;

      if(ProdCons3.producerFinishedCount == ProdCons3.numProducers) {
        for(int i = 0; i <= numConsumers; i++) {
          ProdCons3.queueLock.lock();

          try {
	    	while(queue.size() == ProdCons3.BUFSIZE)
              ProdCons3.queueNotFull.await();

            queue.add(-1);
            ProdCons3.queueNotEmpty.signal();

          } catch(Exception e) {
            System.out.println(e.getMessage());
          } finally {
            ProdCons3.queueLock.unlock();
          }
        }
      }
    }
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
    for(int i = 1; i < ProdCons3.NUMITEMS; i++) {
      ProdCons3.queueLock.lock();

      try {
        while(queue.size() == 0)
          ProdCons3.queueNotEmpty.await();

        int p = queue.remove();
        if(p == ProdCons3.TERMINATE) {
          ProdCons3.queueNotFull.signal();
          ProdCons3.queueLock.unlock();
          break;
        } else {
          count++;
          System.out.println("consumer[" + id + "] rem'd " + p + " (qsz: " + queue.size() + ")");
          ProdCons3.queueNotFull.signal();
        }
      } catch(Exception e) {
        ProdCons3.queueLock.unlock();
        System.out.println(e.getMessage());
      }

      ProdCons3.queueLock.unlock();
    }

    System.out.println("consumer[" + id + "] ending");

    synchronized(ProdCons3.consumerStatsLck) {
      stats[id] = count;
    }
  }
}

