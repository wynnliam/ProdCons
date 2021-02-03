// Liam Wynn, 1/19/2021, CS515p: Parallel Programming

/*
  In this assignment, I implement a producer-consumer
  model with one producer and multiple consumers. In the
  previous version, the producer would add one item
  and then wait, since that meant the buffer was not empty.
  I will modify it so the producer will add items until
  the buffer is full, in which case he will then wait
  until it is empty.
*/

#include <iostream>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <vector>
#include "./queue.h"

using namespace std;

// The queue's capacity.
const int BUFSIZE = 20;
// We add all values numbers from 1
// to this value into the queue.
const int NUMITEMS = 100;
// When read, it tells a consumer to stop
// consuming.
const int TERMINATE_VAL = -1;

// Make global since producer and
// main thread handle this.
int num_consumers;
// Keeps track of each consumer thread's items they
// consumed.
vector<int> consumed_count;

Queue queue(BUFSIZE);
mutex mtx_queue_state;
condition_variable queue_not_empty, queue_not_full;

// Mutex for controlling output. cout is not atomic!
mutex mtx_output;

void producer() {
  mtx_output.lock();
  cout << "Producer starting on core " << sched_getcpu() << endl;
  mtx_output.unlock();

  // num_consumers so we can add the termination
  // value (-1) to the queue as well as regular items
  // in one go.
  for(int i = 1; i <= NUMITEMS + num_consumers; i++) {
    unique_lock<mutex> queue_lck(mtx_queue_state);
    while(queue.size() == BUFSIZE)
      queue_not_full.wait(queue_lck);

    // Once we finish adding normal values,
    // we add terminate value to tell consumers
    // to stop running.
    if(i > NUMITEMS) {
      queue.add(TERMINATE_VAL);
    } else {
      queue.add(i);

      mtx_output.lock();
      cout << "Producer added " << i << " (qsz: " << queue.size() << ")" << endl;
      mtx_output.unlock();
    }

    queue_not_empty.notify_one();
    queue_lck.unlock();
  }

  mtx_output.lock();
  cout << "Producer ending" << endl;
  mtx_output.unlock();
}

void consumer(const int thread_id) {
  mtx_output.lock();
  cout << "Consumer[" << thread_id << "] starting on core " << sched_getcpu() << endl;
  mtx_output.unlock();

  for(int i = 1; i <= NUMITEMS; i++) {
    unique_lock<mutex> queue_lck(mtx_queue_state);

    while(queue.isEmpty())
      queue_not_empty.wait(queue_lck);

    int item = queue.remove();
    if(item == TERMINATE_VAL) {
      queue_not_full.notify_one();
      queue_lck.unlock();
      break;
    }

    consumed_count[thread_id] += 1;

    mtx_output.lock();
    cout << "Consumer[" << thread_id << "] rem'd " << item << " (qsz: " << queue.size() << ")" << endl;
    mtx_output.unlock();

    queue_not_full.notify_one();
    queue_lck.unlock();
  }

  mtx_output.lock();
  cout << "Consumer[" << thread_id << "] ending" << endl;
  mtx_output.unlock();
}

int main(int argc, char** argv) {
  num_consumers = 1;

  if(argc > 1)
    num_consumers = atoi(argv[1]);
  if(num_consumers < 1)
    num_consumers = 1;

  cout << "Number of consumers is " << num_consumers << endl;

  thread producer_thread(producer);
  vector<thread> consumers;

  for(int i = 0; i < num_consumers; i++) {
    consumed_count.push_back(0);
    consumers.push_back(thread(consumer, i));
  }

  producer_thread.join();
  for(auto t = consumers.begin(); t != consumers.end(); t++)
    (*t).join();

  int total = 0;
  cout << "Consumer stats: ";
  cout << "[";
  for(auto t = consumed_count.begin(); t != consumed_count.end(); t++) {
    total += *t;
    cout << *t << ", ";
  }
  cout << "] = " << total << endl;

  cout << "main: all done!" << endl;
  return 0;
}
