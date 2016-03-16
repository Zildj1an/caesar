package hyflow.caesar;

import hyflow.caesar.messages.ProposeReply;
import hyflow.common.ProcessDescriptor;
import hyflow.common.Request;

import java.util.Arrays;

/**
 * Created by balajiarun on 3/14/16.
 */
public class ProposalReplyInfo {

    private final Request request;
    private final ProposeReply[] replies;

    private int count;
    private boolean done;

    private final int quorum;

    public ProposalReplyInfo(Request request) {
        this.request = request;

        int numReplicas = ProcessDescriptor.getInstance().numReplicas;
        replies = new ProposeReply[numReplicas];

        count = 0;
        done = false;

        int failures = (int) Math.floor(ProcessDescriptor.getInstance().numReplicas / 2.0) + 1;
        quorum = 2 * failures + 1;
    }

    public Request getRequest() {
        return request;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone() {
        done = true;
    }

    public void addReply(ProposeReply msg, int sender) {
        replies[sender] = msg;
        count++;
        request.getPred().addAll(msg.getPred());
    }

    public boolean isFastQuorum() {
        return (count >= quorum);
    }

    public boolean shouldRetry() {
        return Arrays.stream(replies).anyMatch((ProposeReply reply) ->
           reply.getStatus() == ProposeReply.Status.NACK
        );
    }
}