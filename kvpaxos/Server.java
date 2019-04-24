package kvpaxos;
import paxos.Paxos;
import paxos.State;
// You are allowed to call Paxos.Status to check if agreement was made.

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class Server implements KVPaxosRMI {

    ReentrantLock mutex;
    Registry registry;
    Paxos px;
    int me;

    String[] servers;
    int[] ports;
    KVPaxosRMI stub;

    AtomicInteger seq;
    Integer lastPut;
    HashMap<Integer, Op> log;
    HashMap<String, Integer> lib;


    public Server(String[] servers, int[] ports, int me){
        this.me = me;
        this.servers = servers;
        this.ports = ports;
        this.mutex = new ReentrantLock();
        this.px = new Paxos(me, servers, ports);
        // Your initialization code here
        this.seq = new AtomicInteger();
        this.seq.set(0);
        this.lastPut = -1;
        this.log = new HashMap<>();
        this.lib = new HashMap<>();


        try{
            System.setProperty("java.rmi.server.hostname", this.servers[this.me]);
            registry = LocateRegistry.getRegistry(this.ports[this.me]);
            stub = (KVPaxosRMI) UnicastRemoteObject.exportObject(this, this.ports[this.me]);
            registry.rebind("KVPaxos", stub);
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    // RMI handlers
    public Response Get(Request req){
        // start a paxos instance
        // wait for decision
        // if decision is op, return
        // else, try again
        mutex.lock();

        // implement all puts up to this point
        for (lastPut++; lastPut < seq.get(); lastPut++) {
            Op oper = log.get(lastPut);
            px.Done(lastPut);
            if (oper.op.equals("put")) {
                lib.put(oper.key, oper.value);
            }
        }

        Op desired = req.operation;
        while (true) {
            Integer round = seq.getAndIncrement();
            px.Start(round, desired);
            Op actual = wait(round);
            log.put(round, actual);
            if (actual.op.equals("put")) {
                lib.put(actual.key, actual.value);
                lastPut = round;
            }
            else if (actual.equals(desired)) {
                mutex.unlock();
                return new Response(lib.get(actual.key));
            }
        }

    }

    public Response Put(Request req){
        // start a paxos instance
        // wait for decision
        // if decision is op, return
        // else, try again
        mutex.lock();
        Op desired = req.operation;
        while (true) {
            Integer round = seq.getAndIncrement();
            px.Start(round, desired);
            Op actual = wait(round);
            log.put(round, actual);
            if (actual.equals(desired)) {
                mutex.unlock();
                return new Response(true);
            }
        }
    }


    public Op wait(int seq) {
        int to = 10;
        while (true){
            Paxos.retStatus ret = this.px.Status(seq);
            if(ret.state == State.Decided){
                return Op.class.cast(ret.v);
            }
            try {
                Thread.sleep(to);
            } catch (Exception e){
                e.printStackTrace();
            }
            if( to < 1000){
                to = to * 2;
            }
        }
    }

}
