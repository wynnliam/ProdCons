// Liam Wynn, 1/20/2021, CS515p: Assignment 1

/*
  Implements a very simple producer-consumer model
  in Java. This will only include one producer and
  one consumer.
*/

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

class ProdCons1 {
  public static final int BUFSIZE = 20;
  public static final int NUMITEMS = 100;

  public static final Lock queueLock = new ReentrantLock();
  public static final Condition queueEmpty = queueLock.newCondition();
  public static final Condition queueNotEmpty = queueLock.newCondition();

  public static void main(String[] args) {
    Queue<Integer> queue = new LinkedList<>();

    Producer producerThread = new Producer(queue);
    Consumer consumerThread = new Consumer(queue);

    try {
      producerThread.start();
      consumerThread.start();

      producerThread.join();
      consumerThread.join();
    } catch(Exception e) {
      System.out.println("Error executing threads");
    }
  }
}

class Producer extends java.lang.Thread {
  private Queue<Integer> queue;

  public Producer(Queue<Integer> queue) {
    this.queue = queue;
  }

  public void run() {
    for(int i = 1; i <= ProdCons1.NUMITEMS; i++) {
      ProdCons1.queueLock.lock();

      try {
        while(queue.size() > 0)
          ProdCons1.queueEmpty.await();

        queue.add(i);
        System.out.println("Added " + i);

        ProdCons1.queueNotEmpty.signal();

      } catch(Exception e) { System.out.println(e.getMessage()); }
      finally {
        ProdCons1.queueLock.unlock();
      }
    }
  }
}

class Consumer extends java.lang.Thread {
  private Queue<Integer> queue;

  public Consumer(Queue<Integer> queue) {
    this.queue = queue;
  }

  public void run() {
    for(int i = 1; i <= ProdCons1.NUMITEMS; i++) {
      ProdCons1.queueLock.lock();

      try {
        while(queue.size() == 0)
          ProdCons1.queueNotEmpty.await();

        int p = queue.remove();
        System.out.println("Rem'd " + p);

        if(queue.size() == 0)
          ProdCons1.queueEmpty.signal();

      } catch(Exception e) { System.out.println(e.getMessage()); }
      finally {
        ProdCons1.queueLock.unlock();
      }
    }
  }
}
