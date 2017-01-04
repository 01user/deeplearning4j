package org.nd4j.parameterserver.distributed.conf;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.parameterserver.distributed.enums.FaultToleranceStrategy;
import org.nd4j.parameterserver.distributed.enums.NodeRole;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic configuration pojo for VoidParameterServer
 * @author raver119@gmail.com
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
@Data
public class Configuration implements Serializable {
    private int streamId;
    private int unicastPort;
    private int multicastPort;
    private int numberOfShards;
    private FaultToleranceStrategy faultToleranceStrategy;
    private List<String> shardAddresses = new ArrayList<>();
    private List<String> backupAddresses = new ArrayList<>();

    // this is very important parameter
    private String networkMask;

    // This two values are optional, and have effect only for MulticastTransport
    private String multicastNetwork;
    private String multicastInterface;
    private int ttl = 4;
    protected NodeRole forcedRole;

    // FIXME: probably worth moving somewhere else
    // this part is specific to w2v
    private boolean useHS = true;
    private boolean useNS = false;

    public void setStreamId(int streamId) {
        if (streamId < 1 )
            throw new ND4JIllegalStateException("You can't use streamId 0, please specify other one");

        this.streamId = streamId;
    }


    public void setShardAddresses(List<String> addresses) {
        this.shardAddresses = addresses;
    }

    public void setShardAddresses(String... Ips) {
        shardAddresses = new ArrayList<>();

        for (String ip: Ips) {
            if (ip != null)
                shardAddresses.add(ip);
        }
    }

    public void setBackupAddresses(List<String> addresses) {
        this.backupAddresses = addresses;
    }

    public void setBackupAddresses(String... Ips) {
        backupAddresses = new ArrayList<>();

        for (String ip: Ips) {
            if (ip != null)
                backupAddresses.add(ip);
        }
    }
}
