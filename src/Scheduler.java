import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** 
 * Scheduler.java
 * SYSC3303G4
 *  @author 
 *  
 *  @version Iteration 1
 *  
 * 
 * 
 * This class is to receive the information/requests from the FloorSubsystem and send
 * them to ElevatorSubSystem and send response back to the FloorSubsystem.
 * The scheduler accepts inputs from the InputEvent class and send the requests to 
 * ElevatorSubSystem. The Scheduler is also updated when an Elevator reaches it's desired floor
 * 
 */
/**
 * @author TZ-L
 *
 */
/**
 * @author TZ-L
 *
 */
public class Scheduler {
	
	private static final int FLOOR_COUNT = 6;
	
	private static final int ELEVATOR_COUNT = 1;
	
	// List of input events received from Floor Subsystem to be handled
	private ArrayList<InputEvent> eventList;
	
	private ArrayList<InputEvent> upRequests;
	
	private ArrayList<InputEvent> downRequests;
	
	private ArrayList<ArrayList<Integer>> elevatorTaskQueue;
	
	private ArrayList<Integer> currentPositionList;
	
	private DatagramPacket sendPacket;
	
	private enum Direction{
		UP, DOWN, IDLE
	}
	
	private ArrayList<Direction> directionList;
	
	// Default byte array size for Datagram packets
	private static final int BYTE_SIZE = 6400;
	
	private DatagramSocket sendSocket, floorReceiveSocket, elevatorReceiveSocket;
	
	private static final int FLOOR_RECEIVE_PORT = 60002;
	
	private static final int ELEVATOR_SEND_PORT = 60008;
	
	private static final int ELEVATOR_RECEIVE_PORT = 60006;
	
	private static final int FLOOR_SEND_PORT = 60004;
	
	
	
	/**
	 * Constructor 	
	 */
	public Scheduler() {
		
	
		this.elevatorTaskQueue = new ArrayList<ArrayList<Integer>>(ELEVATOR_COUNT);	
		this.elevatorTaskQueue.add(new ArrayList<Integer>());
		
		//current position of elevator is 1
		this.currentPositionList = new ArrayList<Integer>(ELEVATOR_COUNT);
		this.currentPositionList.add(1);
		this.currentPositionList.set(0, 1);

		
		this.upRequests = new ArrayList<InputEvent>();
		
		this.downRequests = new ArrayList<InputEvent>();
		
		this.eventList = new ArrayList<InputEvent>();
		
		this.directionList = new ArrayList<Direction>(ELEVATOR_COUNT);
		
		for (Direction direction: directionList) {
			direction = Direction.IDLE;
		}
		
		try {
			floorReceiveSocket = new DatagramSocket(FLOOR_RECEIVE_PORT);
			//elevatorReceiveSocket = new DatagramSocket(ELEVATOR_RECEIVE_PORT);
		} catch (SocketException se) {
	        se.printStackTrace();
	        System.exit(1);
		}
	
		try {
			elevatorReceiveSocket = new DatagramSocket(ELEVATOR_RECEIVE_PORT);
			//elevatorReceiveSocket = new DatagramSocket(ELEVATOR_RECEIVE_PORT);
		} catch (SocketException se) {
	        se.printStackTrace();
	        System.exit(1);
		}
	}
	
	/**
	 * Receive input event list 
	 */
	public void receiveInputEventList() {
		 byte[] data = new byte[BYTE_SIZE];
	     DatagramPacket receivePacket = new DatagramPacket(data, data.length);
	     
	     // Receive datagram socket from floor subsystem
	     try {  
	         floorReceiveSocket.receive(receivePacket);
	      } catch(IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }
	     
	     this.eventList.addAll(byteArrayToList(data));
	     
	     
	     
	     for (InputEvent event : this.eventList) {
	     
	    	 System.out.println("Request from: " + event.getCurrentFloor() + " destination: " + event.getDestinationFloor());
	     }
	     
	}

	
	/**
	 * Converts bytes to array 
	 * @param data
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<InputEvent> byteArrayToList(byte[] data){
		
		ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
	    ObjectInputStream objStream = null;
		try {
			objStream = new ObjectInputStream(byteStream);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
	    try {
			return (ArrayList<InputEvent>) objStream.readObject();
		} catch (ClassNotFoundException e) {
			// Class not found
			e.printStackTrace();
		} catch (IOException e) {
			// Could not red object from stream
			e.printStackTrace();
		}
	    
		return null;
		
	}
	
	/**
	 * Processing request 
	 */
	public void processRequests() {
		for (InputEvent event : eventList) {
			if (event.getUp() == true) {
				this.upRequests.add(event);
			} else {
				this.downRequests.add(event);
			}
		}
		
		this.eventList.clear();
		
		if (!upRequests.isEmpty()) {
			Collections.sort(upRequests);
		}
		
		if (!downRequests.isEmpty()) {
			Collections.sort(downRequests);
		}
		
		if (!upRequests.isEmpty()) {
			for (Direction direction : this.directionList) {
				if (direction == Direction.IDLE) {
					direction = Direction.UP;
					break;
				}
			}
		} else if (!downRequests.isEmpty()) {
			for (Direction direction : this.directionList) {
				if (direction == Direction.IDLE) {
					direction = Direction.DOWN;
					break;
				}
			}
		}
		
		Iterator<InputEvent> i = upRequests.iterator();
		
		System.out.println(this.currentPositionList);
		
		while(i.hasNext()) {
			InputEvent e = i.next();
			if (e.getCurrentFloor().equals(this.currentPositionList.get(0))) {
				this.elevatorTaskQueue.get(0).add(e.getDestinationFloor());
			} else {
				this.elevatorTaskQueue.get(0).add(e.getCurrentFloor());
				this.elevatorTaskQueue.get(0).add(e.getDestinationFloor());
			}
			i.remove();
		}
		
		Iterator<InputEvent> d = downRequests.iterator();
		
		while(d.hasNext()) {
			InputEvent e = d.next();
			if (e.getCurrentFloor().equals(this.currentPositionList.get(0))) {
				this.elevatorTaskQueue.get(0).add(e.getCurrentFloor());
			} else {
				this.elevatorTaskQueue.get(0).add(e.getDestinationFloor());
				this.elevatorTaskQueue.get(0).add(e.getDestinationFloor());
			}
			d.remove();
		}
		
		
	}
	
	/**
	 * Converts task list to Bytes 
	 * @param elevatorNumber
	 * @return
	 */
	public byte[] taskListToByteArray(int elevatorNumber) {
			
		ArrayList<Integer> list = new ArrayList<Integer>();
		System.out.println(this.elevatorTaskQueue);
		for (Integer integer : this.elevatorTaskQueue.get(0)) {
			if (!list.contains(integer)) {
				list.add(integer);
			}
		}
		
		this.elevatorTaskQueue.get(0).clear();
		this.elevatorTaskQueue.get(0).addAll(list);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTE_SIZE);
		
		ObjectOutputStream oos = null;
		
		try {
			oos = new ObjectOutputStream(baos);
		} catch (IOException e1) {
			// Unable to create object output stream
			e1.printStackTrace();
		}
		
		try {
			oos.writeObject(this.elevatorTaskQueue.get(0));
		} catch (IOException e) {
			// Unable to write eventList in bytes
			e.printStackTrace();
		}
		
		this.elevatorTaskQueue.get(0).clear();
		
		return baos.toByteArray();
		
		
	}
	
	/**
	 * Send task to unique elevator
	 * @param elevatorNumber
	 */
	public void sendTask(int elevatorNumber) {
		if (this.elevatorTaskQueue.get(0).size() > 0) {
			byte[] data = taskListToByteArray(elevatorNumber);
			
			// Create Datagram packet containing byte array of event list information
			try {
			     sendPacket = new DatagramPacket(data,
			                                     data.length, InetAddress.getLocalHost(), ELEVATOR_SEND_PORT);
			  } catch (UnknownHostException e) {
			     e.printStackTrace();
			     System.exit(1);
			  }
			
			try {
				this.sendSocket = new DatagramSocket();
			} catch (SocketException se) {
				se.printStackTrace();
				System.exit(1);
			}
			
			try {
		         sendSocket.send(sendPacket);
		      } catch (IOException e) {
		         e.printStackTrace();
		         System.exit(1);
		      }
			sendSocket.close();
		}
	}	
	
	/**
	 * Receive information from elevator 
	 */
	public void receiveFromElevator() {
		byte[] data = new byte[BYTE_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(data, data.length);

		// Receive datagram socket from floor subsystem
		try {
			elevatorReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		Pair arrival = byteArrayToPair(data);
		
		this.currentPositionList.set(0, arrival.getInteger());
		
		System.out.println("The elevator has arrived at floor: " + arrival.getInteger());
	
		byte[] sendData = data;

		try {
		     sendPacket = new DatagramPacket(sendData,
		                                     sendData.length, InetAddress.getLocalHost(), FLOOR_SEND_PORT);
		  } catch (UnknownHostException e) {
		     e.printStackTrace();
		     System.exit(1);
		  }
		
		try {
			this.sendSocket = new DatagramSocket();
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(1);
		}
		
		try {
	         sendSocket.send(sendPacket);
	      } catch (IOException e) {
	         e.printStackTrace();
	         System.exit(1);
	      }
		sendSocket.close();
	}
	
	
	/**
	 * convert Byte Array to Pair object 
	 * @param data
	 * @return
	 */
	private Pair byteArrayToPair(byte[] data) {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
	    ObjectInputStream objStream = null;
		try {
			objStream = new ObjectInputStream(byteStream);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		
	    try {
			return (Pair) objStream.readObject();
		} catch (ClassNotFoundException e) {
			// Class not found
			e.printStackTrace();
		} catch (IOException e) {
			// Could not red object from stream
			e.printStackTrace();
		}
	    
		return null;
	}

	/**
	 * main function 
	 * @param args
	 */
	public static void main(String[] args) {
		Scheduler s = new Scheduler();
		
		Thread receiveFromElevator = new Thread() {
			public void run() {
				while(true) {
					s.receiveFromElevator();
				}
			}
		};
		
		Thread runScheduler = new Thread() {
			public void run() {
				while(true) {
					s.receiveInputEventList();
					s.processRequests();
					s.sendTask(1);
				}
			}
		};
		
		runScheduler.start();
		receiveFromElevator.start();
		
		
		

	}

}
