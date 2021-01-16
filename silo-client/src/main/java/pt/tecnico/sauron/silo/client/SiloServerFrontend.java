package pt.tecnico.sauron.silo.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.grpc.*;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.*;
import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.Empty;

/**
 * Server communication class. Hides all distributed logic from those who use it.
 */
public class SiloServerFrontend {

  // Base path for zookeeper node structure
  private static final String BASEPATH = "/grpc/sauron/silo";
  private final ZKNaming zkNaming;

  // Grpc channel and stub
  private ManagedChannel _channel;
  private SiloGrpc.SiloBlockingStub _stub;

  // Timestamp and cache
  private final List<Integer> _currentTimestamp;
  private final SiloFrontendCache cache = new SiloFrontendCache(32);

  // Camera for re-login and single-instance flag
  private Camera cam;
  private final boolean isLocked;

  private int sessionId;
  private String lockedPath;

  // Logger, handler and messages
  private final Logger logger = Logger.getLogger(SiloServerFrontend.class.getName());
  private static final String SERVER_DOWN = "Replica is down. Trying another one";
  private static final String FAILED_3_TIMES= "Failed 3 times. Reconnecting";

  /**
   * Constructor for connecting to a specific replica.
   * Mainly used for test purposes, as all operations will
   * fail if specified replica is unavailable.
   *
   * @param zooHost ZooKeeper host name.
   * @param zooPort ZooKeeper port number.
   * @param idReplica replicaId to connect to.
   * @throws ZKNamingException
   * @throws IOException
   */
  public SiloServerFrontend(final String zooHost, final String zooPort, int nRep, final String idReplica) throws ZKNamingException, IOException{
    initLogger();
    isLocked = true;
    this.zkNaming = new ZKNaming(zooHost,zooPort);
    lockedPath = BASEPATH + "/" + idReplica;
    _currentTimestamp = new ArrayList<>(Collections.nCopies(nRep, 0));
    connectLocked();
  }

  /**
   * Main constructor. Will connect to a random replica.
   * All operations will connect to another replica and retry
   * operation if replica is down or silent.
   *
   * @param zooHost ZooKeeper host name.
   * @param zooPort ZooKeeper port number.
   * @throws IOException
   */
  public SiloServerFrontend(final String zooHost, final String zooPort, int nRep) throws IOException{
    initLogger();
    _currentTimestamp = new ArrayList<>(Collections.nCopies(nRep, 0));
    this.isLocked = false;
    this.zkNaming = new ZKNaming(zooHost,zooPort);
    this.connectRandom();
  }

  /**
   * Initializes a logger.
   *
   * @throws IOException
   */

  private void initLogger() throws IOException{
    logger.setUseParentHandlers(false);
    String fileName = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss").format(LocalDateTime.now()) + ".log";
    FileHandler file = new FileHandler(fileName, true);
    SimpleFormatter formatter = new SimpleFormatter();
    file.setFormatter(formatter);
    logger.addHandler(file);
  }

  /**
   * Initializes a camera. Necessary for loggin in to
   * multiple replicas without app interaction.
   *
   * @param name Camera name.
   * @param latitude Camera latitude.
   * @param longitude Camera longitude.
   */
  public void setCamera(final String name, final float latitude, final float longitude){
    this.cam = Camera.newBuilder().setName(name).setCoordinates(Coordinates.newBuilder().setLongitude(longitude).setLatitude(latitude).build()).build();
  }


  /**
   * Connects to a random replica.
   * Will shutdown previous channel.
   */
  private void connectRandom(){
    try{
      if(_channel != null){
        _channel.shutdownNow();
      }
    final Collection<ZKRecord> records = zkNaming.listRecords(BASEPATH);
    final String path = records.toArray(ZKRecord[]::new)[new Random().nextInt(records.size())].getPath();
    final String target = zkNaming.lookup(path).getURI();
    _channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    _stub = SiloGrpc.newBlockingStub(_channel);
    logger.log(Level.INFO, ()->"Connected to " + path + " on " + target);
    }catch(ZKNamingException e){
      logger.severe(e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Connects to a random replica, loggin in with saved camera immediately after.
   *
   * @see SiloServerFrontend#connectRandom()
   * @see SiloServerFrontend#camJoin(CamJoinRequest.Builder, int)
   */
  private void connectRandomCam(){
    final CamJoinRequest.Builder request = CamJoinRequest.newBuilder().setCam(this.cam);
    connectRandom();
    camJoin(request, 0);
  }

  /**
   * Connects to locked replica.
   * Will shutdown previous channel.
   */
  private void connectLocked(){
    try{
      if(_channel != null){
        _channel.shutdownNow();
      }
      String target;
      target = zkNaming.lookup(lockedPath).getURI();

      logger.log(Level.INFO, "Lock connected to {0}", lockedPath);

      _channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
      _stub = SiloGrpc.newBlockingStub(_channel);
    }catch(ZKNamingException e){
      logger.severe(e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Connects to locked replica, loggin in with saved camera immediately after.
   *
   * @see SiloServerFrontend#connectLocked()
   * @see SiloServerFrontend#camJoin(CamJoinRequest.Builder, int)
   */
  private void connectLockedCam(){
    final CamJoinRequest.Builder request = CamJoinRequest.newBuilder().setCam(this.cam);
    connectLocked();
    camJoin(request, 0);
  }

  /**
   * Public ctrlPing access point. Hides counter logic from user.
   *
   * @param request a PingRequest
   * @return a pingResponse
   *
   * @see SiloServerFrontend#ctrlPing(PingRequest, int)
   */
  public PingResponse ctrlPing(final PingRequest request) {
    return ctrlPing(request, 0);
  }

  /**
   * Safe recursive implementation of ctrlPing. Tries 3 times or until an
   * UNAVAILABLE code is received. Then, connects to another replica
   * and starts over.
   *
   * @param request a PingRequest.
   * @param c repetition counter.
   * @return a PingResponse.
   *
   * @see SiloServerFrontend#connectRandom()
   */
  private PingResponse ctrlPing(final PingRequest request, final int c) {
    // If this is the 4th try, try another replica or end process, if locked.
    if(c == 3){
      if(!isLocked){
        logger.warning(FAILED_3_TIMES);
        connectRandom();
        return ctrlPing(request, 0);
      }else{
        throw new StatusRuntimeException(Status.UNAVAILABLE);
      }
    }

    try{

      return _stub.withDeadlineAfter(2, TimeUnit.SECONDS).ctrlPing(request);

    } catch(final StatusRuntimeException e){
      if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED)
        // Deadline, try again
        return ctrlPing(request, c+1);
      else if(e.getStatus().getCode() == Status.Code.UNAVAILABLE){
        logger.warning(SERVER_DOWN);
        if(!isLocked){
          // Server down, connect to another node and restart count
          connectRandom();
          return ctrlPing(request, 0);
        }else{
          // Server down, check if target changed
          connectLocked();
          return ctrlPing(request, c+1);
        }
      }else{
        // Unknown error, stop process
        logger.severe(e.getMessage());
        throw e;
      }
    }
  }

  /**
   * Public ctrlClear access point. Hides counter logic from user.
   *
   * @see SiloServerFrontend#ctrlClear(int)
   */
  public void ctrlClear() {
    ctrlClear(0);
  }

  /**
   * Safe recursive implementation of ctrlClear. Tries 3 times or until an
   * UNAVAILABLE code is received. Then, connects to another replica
   * and starts over.
   *
   * @param c repetition counter.
   *
   * @see SiloServerFrontend#connectRandom()
   */
  private void ctrlClear(final int c){
    // If this is the 4th try, try another replica or end process, if locked.
    if(c == 3){
      if(!isLocked){
        logger.warning(FAILED_3_TIMES);
        connectRandom();
        ctrlClear(0);
        return;
      }else{
        throw new StatusRuntimeException(Status.UNAVAILABLE);
      }
    }

    try{
      _stub.withDeadlineAfter(2, TimeUnit.SECONDS).ctrlClear(Empty.newBuilder().build());
    }catch(final StatusRuntimeException e){
      if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED)
        // Deadline, try again
        ctrlClear(c+1);
      else if(e.getStatus().getCode() == Status.Code.UNAVAILABLE){
        logger.warning(SERVER_DOWN);
        if(!isLocked){
        // Server down, connect to another node and restart count
          connectRandom();
          ctrlClear(0);
        }else{
          // Server down, check if target changed
          connectLocked();
          ctrlClear(c+1);
        }
      }else{
        // Unknown error, stop process
        logger.severe(e.getMessage());
        throw e;
      }
    }
  }

  /**
   * Public ctrlInit access point. Hides counter logic from user.
   *
   * @param request an InitRequest.
   *
   * @see SiloServerFrontend#ctrlInit(InitRequest, int)
   */
  public void ctrlInit(final InitRequest request) {
    ctrlInit(request, 0);
  }

  /**
   * Safe recursive implementation of ctrlInit. Tries 3 times or until an
   * UNAVAILABLE code is received. Then, connects to another replica
   * and starts over.
   *
   * @param request a InitRequest.
   * @param c repetition counter.
   *
   * @see SiloServerFrontend#connectRandom()
   */
  private void ctrlInit(final InitRequest request, final int c){
    // If this is the 4th try, try another replica or end process, if locked.
    if(c == 3){
      if(!isLocked){
        logger.warning(FAILED_3_TIMES);
        connectRandom();
        ctrlInit(request, 0);
        return;
      }else{
        throw new StatusRuntimeException(Status.UNAVAILABLE);
      }
    }

    try{
      _stub.withDeadlineAfter(2, TimeUnit.SECONDS).ctrlInit(request);

    } catch(final StatusRuntimeException e){
      if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED)
        // Deadline, try again
        ctrlInit(request, c+1);
      else if(e.getStatus().getCode() == Status.Code.UNAVAILABLE){
        logger.warning(SERVER_DOWN);
        if(!isLocked){
        // Server down, connect to another node and restart count
          connectRandom();
          ctrlInit(request, 0);
        }else{
          // Server down, check if target changed
          connectLocked();
          ctrlInit(request, c+1);
        }
      }else{
        // Unknown error, stop process
        logger.severe(e.getMessage());
        throw e;
      }
    }
  } 

  /**
   * Public trackMatch access point. Hides counter logic from user.
   *
   * @param request a TrackMatchRequest.Builder. Client timestamp will be added.
   * @return a TrackMatchResponse.
   *
   * @see SiloServerFrontend#trackMatch(TrackMatchRequest.Builder, int)
   */
  public TrackMatchResponse trackMatch(final TrackMatchRequest.Builder request) {
    return trackMatch(request, 0);
  }

  /**
   * Safe recursive implementation of trackMatch. Tries 3 times or until an
   * UNAVAILABLE code is received. Then, connects to another replica
   * and starts over.
   *
   * @param request a TrackMatchRequest.Builder. Client timestamp will be added.
   * @param c repetition counter.
   * @return a TrackMatchResponse.
   *
   * @see SiloServerFrontend#connectRandom()
   */
  private TrackMatchResponse trackMatch(final TrackMatchRequest.Builder request, final int c){
    // If this is the 4th try, try another replica or end process, if locked.
    if(c==3){
      if(!isLocked){
        logger.warning(FAILED_3_TIMES);
        connectRandom();
        return trackMatch(request, 0);
      }else{
        throw new StatusRuntimeException(Status.UNAVAILABLE);
      }
    }

    try{
      TrackMatchResponse res = _stub.withDeadlineAfter(2, TimeUnit.SECONDS).trackMatch(request.build());
      List<Integer> incomingTS = res.getTimestamp().getTimestampList();
      String cacheID = "trackMatch_" + request.getType() +"_"+request.getIdentifier();

      if(updateTimestamp(incomingTS) || !cache.hasData(cacheID)){
        // If response is up to date or is not but there is no saved information on request.
        logger.log(Level.INFO, () -> "Added " + cacheID+ " to cache.");
        cache.addData(cacheID, res);
      }
      else{
        // If response is outdated but we have it saved in cache
        logger.log(Level.WARNING, ()-> " Received outdated response. Current timestamp: " + _currentTimestamp +", incoming timstamp: " + incomingTS);
        logger.log(Level.INFO, () -> "Fecthing " + cacheID + " from cache.");
        res = (TrackMatchResponse)cache.getData(cacheID);
      }

      return res;
    } catch(final StatusRuntimeException e){
      if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED)
        // Deadline, try again
        return trackMatch(request, c+1);
      else if(e.getStatus().getCode() == Status.Code.UNAVAILABLE){
        logger.warning(SERVER_DOWN);
        if(!isLocked){
          // Server down, connect to another node and restart count
          connectRandom();
          return trackMatch(request, 0);
        }else{
          // Server down, check if target changed
          connectLocked();
          return trackMatch(request, c+1);
        }
      }else{
        // Unknown error, stop process
        logger.severe(e.getMessage());
        throw e;
      }
    }
  }

  /**
   * Public track access point. Hides counter logic from user.
   *
   * @param request a TrackRequest.Builder. Client timestamp will be added.
   * @return a TrackResponse.
   *
   * @see SiloServerFrontend#track(TrackRequest.Builder, int)
   */
  public TrackResponse track(final TrackRequest.Builder request) {
    return track(request, 0);
  }

  /**
   * Safe recursive implementation of track. Tries 3 times or until an
   * UNAVAILABLE code is received. Then, connects to another replica
   * and starts over.
   *
   * @param request a TrackRequest.Builder. Client timestamp will be added.
   * @param c repetition counter.
   * @return a TrackResponse.
   *
   * @see SiloServerFrontend#connectRandom()
   */
  private TrackResponse track(final TrackRequest.Builder request, final int c){
    // If this is the 4th try, try another replica or end process, if locked.
    if(c==3){
      if(!isLocked){
        logger.warning(FAILED_3_TIMES);
        connectRandom();
        return track(request, 0);
      }else{
        throw new StatusRuntimeException(Status.UNAVAILABLE);
      }
    }

    try{
      TrackResponse res = _stub.withDeadlineAfter(2, TimeUnit.SECONDS).track(request.build());
      List<Integer> incomingTS = res.getTimestamp().getTimestampList();
      String cacheID = "track_" + request.getType() +"_"+request.getIdentifier();

      if(updateTimestamp(incomingTS)|| !cache.hasData(cacheID)){
        // If response is up to date or is not but there is no saved information on request.
        logger.log(Level.INFO, ()->"Added "+ cacheID +" to cache.");
        cache.addData(cacheID, res);
      }
      else{
        // If response is outdated but we have it saved in cache
        logger.log(Level.WARNING, ()-> " Received outdated response. Current timestamp: " + _currentTimestamp +", incoming timstamp: " + incomingTS);
        logger.log(Level.INFO, ()->"Fecthing " + cacheID + " from cache.");
        res = (TrackResponse)cache.getData(cacheID);
      }

      return res;
    } catch(final StatusRuntimeException e){
      if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED)
        // Deadline, try again
        return track(request, c+1);
      else if(e.getStatus().getCode() == Status.Code.UNAVAILABLE){
        logger.warning(SERVER_DOWN);
        if(!isLocked){
          // Server down, connect to another node and restart count
          connectRandom();
          return track(request, 0);
        }else{
          // Server down, check if target changed
          connectLocked();
          return track(request, c+1);
        }
      }else{
        // Unknown error, stop process
        logger.severe(e.getMessage());
        throw e;
      }
    }
  }

  /**
   * Public trace access point. Hides counter logic from user.
   *
   * @param request a TraceRequest.Builder. Client timestamp will be added.
   * @return a TraceResponse.
   *
   * @see SiloServerFrontend#trace(TraceRequest.Builder, int)
   */
  public TraceResponse trace(final TraceRequest.Builder request) {
    return trace(request, 0);
  }

  /**
   * Safe recursive implementation of trace. Tries 3 times or until an
   * UNAVAILABLE code is received. Then, connects to another replica
   * and starts over.
   *
   * @param request a TraceRequest.Builder. Client timestamp will be added.
   * @param c repetition counter.
   * @return a TraceResponse.
   *
   * @see SiloServerFrontend#connectRandom()
   */
  private TraceResponse trace(final TraceRequest.Builder request, final int c){
    // If this is the 4th try, try another replica or end process, if locked.
    if(c==3){
      if(!isLocked){
        logger.warning(FAILED_3_TIMES);
        connectRandom();
        return trace(request, 0);
      }else{
        throw new StatusRuntimeException(Status.UNAVAILABLE);
      }
    }

    try{
      TraceResponse res = _stub.withDeadlineAfter(2, TimeUnit.SECONDS).trace(request.build());
      List<Integer> incomingTS = res.getTimestamp().getTimestampList();
      String cacheID = "trace_" + request.getType() +"_"+request.getIdentifier();

      if(updateTimestamp(incomingTS) || !cache.hasData(cacheID)){
        // If response is up to date or is not but there is no saved information on request.
        logger.log(Level.INFO, ()->"Added "+cacheID+" to cache.");
        cache.addData(cacheID, res);
      }
      else{
        // If response is outdated but we have it saved in cache
        logger.log(Level.WARNING, ()-> " Received outdated response. Current timestamp: " + _currentTimestamp +", incoming timstamp: " + incomingTS);
        logger.log(Level.INFO, ()->"Fecthing "+cacheID+" from cache.");
        res = (TraceResponse)cache.getData(cacheID);
      }

      return res;
    } catch(final StatusRuntimeException e){
      if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED)
        // Deadline, try again
        return trace(request, c+1);
      else if(e.getStatus().getCode() == Status.Code.UNAVAILABLE){
        logger.warning(SERVER_DOWN);
        if(!isLocked){
          // Server down, connect to another node and restart count
          connectRandom();
          return trace(request, 0);
        }else{
          // Server down, check if target changed
          connectLocked();
          return trace(request, c+1);
        }
      }else{
        // Unknown error, stop process
        logger.severe(e.getMessage());
        throw e;
      }
    }
  }

  /**
   * Public report access point. Hides counter logic from user.
   *
   * @param request a ReportRequest.Builder. Client timestamp will be added.
   *
   * @see SiloServerFrontend#report(ReportRequest.Builder, int)
   */
  public void report(final ReportRequest.Builder request) {
    report(request, 0);
  }

  /**
   * Safe recursive implementation of report. Tries 3 times or until an
   * UNAVAILABLE code is received. Then, connects to another replica, logs in
   * and starts over.
   *
   * @param request a reportRequest.Builder. Client timestamp will be added.
   * @param c repetition counter.
   *
   * @see SiloServerFrontend#connectRandomCam()
   */
  private void report(final ReportRequest.Builder request, final int c) {
    // If this is the 4th try, try another replica or end process, if locked.
    if(c == 3){
      if(!isLocked){
        logger.warning(FAILED_3_TIMES);
        connectRandomCam();
        report(request, 0);
        return;
      }else{
        throw new StatusRuntimeException(Status.UNAVAILABLE);
      }
    }

    try{
      // Create Timestamp
			Timestamp ts = Timestamp.newBuilder().addAllTimestamp(_currentTimestamp).build();
      ReportResponse res = _stub.withDeadlineAfter(2, TimeUnit.SECONDS).report(request.setTimestamp(ts).setClientId(sessionId).build());
      updateTimestamp(res.getTimestamp().getTimestampList());

    } catch(final StatusRuntimeException e){
      if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED)
        // Deadline, try again
        report(request, c+1);
      else if(e.getStatus().getCode() == Status.Code.UNAVAILABLE){
        logger.warning(SERVER_DOWN);
        if(!isLocked){
          // Server down, connect to another node and restart count
          connectRandomCam();
          report(request, 0);
        }else{
          // Server down, check if target changed
          connectLockedCam();
          report(request, c+1);
        }
      }else{
        // Unknown error, stop process
        logger.severe(e.getMessage());
        throw e;
      }
    }
  }

  /**
   * Public camInfo access point. Hides counter logic from user.
   *
   * @param request a camInfoRequest.Builder. Client timestamp will be added.
   * @return a camInfoResponse.
   *
   * @see SiloServerFrontend#camInfo(camInfoRequest.Builder, int)
   */
  public CamInfoResponse camInfo(final CamInfoRequest.Builder request){
    return camInfo(request, 0);
  }

  /**
   * Safe recursive implementation of camInfo. Tries 3 times or until an
   * UNAVAILABLE code is received. Then, connects to another replica
   * and starts over.
   *
   * @param request a CamInfoRequest.Builder. Client timestamp will be added.
   * @param c repetition counter.
   * @return a CamInfoResponse.
   *
   * @see SiloServerFrontend#connectRandom()
   */
  private CamInfoResponse camInfo(final CamInfoRequest.Builder request, final int c) {
    // If this is the 4th try, try another replica or end process, if locked.
    if(c == 3){
      if(!isLocked){
        logger.warning(FAILED_3_TIMES);
        connectRandom();
        return camInfo(request, 0);
      }else{
        throw new StatusRuntimeException(Status.UNAVAILABLE);
      }
    }

    try{
      CamInfoResponse res =  _stub.withDeadlineAfter(2, TimeUnit.SECONDS).camInfo(request.build());

      if(updateTimestamp(res.getTimestamp().getTimestampList()))
        cache.addData(request.toString(), res);
      else
        res = (CamInfoResponse)cache.getData(request.toString());

      return res;

    } catch(final StatusRuntimeException e){
      if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED)
        // Deadline, try again
        return camInfo(request, c+1);
      else if(e.getStatus().getCode() == Status.Code.UNAVAILABLE){
        logger.warning(SERVER_DOWN);
        if(!isLocked){
          // Server down, connect to another node and restart count
          connectRandom();
          return camInfo(request, 0);
        }else{
          // Server down, check if target changed
          connectLocked();
          return camInfo(request, c+1);
        }
      }else{
        // Unknown error, stop process
        logger.severe(e.getMessage());
        throw e;
      }
    }
  }

  /**
   * Public camJoin access point. Hides counter logic from user.
   *
   * @param request a CamJoinRequest.Builder. Client timestamp will be added.
   *
   * @see SiloServerFrontend#camJoin(CamJoinRequest.Builder, int)
   */
  public void camJoin(final CamJoinRequest.Builder request){
    camJoin(request, 0);
  }

  /**
   * Safe recursive implementation of camJoin. Tries 3 times or until an
   * UNAVAILABLE code is received. Then, connects to another replica
   * and starts over.
   *
   * @param request a CamJoinRequest.Builder. Client timestamp will be added.
   * @param c repetition counter.
   *
   * @see SiloServerFrontend#connectRandom()
   */
  public void camJoin(final CamJoinRequest.Builder request, final int c) {
    // If this is the 4th try, try another replica or end process, if locked.
    if(c == 3){
      if(!isLocked){
        logger.warning(FAILED_3_TIMES);
        connectRandom();
        camJoin(request, 0);
        return;
      }else{
        throw new StatusRuntimeException(Status.UNAVAILABLE);
      }
    }

    try{
      // Create Timestamp
      Timestamp ts = Timestamp.newBuilder().addAllTimestamp(_currentTimestamp).build();
      logger.log(Level.INFO, "Current timestamp: {0}", _currentTimestamp);
      CamJoinResponse res = _stub.withDeadlineAfter(2, TimeUnit.SECONDS).camJoin(request.setTimestamp(ts).build());
      sessionId = res.getClientId();
      updateTimestamp(res.getTimestamp().getTimestampList());

    } catch(final StatusRuntimeException e){
      if(e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED)
        // Deadline, try again
        camJoin(request, c+1);
      else if(e.getStatus().getCode() == Status.Code.UNAVAILABLE){
        logger.warning(SERVER_DOWN);
        if(!isLocked){
          // Server down, connect to another node and restart count
          connectRandom();
          camJoin(request, 0);
        }else{
          // Server down, check if target changed
          connectLocked();
          camJoin(request, c+1);
        }
      }else{
        // Unknown error, stop process
        logger.severe(e.getMessage());
        throw e;
      }
    }
  }

  /**
   * Shutdown frontend. Clears channel and closes logger handlers.
   */
  public void shutdown() {
    _channel.shutdownNow();
    for(final Handler h:logger.getHandlers()){
      h.close();
    }
  }

  /**
   * Updates client timestamp with a received timestamp.
   *
   * @param newTimestamp received timestamp.
   * @return true if newTimestamp >= currentTimestamp. False otherwise
   */
  private Boolean updateTimestamp(final List<Integer> newTimestamp){
    logger.log(Level.INFO, ()-> "Comparing current timestamp " + _currentTimestamp + "with " + newTimestamp);
    Boolean isPosterior = true;
    for (int i = 0; i < newTimestamp.size(); i++) {
      if (newTimestamp.get(i) >= _currentTimestamp.get(i))
        _currentTimestamp.set(i, newTimestamp.get(i));
      else
        isPosterior = false;
    }
    return isPosterior;
  }
}