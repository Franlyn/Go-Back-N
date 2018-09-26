// sender.java
// Author: Franlyn Liu

/*
read data from the specified file and send it 
using the Go-Back-N protocol to the receiver via the network emulator
*/


/* Required Inputs in order:
<host address of the network emulator>
<UDP port number used by the emulator to receive data from the sender>
<UDP port number used by the sender to receive ACKs from the emulator>
<name of the file to be transferred>
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;

public class sender {
	static final int windowSize = 10;
	static final int maxPacketDataLength = 500;
	static final int SeqNumModulo = 32;

	static final int timer = 10;	// timer is set to 10 msec

	static String emuHostAddr = null;
	static int port_emuData = 0;
	static int port_senderACK = 0;
	static String fileName = null;


	public static void main(String[] args) throws Exception {

		if (args.length != 4) {
			System.out.println("Invalid inputs.");
			System.exit(1);
		}

		// Store input values into the four params
		emuHostAddr = args[0];
		port_emuData = Integer.parseInt(args[1]);
		port_senderACK = Integer.parseInt(args[2]);
		fileName = args[3];

		// Read from file and process into packets
		byte[] dataRead = Files.readAllBytes(new File(fileName).toPath());
		packet[] packetSend = parseToPackets(dataRead);

		/* Send data using the Go-Back-N protocol to the receiver 
		via the network emulator */
		PrintWriter seqnumLog = null;
		PrintWriter ackLog = null;
		try {
			// Create seqnum.log and ack.log
			seqnumLog = new PrintWriter("seqnum.log", "UTF-8");
			ackLog = new PrintWriter("ack.log", "UTF-8");
			sendData(packetSend, seqnumLog, ackLog);
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Sending file failed");
		}

		// close the writer
		seqnumLog.close();
		ackLog.close();
	}


	/* Send data using the Go-Back-N protocol to the receiver 
		via the network emulator */
	private static void sendData(packet[] packets, PrintWriter seqnumLog, PrintWriter ackLog) {
		int next = 0;		// index of the next packet needs to be sent
		int isACKed = 0;		// next packet that should be ACKed
		int packetLen = packets.length;	// # packets in total

		InetAddress IPAddr = null;
		DatagramSocket sendSocket = null;
		DatagramSocket rcvSocket = null;

		try {
			IPAddr = InetAddress.getByName(emuHostAddr);

			sendSocket = new DatagramSocket(0);	// send data
			rcvSocket = new DatagramSocket(port_senderACK);	// receive ACK
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("sender: Fail to find IP or create sockets");
		}

		while (isACKed < packetLen) {		// loop until all packets got ACKed

			// if window is not full, send packets
			while (next < packetLen && next < isACKed + windowSize) {

				byte[] dataToSend = packets[next].getUDPdata();
				DatagramPacket packetToSend = 
					new DatagramPacket(dataToSend, dataToSend.length, IPAddr, port_emuData);
				try {
					sendSocket.send(packetToSend);
				} catch (Exception e) {
                 		       System.out.println(e);
                        		System.out.println("sender: Fail to send packets");
               			 }

				// Write to seqnum.log
				seqnumLog.println(packets[next].getSeqNum());

				next++;
			}

			// receive ACKs
			try {
				byte[] dataRcvd = new byte[1024];
				// set the timer
				rcvSocket.setSoTimeout(timer);
				DatagramPacket packetAcked = new DatagramPacket(dataRcvd, dataRcvd.length);
				rcvSocket.receive(packetAcked);

				// parse data into the packet
				packet packetRcvd = packet.parseUDPdata(packetAcked.getData());
				// Get the seqnum
				int seqnumRcvd = packetRcvd.getSeqNum();

				// Write to ack.log
				ackLog.println(seqnumRcvd);

				// Update isACKed, need to consider cummulative ACKs
				isACKed += seqnumRcvd - isACKed % SeqNumModulo + 1;

			} catch (SocketTimeoutException e) {
				//System.out.println("Time out");
				/* A timeout occurs,
				the sender resends all packets that have been previously sent 
				but that have not yet been acknowledged */
				for (int i = isACKed; i < next; i++) {
					byte[] dataToSend = packets[i].getUDPdata();
					DatagramPacket packetToSend = 
						new DatagramPacket(dataToSend, dataToSend.length, IPAddr, port_emuData);
					try {
						sendSocket.send(packetToSend);
					} catch (Exception err) {
                         			System.out.println(err);
                                        	System.out.println("sender: Fail to resend packets");
                                 	}
				}
			} catch (Exception e) {
                                        System.out.println(e);
                                        System.out.println("sender: Fail to receive ACKs");
                        }
		}

		// Send an EOT packet to the receiver
		try {
			packet eotPacket = packet.createEOT(packetLen);
			byte[] eotData = eotPacket.getUDPdata();
			DatagramPacket eot = new DatagramPacket(eotData, eotData.length, IPAddr, port_emuData);
			sendSocket.send(eot);
			//System.out.println("sender: eot sent");
		} catch (Exception e) {
                        System.out.println(e);
                        System.out.println("sender: Fail to send EOT packet");
                }

		// receive the EOT from receiver
		int eotTypeRcv = -1;
		do {
			byte[] eotDataRcv = new byte[1024];
			DatagramPacket eotRcv = new DatagramPacket(eotDataRcv, eotDataRcv.length);
			try {
				rcvSocket.receive(eotRcv);

				packet eotPacketRcv = packet.parseUDPdata(eotRcv.getData());
				eotTypeRcv = eotPacketRcv.getType();
			} catch (Exception e) {
                                System.out.println(e);
                                System.out.println("sender: Fail to receive EOT");
                        }
		} while (eotTypeRcv != 2);
	}


	private static packet[] parseToPackets(byte[] data) throws Exception {
		int byteLen = data.length;
		int num_packets = byteLen / maxPacketDataLength 
			+ (byteLen % maxPacketDataLength == 0 ? 0 : 1);
		packet[] packetSend = new packet[num_packets];

		// Divide the data into packets
		int curByte = 0;
		for (int i = 0; i < num_packets; i++) {
			String curData = new String(Arrays.copyOfRange(data, curByte, 
				Math.min(curByte + maxPacketDataLength, byteLen)));
			curByte += maxPacketDataLength;

			packetSend[i] = packet.createPacket(i % SeqNumModulo, curData);
		}

		return packetSend;
	}
}
