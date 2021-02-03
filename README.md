# ProdCons
Implements three different versions of the Producer-Consumer problem in both Java and C++.

# What are the three versions?
The first one is a simple producer-consumer model where you have one producer and one
consumer. The second one has one producer and multiple consumers. The final version has
multiple producers and multiple consumers.

# How do I run the code?
## C++ Version:
To compile do:

`g++ -g -pthread queue.cpp prodcons1.cpp`

Then run:

`./a.out`

Note that there is `prodcons1.cpp`, `prodcons2.cpp`, and `prodcons3.cpp`. Use whichever
version you want.

## Java Version
To compile do:

`javac -g ProdCons1.java`

Then run:

`java ProdCons1`

Note that there is `ProdCons1.java`, `ProdCons2.java`, and `ProdCons3.java`. Use which ever
version you want.
