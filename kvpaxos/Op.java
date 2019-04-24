package kvpaxos;
import java.io.Serializable;

/**
 * You may find this class useful, free to use it.
 */
public class Op implements Serializable{
    static final long serialVersionUID=33L;
    String op;
    int ClientSeq;
    String key;
    Integer value;

    public Op(String op, int ClientSeq, String key, Integer value){
        this.op = op;
        this.ClientSeq = ClientSeq;
        this.key = key;
        this.value = value;
    }

    public Op(String op, String key, Integer value){
        this.op = op;
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        assert(obj instanceof Op);
        Op other = (Op) obj;
        boolean sameKey = (op.equals(other.op) && key.equals(other.key));
        if (op.equals("put"))
            return sameKey && value.equals(other.value);
        else
            return sameKey;
    }
}
