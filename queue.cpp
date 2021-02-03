//----------------------------------------------------------------------------- 
// Program code for CS 415P/515 Parallel Programming, Portland State University
//----------------------------------------------------------------------------- 

#include <assert.h>
#include "queue.h"

void Queue::add(int val) {
  assert(count < capacity);
  buffer[tail] = val;
  tail = (tail + 1) % capacity;
  count++;
}

int Queue::remove() {
  assert(count > 0);
  int val = buffer[head];  
  head = (head + 1) % capacity;
  count--;
  return val;
}

int  Queue::size()    { return count; }
bool Queue::isFull()  { return count >= capacity; }
bool Queue::isEmpty() { return count == 0; }

