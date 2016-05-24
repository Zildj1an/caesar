package hyflow.benchmark.kv;

import hyflow.benchmark.AbstractService;
import hyflow.common.ProcessDescriptor;
import hyflow.common.Request;
import hyflow.common.RequestId;
import hyflow.transaction.SharedObjectRegistry;
import hyflow.transaction.TransactionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by balaji on 4/25/16.
 */
public class KeyValue extends AbstractService {

    private static final Logger logger = LogManager.getLogger(KeyValue.class);

    private static final int INITIAL_VALUE = 1000;
    private final int size;
    private final SharedObjectRegistry registry;
    private final Random random = new Random();

    public KeyValue(String fileName) throws IOException {
        super(fileName);

        size = Integer.parseInt(configuration.getProperty("size", "1000"));
        registry = new SharedObjectRegistry(size);

        for (int id = 0; id < this.size; id++) {
            Value val = new Value(id, INITIAL_VALUE);
            this.registry.registerObjects(id, val);
        }

    }

    private int getValue(int key) {
        Value object = (Value) registry.getObject(key);
        return object.getValue();
    }

    private int putValue(int key, int value) {
        Value object = (Value) registry.getObject(key);
        int prevValue = object.getValue();
        object.setValue(value);
        return prevValue;
    }

    @Override
    public Request createRequest(RequestId rId, boolean read, int accessMode, int batchSize, int numReplicas) {
        final int MIN_PAYLOAD_SIZE = 6;

        Request request;
        byte[] payload = new byte[MIN_PAYLOAD_SIZE + batchSize * 8];
        int[] objectId = new int[batchSize];
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        int key, value;

        if (read) {
            buffer.put((byte) TransactionType.ReadOnlyTransaction.ordinal());
            buffer.put((byte) OpType.Get.ordinal());
        } else {
            buffer.put((byte) TransactionType.ReadWriteTransaction.ordinal());
            buffer.put((byte) OpType.Put.ordinal());
        }

        buffer.putInt(batchSize);

        for (int i = 0; i < batchSize; i++) {

            switch (accessMode) {

                case 0:
                    key = rId.getSeqNumber() % ProcessDescriptor.getInstance().conflictPool;
                    objectId[i] = key;
                    break;

                case 1:

                    key = rId.getClientId() + (rId.getSeqNumber() * numReplicas) + ProcessDescriptor.getInstance().conflictPool + 1;
                    objectId[i] = key;
                    break;

//            case 2:
//
//                int access = size / clientCount;
//                key = random.nextInt(access) + (access * rId.getClientId());
//                objectId[0] = key;
//                break;

                default:
                    key = random.nextInt(this.size);
                    objectId[i] = key;
            }

            value = random.nextInt(INITIAL_VALUE);

            buffer.putInt(key);
            buffer.putInt(value);
        }

        buffer.flip();

        request = new Request(rId, objectId, payload);
        return request;
    }

    @Override
    public void executeRequest(Request request) {
        byte[] payload = request.getPayload();
        ByteBuffer buffer = ByteBuffer.wrap(payload);

        buffer.get(); // TransactionType
        OpType command = OpType.values()[buffer.get()];

        final int batchSize = buffer.getInt();

        for (int i = 0; i < batchSize; i++) {
            final int key = buffer.getInt();
            final int value = buffer.getInt();

            if (command == OpType.Put)
                putValue(key, value);
            else
                getValue(key);
        }
    }

    @Override
    public int getTotalObjects() {
        return size;
    }

    private enum OpType {
        Get,
        Put
    }
}
