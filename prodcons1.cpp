// Liam Wynn, 1/19/2021, CS515p: Parallel Programming

/*
  In this assignment, I implement a very simple
  producer-consumer model. It will only have one
  producer and one consumer.
*/

#include <iostream>
#include <thread>
#include <mutex>
#include <condition_variable>
#include "./queue.h"

using namespace std;

// The queue's capacity.
const int BUFSIZE = 20;
// We add all values numbers from 1
// to this value into the queue.
const int NUMITEMS = 100;

Queue queue(BUFSIZE);
mutex mtx_queue_state;
condition_variable queue_not_empty, queue_empty;

// Mutex for controlling output. cout is not atomic!
mutex mtx_output;

void producer() {
  mtx_output.lock();
  cout << "Producer starting on core " << sched_getcpu() << endl;
  mtx_output.unlock();

  for(int i = 1; i <= NUMITEMS; i++) {
    unique_lock<mutex> queue_lck(mtx_queue_state);
    while(!queue.isEmpty())
      queue_empty.wait(queue_lck);

    if(!queue.isFull()) {
      queue.add(i);
      cout << "Producer added " << i << " (qsz: " << queue.size() << ")" << endl;
    }

    queue_not_empty.notify_one();
    queue_lck.unlock();
  }

  mtx_output.lock();
  cout << "Producer ending" << endl;
  mtx_output.unlock();
}

void consumer() {
  mtx_output.lock();
  cout << "Consumer starting on core " << sched_getcpu() << endl;
  mtx_output.unlock();

  for(int i = 1; i <= NUMITEMS; i++) {
    unique_lock<mutex> queue_lck(mtx_queue_state);

    while(queue.isEmpty())
      queue_not_empty.wait(queue_lck);

    int item = queue.remove();
    cout << "Consumer rem'd " << item << " (qsz: " << queue.size() << ")" << endl;

    if(queue.isEmpty())
      queue_empty.notify_one();

    queue_lck.unlock();
  }

  mtx_output.lock();
  cout << "Consumer ending" << endl;
  mtx_output.unlock();
}

int main() {
  thread producer_thread(producer);
  thread consumer_thread(consumer);

  producer_thread.join();
  consumer_thread.join();

  cout << "main: all done!" << endl;
  return 0;
}
