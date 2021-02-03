// Liam Wynn, 1/19/2021, CS515p: Parallel Programming

/*
  In this assignment we implement a full
  rich producer-consumer model. Whereas
  the previous iteration had a variable
  number of consumers, this one will have 
  also a variable number of producers.
  Each producer will be responsible for
  a portion of all the items.
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

// The number of threads that will add items
// to the queue.
int num_producers;
// The number of items for each producer thread.
// This will be NUMITEMS / num_producers.
int items_per_thread;
// If NUMITEMS / num_producers has a remainder,
// we store that here. This will be added to the
// last producer thread.
int items_remainder;

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

// When a producer thread is done, it will update
// this value. When the last thread reaches this
// value, it will add the terminating values.
mutex mtx_producer_finished;
int prod_finished_count;

void producer(const int thread_id) {
  int start, end;

  // Plus 1 so that we start at 1.
  start = thread_id * items_per_thread + 1;
  end = start + items_per_thread - 1;

  if(thread_id == num_producers - 1)
    end += items_remainder;

  mtx_output.lock();
  cout << "Producer[" << thread_id << "] for segment ["
       << start << "," << end << "] starting on core " << sched_getcpu() << endl;
  mtx_output.unlock();

  // Each producer can had its items in any order.
  for(int i = start; i <= end; i++) {
    unique_lock<mutex> queue_lck(mtx_queue_state);
    while(queue.size() == BUFSIZE) {
      queue_not_full.wait(queue_lck);
    }

    // Once we finish adding normal values,
    // we add terminate value to tell consumers
    // to stop running.
    queue.add(i);

    mtx_output.lock();
    cout << "Producer[" << thread_id << "] added " << i << " (qsz: " << queue.size() << ")" << endl;
    mtx_output.unlock();

    queue_not_empty.notify_one();
    queue_lck.unlock();
  }

  mtx_output.lock();
  cout << "Producer[" << thread_id << "] ending" << endl;
  mtx_output.unlock();

  mtx_producer_finished.lock();
  prod_finished_count++;
  if(prod_finished_count == num_producers) {
    for(int i = 0; i < num_consumers; i++) { 
      unique_lock<mutex> queue_lck(mtx_queue_state);
      while(queue.size() == BUFSIZE)
        queue_not_full.wait(queue_lck);

      queue.add(TERMINATE_VAL);
      queue_not_empty.notify_one();
      queue_lck.unlock();
    }
  }
  mtx_producer_finished.unlock();
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
  num_producers = 1;
  prod_finished_count = 0;

  if(argc >= 3) {
    num_consumers = atoi(argv[1]);
    num_producers = atoi(argv[2]);
  } else if(argc >= 2) {
    num_consumers = atoi(argv[1]);
  }

  num_consumers = num_consumers <= 0 ? 1 : num_consumers;
  num_producers = num_producers <= 0 ? 1 : num_producers;

  items_per_thread = NUMITEMS / num_producers;
  items_remainder = NUMITEMS - (items_per_thread * num_producers);

  cout << "Number of consumers is " << num_consumers << endl;
  cout << "Number of producers is " << num_producers << endl;

  vector<thread> producers;
  vector<thread> consumers;

  for(int i = 0; i < num_producers; i++)
    producers.push_back(thread(producer, i));

  for(int i = 0; i < num_consumers; i++) {
    consumed_count.push_back(0);
    consumers.push_back(thread(consumer, i));
  }

  for(auto t = producers.begin(); t != producers.end(); t++)
    (*t).join();
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
