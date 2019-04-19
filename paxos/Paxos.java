package paxos;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
/**
 * This class is the main class you need to implement paxos instances.
 */
public class Paxos implements PaxosRMI, Runnable{

    ReentrantLock mutex;
    Condition decided = mutex.newCondition();
    String[] peers; // hostname
    int[] ports; // host port
    int me; // index into peers[]

    Registry registry;
    PaxosRMI stub;

    AtomicBoolean dead;// for testing
    AtomicBoolean unreliable;// for testing
    
    Map<Long, Integer> idMap;
    Map<Integer, A_State> seqMap;
    int seq;
    Object value;
    AtomicInteger z;
    int maxSeq;
    //highest prepare seen
    //highest accept seen
    //n value
    
    private class A_State {
    		
    		int n_p;
    		int n_prime;
    		Object v_prime;
        int n_a;
        Object v_a;
        Object v;
        int n;
        retStatus status; 
        
        public A_State(Object value) {
        		status = new retStatus(State.Pending, null);
        }
    }
    


    /**
     * Call the constructor to create a Paxos peer.
     * The hostnames of all the Paxos peers (including this one)
     * are in peers[]. The ports are in ports[].
     */
    public Paxos(int me, String[] peers, int[] ports){

        this.me = me;
        this.peers = peers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.dead = new AtomicBoolean(false);
        this.unreliable = new AtomicBoolean(false);

        // Your initialization code here
        idMap = new HashMap<Long, Integer>();
        seqMap = new HashMap<Integer, A_State>();
        	z.set(-1);
        	

        // register peers, do not modify this part
        try{
            System.setProperty("java.rmi.server.hostname", this.peers[this.me]);
            registry = LocateRegistry.createRegistry(this.ports[this.me]);
            stub = (PaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("Paxos", stub);
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    /**
     * Call() sends an RMI to the RMI handler on server with
     * arguments rmi name, request message, and server id. It
     * waits for the reply and return a response message if
     * the server responded, and return null if Call() was not
     * be able to contact the server.
     *
     * You should assume that Call() will time out and return
     * null after a while if it doesn't get a reply from the server.
     *
     * Please use Call() to send all RMIs and please don't change
     * this function.
     */
    public Response Call(String rmi, Request req, int id){
        Response callReply = null;

        PaxosRMI stub;
        try{
            Registry registry=LocateRegistry.getRegistry(this.ports[id]);
            stub=(PaxosRMI) registry.lookup("Paxos");
            if(rmi.equals("Prepare"))
                callReply = stub.Prepare(req);
            else if(rmi.equals("Accept"))
                callReply = stub.Accept(req);
            else if(rmi.equals("Decide"))
                callReply = stub.Decide(req);
            else
                System.out.println("Wrong parameters!");
        } catch(Exception e){
            return null;
        }
        return callReply;
    }


    /**
     * The application wants Paxos to start agreement on instance seq,
     * with proposed value v. Start() should start a new thread to run
     * Paxos on instance seq. Multiple instances can be run concurrently.
     *
     * Hint: You may start a thread using the runnable interface of
     * Paxos object. One Paxos object may have multiple instances, each
     * instance corresponds to one proposed value/command. Java does not
     * support passing arguments to a thread, so you may reset seq and v
     * in Paxos object before starting a new thread. There is one issue
     * that variable may change before the new thread actually reads it.
     * Test won't fail in this case.
     *
     * Start() just starts a new thread to initialize the agreement.
     * The application will call Status() to find out if/when agreement
     * is reached.
     */
    public void Start(int seq, Object value){
    		maxSeq = Math.max(seq, this.seq);
        this.seq = seq;
        this.value = value;
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public void run(){
        Long id = Thread.currentThread().getId();
        idMap.put(id, this.seq);
        A_State local_state = new A_State(this.value);
        seqMap.put(this.seq, local_state);
        int countOk = 0;
        Response resp;
        while(local_state.status.state != State.Decided) {
        		getN(local_state);
        		countOk = 0;
        		local_state.n_prime = 0;
        		mutex.lock();
        		for(int i = 0; i < peers.length; i++) {
        			if (i == me) {
    					resp = Prepare( new Request(local_state.n));
    				} else {
    					resp = Call("Prepare", new Request(local_state.n), i);
    				}        			
        			if (!resp.p_reject) {
        				countOk++;
        				if(resp.n_a > local_state.n_prime) {
        					local_state.n_prime = resp.n_a;
        					local_state.v_prime = resp.v_a;
        				}
        				
        			}
        		}
        		if (countOk > peers.length/2) {
        			if(local_state.n > local_state.n_prime) {
        				local_state.v_prime = local_state.v;
        			}
        			countOk = 0;
        			for(int i = 0; i < peers.length; i++) {
        				if (i == me) {
        					resp = Accept( new Request(local_state.n, local_state.v_prime));
        				} else {
        					resp = Call("Accept", new Request(local_state.n, local_state.v_prime), i);
        				}
        			
        				if (!resp.a_reject) {
            				countOk++;
            				
            			}
        			}
        			if(countOk > peers.length/2) {
        				for(int i = 0; i < peers.length; i++) {
        					if (i == me) {
            					resp = Decide( new Request(local_state.v_prime));
            				} else {
            					resp = Call("Decide", new Request(local_state.v_prime), i);
            				}
            			}
        			}
        			
        		}
        		mutex.unlock();
        }
   
    }
    
    public void getN(A_State local_state) {
    		mutex.lock();
    		int mod = local_state.n_p % peers.length;
    		local_state.n = local_state.n_p + mod + me;
    		mutex.unlock();
    }

    // RMI handler
    public Response Prepare(Request req){
    		mutex.lock();
    		Response res = new Response();
    		int seq = idMap.get(Thread.currentThread().getId());
    		A_State a_state = seqMap.get(seq);
    		if (req.n > a_state.n_p) {
        		a_state.n_p = req.n;
        		res.prepareOk(a_state.n_p, a_state.n_a, a_state.v_a);
        } else {
        		res.prepareReject();
        }
    	
    		mutex.unlock();
    		return res;
    		    
    }

    public Response Accept(Request req){
    		mutex.lock();
    		Response res = new Response();
		int seq = idMap.get(Thread.currentThread().getId());
		A_State a_state = seqMap.get(seq);
    		
    		if (req.n > a_state.n_p) {
    			a_state.n_p = req.n;
    			a_state.n_a = req.n;
    			a_state.v_a = req.v;
    			res.acceptOk(a_state.n_p);
    		} else {
    			res.acceptReject();
    		}
    		mutex.unlock();
    		return res;  

    }

    public Response Decide(Request req){
    		mutex.lock();
    		int seq = idMap.get(Thread.currentThread().getId());
		A_State a_state = seqMap.get(seq);
		a_state.status.state = State.Decided;
		a_state.status.v = req.v;
		mutex.unlock();
		decided.signalAll();
		return null;

    }

    /**
     * The application on this machine is done with
     * all instances <= seq.
     *
     * see the comments for Min() for more explanation.
     */
    public void Done(int seq) {
        for(int i = z; i <= seq; i++) {
        		while(seqMap.get(i).status.state != State.Decided) {
        			decided.await();
        		}
        }
    }
    
    public int getMaxDone() {
    	
    }


    /**
     * The application wants to know the
     * highest instance sequence known to
     * this peer.
     */
    public int Max(){
        return maxSeq;
    }

    /**
     * Min() should return one more than the minimum among z_i,
     * where z_i is the highest number ever passed
     * to Done() on peer i. A peers z_i is -1 if it has
     * never called Done().

     * Paxos is required to have forgotten all information
     * about any instances it knows that are < Min().
     * The point is to free up memory in long-running
     * Paxos-based servers.

     * Paxos peers need to exchange their highest Done()
     * arguments in order to implement Min(). These
     * exchanges can be piggybacked on ordinary Paxos
     * agreement protocol messages, so it is OK if one
     * peers Min does not reflect another Peers Done()
     * until after the next instance is agreed to.

     * The fact that Min() is defined as a minimum over
     * all Paxos peers means that Min() cannot increase until
     * all peers have been heard from. So if a peer is dead
     * or unreachable, other peers Min()s will not increase
     * even if all reachable peers call Done. The reason for
     * this is that when the unreachable peer comes back to
     * life, it will need to catch up on instances that it
     * missed -- the other peers therefore cannot forget these
     * instances.
     */
    public int Min(){
        // Your code here

    }



    /**
     * the application wants to know whether this
     * peer thinks an instance has been decided,
     * and if so what the agreed value is. Status()
     * should just inspect the local peer state;
     * it should not contact other Paxos peers.
     */
    public retStatus Status(int seq){
    		return seqMap.get(seq).status;
    }

    /**
     * helper class for Status() return
     */
    public class retStatus{
        public State state;
        public Object v;

        public retStatus(State state, Object v){
            this.state = state;
            this.v = v;
        }
    }

    /**
     * Tell the peer to shut itself down.
     * For testing.
     * Please don't change these four functions.
     */
    public void Kill(){
        this.dead.getAndSet(true);
        if(this.registry != null){
            try {
                UnicastRemoteObject.unexportObject(this.registry, true);
            } catch(Exception e){
                System.out.println("None reference");
            }
        }
    }

    public boolean isDead(){
        return this.dead.get();
    }

    public void setUnreliable(){
        this.unreliable.getAndSet(true);
    }

    public boolean isunreliable(){
        return this.unreliable.get();
    }


}
