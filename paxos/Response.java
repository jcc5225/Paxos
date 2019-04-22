package paxos;
import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the response message for each RMI call.
 * Hint: You may need a boolean variable to indicate ack of acceptors and also you may need proposal number and value.
 * Hint: Make it more generic such that you can use it for each RMI call.
 */
public class Response implements Serializable {
    static final long serialVersionUID=2L;
    // your data here
    boolean ack;
    int n_a;
    Object v_a;
    type type;
    int n;
    boolean a_reject;
    boolean p_reject;
    
    public enum type {
    	ACCEPT,
    	DECIDE,
    	PREPARE
    }
    
    public Response() {
    		a_reject = false;
    		p_reject = false;
    }
    
    public void acceptOk(int n) {
    		type = type.ACCEPT;
    		this.n = n;
    }
    
    public void prepareOk(int n, int n_a, Object value_a) {
		type = type.PREPARE;
		this.n = n;
		this.v_a = value_a;
		this.n_a = n_a;
    }
    public void prepareReject() {
    		type = type.PREPARE;
    		p_reject = true;
    }
    public void acceptReject() {
		type = type.ACCEPT;
		a_reject = true;
    }
}
