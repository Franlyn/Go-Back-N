# Go-back-N Protocol
This project implements the Go-Back-N protocol, which could be used to transfer a text file from one host to another across an unreliable network.

## Language: 
Java

## Files: 
sender.java, receiver.java, packet.java, README.md, Emulator-linux386, Makefile<br />
Emulator-linux386 is the executable of a network emulator to test the performance of the application over a virtual network

## Compilation: 
Makefile is included. Can be compiled by command "make".

## Execution and Parameters:
sender:
<ul>
<li><host address of the network emulator></li>
<li><UDP port number used by the emulator to receive data from the sender></li>
<li><UDP port number used by the sender to receive ACKs from the emulator></li>
<li><name of the file to be transferred></li>
</ul>

receiver:
<ul>
<li><hostname for the network emulator></li>
<li><UDP port number used by the link emulator to receive ACKs from the receiver></li>
<li><UDP port number used by the receiver to receive data from the emulator></li>
<li><name of the file into which the received data is written></li>
</ul>

Example Execution:
<ul>
<li>On the host host1: nEmulator 9991 host2 9994 9993 host3 9992 1 0.2 0</li>
<li>On the host host2: java receiver host1 9993 9994 <output File></li>
<li>On the host host3: java sender host1 9991 9992 <input file></li>
</ul>


## Machine & Test Cases:
ubuntu1604-008: ./nEmulator-linux386 9991 ubuntu1604-004 9994 9993 ubuntu1604-002 9992 1 0.2 0
ubuntu1604-004: java receiver ubuntu1604-008 9993 9994 "output.txt"
ubuntu1604-002: java sender ubuntu1604-008 9991 9992 "input.txt"

