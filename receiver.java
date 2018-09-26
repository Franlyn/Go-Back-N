// receiver.java
// Author: Franlyn Liu

/*
receiving packets sent by the sender via the network emulator
*/


/* Required Inputs in order:
<hostname for the network emulator>
<UDP port number used by the link emulator to receive ACKs from the receiver>
<UDP port number used by the receiver to receive data from the emulator>
<name of the file into which the received data is written
*/

import java.io.*;
import java.net.*;
import java.util.*;


class receiver {
	static final int SeqNumModulo = 32;

	static String emuHostAddr = null;
	// UDP port number used by the link emulator to receive ACKs from the receiver
	static int port_ACK = 0;
	// UDP port number used by the receiver to receive data from the emulator
	static int port_data = 0;
	static String fileName = null;

	public static void main(String args[]) throws Exception {
		if (args.length != 4) {
			System.err.println("Invalid inputs.");
			System.exit(1);
		}

		// Store input values into the four params
		emuHostAddr = args[0];
		port_ACK = Integer.parseInt(args[1]);
		port_data = Integer.parseInt(args[2]);
		fileName = args[3];

		PrintWriter arrLog = new PrintWriter("arrival.log", "UTF-8");
		PrintWriter outputLog = new PrintWriter(fileName, "UTF-8");

		// receive data and send ACKs
		try {
			recDataSendACK(emuHostAddr, port_ACK, port_data, arrLog, outputLog);
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Receiver failed");
		}
	}

	private static void recDataSendACK(String emuHostAddr, int port_ACK, int port_data,
					PrintWriter arrLog, PrintWriter outputLog) {
		InetAddress IPAddr;
		DatagramSocket socket_ACK;
		DatagramSocket socket_data;

		ArrayList<Integer> seqHasRcvd = new ArrayList<Integer>();

		try {
			IPAddr  = InetAddress.getByName(emuHostAddr);

		// open sockets
			socket_ACK = new DatagramSocket(0);
			socket_data = new DatagramSocket(port_data);
		} catch (Exception e) {
			System.out.println(e);
			System.out.println("Fail to get IP or open the sockets");
			return;
		}

		byte[] dataRcv = new byte[1024];
		byte[] sendACK = new byte[1024];

		/*
		When receiving packets sent by the sender via the network emulator, it should execute the following:
		• check the sequence number of the packet;
		• if the sequence number is the one that it is expecting, 
			it should send an ACK packet back to the sender 
			with the sequence number equal to the sequence number of the received packet;
		• in all other cases, it should discard the received packet 
			and resends an ACK packet for the most recently received in-order packet;
		*/
		int expected = 0;
		boolean first = false;
		while (true) {
			// receive the packet
			DatagramPacket packetRcv = new DatagramPacket(dataRcv, dataRcv.length);
			
			packet packetRcvd = null;

			try {
				socket_data.receive(packetRcv);

				// parse data into the packet
				packetRcvd = packet.parseUDPdata(packetRcv.getData());
			} catch (Exception e) {
				System.out.println(e);
				System.out.println("Receiving packet failed");
				return;
			}

			// Get the seqnum and type
			int seqnumRcvd = packetRcvd.getSeqNum();
			int type = packetRcvd.getType();
			String curData = new String(packetRcvd.getData());

			// Write to arrival.log
			if ( !seqHasRcvd.contains(seqnumRcvd) && type == 1 ) {
				arrLog.println(seqnumRcvd);
				seqHasRcvd.add(seqnumRcvd);
			}

			// Make sure that the first packet is received properly
			if (!first) {
				if (seqnumRcvd == 0) {
					first = true;
				} else {
					continue;
				}
			}

			if (seqnumRcvd == expected) {
				expected = (expected + 1) % SeqNumModulo;
				if (type == 1) {	// Write to output if type is data
					outputLog.print(curData);
				}
			} else {
				seqnumRcvd = (expected - 1) % SeqNumModulo;
			}

			// send EOT or ACK
			packet sendPacket;
			try {
				if (type == 2) {
					sendPacket = packet.createEOT(seqnumRcvd);
				} else {
					sendPacket = packet.createACK(seqnumRcvd);
				}

				sendACK = sendPacket.getUDPdata();
				DatagramPacket packetSend = new DatagramPacket(sendACK, sendACK.length, IPAddr, port_ACK);
				socket_ACK.send(packetSend);

			} catch (Exception e) {
				System.out.println(e);
				System.out.println("receiver: Fail to send EOT or ACK");
				return;
			}

			if (type == 2) {
				arrLog.close();
				outputLog.close();
				break;
			}
		}
	}
}
