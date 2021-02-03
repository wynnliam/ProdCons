//----------------------------------------------------------------------------- 
// Program code for CS 415P/515 Parallel Programming, Portland State University
//----------------------------------------------------------------------------- 

// Queue representation
//
class Queue {
private:
  int *buffer;   // a circular buffer
  int capacity;  // buffer capacity
  int head=0;    // head
  int tail=0;    // tail
  int count=0;	 // current size
public:
  Queue(int bufsize) { // construct a queue for a given size
    capacity = bufsize;
    buffer = new int[capacity];
  }
  void add(int val);   // add a new item (to tail)
  int remove();        // remove an item (from head)
  int  size();         // return current size of queue
  bool isEmpty();      // return true if queue is empty
  bool isFull();       // return true if queue is full
};
