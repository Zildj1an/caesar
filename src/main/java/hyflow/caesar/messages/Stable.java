package hyflow.caesar.messages;

import hyflow.common.Request;
import hyflow.common.RequestId;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class Stable extends Message {
    private static final long serialVersionUID = 1L;

    private final Request request;
    private final RequestId requestId;
    private final int[] objectIds;
    private final List<RequestId> pred;
    private final long position;
    private final byte[] payload;

    public Stable(Request request) {
        this.request = request;
        this.requestId = request.getRequestId();
        this.objectIds = request.getObjectIds();
        this.pred = request.getPred();
        this.position = request.getPosition();
        this.payload = request.getPayload();
    }

    public Stable(DataInputStream input) throws IOException {
        super(input);
        requestId = new RequestId(input.readInt(), input.readInt());

        int oLen = input.readInt();
        objectIds = new int[oLen];
        for (int i=0;i<oLen;i++) {
            objectIds[i] = input.readInt();
        }

        int pLen = input.readInt();
        pred = new ArrayList<>(pLen);
        while (--pLen >= 0)
            pred.add(new RequestId(input));

        position = input.readLong();
        payload = new byte[input.readInt()];
        input.readFully(payload);

        request = new Request(requestId, objectIds, payload);
        request.setPosition(position);
    }

    public MessageType getType() {
        return MessageType.Propose;
    }

    public Request getRequest() {
        return request;
    }

    public int byteSize() {
        return super.byteSize() + requestId.byteSize() + 4 +
                (4 * objectIds.length) + 8 + 4 + payload.length;
    }

    public String toString() {
        return "Stable(" + super.toString() + ")";
    }

    protected void write(ByteBuffer bb) {
        requestId.writeTo(bb);

        bb.putInt(objectIds.length);
        for(int oId : objectIds)
            bb.putInt(oId);

        bb.putInt(pred.size());
        for (RequestId rId : pred) {
            rId.writeTo(bb);
        }

        bb.putLong(position);
        bb.putInt(payload.length);
        bb.put(payload);
    }
}