package pt.tecnico.sauron.silo;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.tecnico.sauron.silo.grpc.*;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.*;

import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;


public class GossipService {
  private ManagedChannel _channel;
  private SiloGrpc.SiloBlockingStub _stub;
  private ZKNaming zkNaming;
  private ZKRecord self;
  private static final String BASEPATH = "/grpc/sauron/silo";
  private Logger logger = Logger.getLogger(GossipService.class.getName());

  public GossipService(ZKNaming zk, int n) throws ZKNamingException{
    zkNaming =  zk; 
    self = zkNaming.lookup(BASEPATH + "/" + n);
  }

  /**
  * Connects to a random replica that is not itself.
  */
  private void connectRandom(){
    try{
      final List<ZKRecord> records = zkNaming.listRecords(BASEPATH).stream().filter(record -> !record.equals(self)).collect(Collectors.toList());
      if(records.size() > 0){
        final String path = records.get(new Random().nextInt(records.size())).getPath();
        final String target = zkNaming.lookup(path).getURI();
        _channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        _stub = SiloGrpc.newBlockingStub(_channel);
        logger.log(Level.INFO, "Connected to {0}" , path);
      }else{
        logger.warning("No other replicas found.");
        throw new StatusRuntimeException(Status.NOT_FOUND);
      }
    }catch(final ZKNamingException e){
      logger.log(Level.SEVERE, e.getMessage());
      throw new StatusRuntimeException(Status.FAILED_PRECONDITION);
    }
  }

  /**
   * Public gossip request access point. Hides counter logic from invoker.
   * 
   * @param request The gossip request to send.
   * @return the gossip response.
   * @see GossipService#gossip(GossipRequest, int)
   */
  public GossipResponse gossip(GossipRequest request){
    connectRandom();
    GossipResponse res = gossip(request, 0);
    _channel.shutdownNow();
    return res;
  }

  /**
   * Safe recursive implementation of gossip request. Tries 3 times or until an 
   * UNAVAILABLE code is received. Then, connects to another replica 
   * and starts over.
   * 
   * @param request the gossip request to send.
   * @param c repetition counter.
   * @return the gossip response.
   */
  private GossipResponse gossip(GossipRequest request, int c){
    if(c == 3){
      connectRandom();
      return gossip(request,0);
    }
    try{
      return _stub.withDeadlineAfter(2, TimeUnit.SECONDS).gossip(request);

    } catch(final StatusRuntimeException e){
      if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED)
        // Deadline, try again
        return gossip(request, c+1);
      else if(e.getStatus().getCode() == Status.Code.UNAVAILABLE){
        // Server down, connect to another node and restart count
        logger.warning("Replica is down");
        connectRandom();
        return gossip(request, 0);
      }else{
        // Unknown error, stop process
        logger.severe(e.getMessage());
        throw e;
      }
    }
  }
}