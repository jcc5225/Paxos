package kvpaxos;

import java.io.Serializable;

/**
 * Please fill in the data structure you use to represent the response message for each RMI call.
 * Hint: Make it more generic such that you can use it for each RMI call.
 */
public class Response implements Serializable {
    static final long serialVersionUID=22L;

    boolean ack;
    int n_a;
    Object v_a;
    Object v_d;
    type type;
    int n;
    boolean a_reject;
    boolean p_reject;
    boolean decided;
    // for server-client interaction
    boolean success = false;
    Integer value;

    public enum type {
        ACCEPT,
        DECIDE,
        PREPARE
    }

    public Response() {
        a_reject = false;
        p_reject = false;
        decided = false;
        this.n_a = 0;
        this.v_a = 0;
    }

    public Response(boolean success) {
        this.success = success;
    }

    public Response(Integer value) {
        this.value = value;
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

    public void decided(Object value) {
        this.v_d = value;
        decided = true;
    }
}
