package pt.tecnico.sauron.silo;

import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.NOT_FOUND;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import com.google.protobuf.Empty;
import com.google.protobuf.util.Timestamps;

import pt.tecnico.sauron.silo.grpc.*;
import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.domain.exceptions.*;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.*;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

public class SiloServerImpl extends SiloGrpc.SiloImplBase {

	// Domain root
	private Silo silo = new Silo();

	private PersistentObjectFactory factory = new PersistentObjectFactory();

	private GossipService gossip;
	
	private final List<Integer> currentTimestamp;

	private Map<Integer, List<List<GossipTarget>>> updateLog = new HashMap<>(); // replica's id, replica's updates (gossipCamera or gossipObject)
	private Map<Integer, Integer> lastUpdates = new HashMap<>();

	Logger logger = Logger.getLogger(SiloServerImpl.class.getName());

	int nReplicas;

	int rID;

	public SiloServerImpl(int nRep, GossipService g, int id){
		nReplicas = nRep;
		gossip = g;
		rID = id;
		currentTimestamp = new ArrayList<>(Collections.nCopies(nRep, 0));
		for(int i=0; i<nRep; i++){
			updateLog.put(i, new ArrayList<>());
		}
	}

	@Override
	/**
	* Checks the client-server connection
	*
	* @param request
	* @param responseObserver 
	*/	
	public void ctrlPing(PingRequest request, StreamObserver<PingResponse> responseObserver) {

		String message = request.getMessage();

		if (message == null || message.isBlank()) {
			logger.severe("Empty message");
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Message cannot be empty!").asRuntimeException());
			return;
		}

		logger.info(message);

		String output = "Hello " + message + "!";
		PingResponse response = PingResponse.newBuilder().setMessage(output).build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	/**
	* Clears the server's current state
	*
	* @param e
	* @param responseObserver 
	*/	
	public void ctrlClear(Empty e, StreamObserver<Empty> responseObserver) {
		silo.clearState();
		logger.info("cleared");
		responseObserver.onNext(e);
		responseObserver.onCompleted();
	}

	@Override
	/**
	* Creates a dummy object used for testing
	*
	* @param request contains the object's type and identifier 
	* @param responseObserver 
	*/	
	public void ctrlInit(InitRequest request, StreamObserver<Empty> responseObserver) {

		Camera dataCam = request.getData().getCam();
		Observation dataObs = request.getData().getObservation();
		PersistentObject obj = null;

		try {
			obj = factory.getObject(dataObs.getType(), dataObs.getIdentifier());
		} catch (SiloException e) {
			logger.severe(e.getMessage());
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
			return;
		}

		// Create camera
		PersistentCamera c = factory.convert(dataCam);
		silo.addCameraUnchecked(c);
		// Create observation
		PersistentObservation obs = new PersistentObservation(
				Instant.ofEpochMilli(Timestamps.toMillis(dataObs.getTimestamp())), c);
		// Add observation to object
		obj.addObservation(obs);
		// Add object to the server
		silo.addObject(obj);

		logger.log(Level.INFO, "Inserted: {0}",obj);

		responseObserver.onNext(Empty.newBuilder().build());
		responseObserver.onCompleted();
	}

	@Override
	/**
	* Creates a trace response with a list of observations of the given object, order by reverse chronological order
	*
	* @param request contains the object's type and identifier 
	* @param responseObserver 
	*
	* @see SiloServerImpl#checkRequest(TraceRequest request)
	*/
	public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {
		StringBuilder bld = new StringBuilder();
		bld.append("Received `trace ").append(request.getType()).append(" ").append(request.getIdentifier()).append("`\n");
		List<PersistentObservation> obs = null;
		List<CompleteObservation> completeObs = new ArrayList<>();

		// Create Timestamp
		bld.append("Current timestamp: ").append(currentTimestamp).append("\n");
		Timestamp timestamp = Timestamp.newBuilder().addAllTimestamp(currentTimestamp).build();
		// Create TraceResponse
		TraceResponse.Builder response = TraceResponse.newBuilder().setTimestamp(timestamp);
		try {
			checkRequest(request);

			PersistentObject obj = silo.getObject(factory.convert(request.getType()), request.getIdentifier());

			if (obj != null) {

				obs = obj.getObservations();

				for (PersistentObservation o : obs) {
					// Create Observation
					Observation observation = Observation.newBuilder().setType(request.getType())
							.setIdentifier(request.getIdentifier())
							.setTimestamp(factory.convert(o.getTimestamp())).build();
					// Create Camera
					Camera camera = factory.convert(o.getCamera());
					// Create CompleteObservation
					CompleteObservation completeObservation = CompleteObservation.newBuilder()
							.setObservation(observation).setCam(camera).build();
					// Add CompleteObservation to list of complete observations
					completeObs.add(completeObservation);
				}

				bld.append("Traced object with id = ").append(obj.getId());
			}

			logger.info(bld.toString());
			response.addAllObservation(completeObs);
			responseObserver.onNext(response.build());
			responseObserver.onCompleted();
		} catch (SiloException e) {
			logger.severe(e.getMessage());
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	/**
	* Creates a track response with the most recent of observation of the given object
	*
	* @param request contains the object's type and identifier 
	* @param responseObserver
	*
	* @see SiloServerImpl#checkRequest(TrackRequest request)
	*/
	public void track(TrackRequest request, StreamObserver<TrackResponse> responseObserver) {
		StringBuilder bld = new StringBuilder();
		bld.append("Received `track ").append(request.getType()).append(" ").append(request.getIdentifier()).append("`\n");

		// Create Timestamp
		bld.append("Current timestamp: ").append(currentTimestamp).append("\n");
		Timestamp timestamp = Timestamp.newBuilder().addAllTimestamp(currentTimestamp).build();
		// Add CompleteObservation and Timestamp to response
		TrackResponse.Builder response = TrackResponse.newBuilder().setTimestamp(timestamp);
		try {
			checkRequest(request);

			PersistentObject obj = silo.getObject(factory.convert(request.getType()), request.getIdentifier());

			if (obj != null) {
				PersistentObservation o = obj.getLatestObservation();

				// Create Observation
				Observation observation = Observation.newBuilder().setType(request.getType())
						.setIdentifier(request.getIdentifier())
						.setTimestamp(factory.convert(o.getTimestamp())).build();

				// Create Camera
				Camera camera = factory.convert(o.getCamera());

				// Create CompleteObservation
				CompleteObservation completeObservation = CompleteObservation.newBuilder().setObservation(observation)
						.setCam(camera).build();
					
				// Add CompleteObservation and Timestamp to response
				response.addObservation(completeObservation);

				bld.append("Tracked object with id = ").append(obj.getId());
			}

			logger.info(bld.toString());
			responseObserver.onNext(response.build());
			responseObserver.onCompleted();
		} catch (SiloException e) {
			logger.severe(e.getMessage());
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	/**
	* Creates a track match  response with the most recent of observation of the given object
	*
	* @param request contains the object's type and part of its identifier 
	* @param responseObserver
	*
	* @see SiloServerImpl#checkRequest(TrackMatchRequest request)
	*/
	public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {
		StringBuilder bld = new StringBuilder();
		bld.append("Received `trackMatch ").append(request.getType()).append(" ").append(request.getIdentifier()).append("`\n");

		// Create Timestamp
		bld.append("Current timestamp: ").append(currentTimestamp).append("\n");
		Timestamp timestamp = Timestamp.newBuilder().addAllTimestamp(currentTimestamp).build();
		TrackMatchResponse.Builder response = TrackMatchResponse.newBuilder().setTimestamp(timestamp);
		try {
			checkRequest(request);

			List<PersistentObject> objs = silo.findByTypeAndId(factory.convert(request.getType()),
					request.getIdentifier());

			List<CompleteObservation> completeObs = new ArrayList<>();

			bld.append("Track objects with id = {\n");

			for (PersistentObject obj : objs) {
				PersistentObservation o = obj.getLatestObservation();

				// Create Observation
				Observation observation = Observation.newBuilder().setType(factory.convert(obj.getType()))
						.setIdentifier(obj.getId()).setTimestamp(factory.convert(o.getTimestamp()))
						.build();

				// Create Camera
				Camera camera = factory.convert(o.getCamera());

				// Create CompleteObservation
				CompleteObservation completeObservation = CompleteObservation.newBuilder().setObservation(observation)
						.setCam(camera).build();

				// Add CompleteObservation to list of complete observations
				completeObs.add(completeObservation);
				bld.append(obj.getId()).append(" ");
			}
			logger.info(bld.append("}").toString());
			

			// Create TrackMatchResponse
			response.addAllObservation(completeObs);
			responseObserver.onNext(response.build());
			responseObserver.onCompleted();
		} catch (SiloException e) {
			logger.severe(e.getMessage());
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	/**
	* Verifies correctness of the track request
	*
	* @param request contains the object's type and identifier 
	*/
	public void checkRequest(TrackRequest request) {
		if (request.getType() == null) {
			throw new SiloException(ErrorMessage.INVALID_TYPE, "null");
		} else if (request.getType() != ObjectType.PERSON && request.getType() != ObjectType.CAR) {
			throw new SiloException(ErrorMessage.INVALID_TYPE, request.getType().toString());
		}
		if (request.getIdentifier() == null || request.getIdentifier().isBlank()) {
			throw new SiloException(ErrorMessage.NULL_ID);
		}
	}

	/**
	* Verifies correctness of the track match request
	*
	* @param request contains the object's type and identifier 
	*/
	public void checkRequest(TrackMatchRequest request) {
		if (request.getType() == null) {
			throw new SiloException(ErrorMessage.INVALID_TYPE, "null");
		} else if (request.getType() != ObjectType.PERSON && request.getType() != ObjectType.CAR) {
			throw new SiloException(ErrorMessage.INVALID_TYPE, request.getType().toString());
		}
		if (request.getIdentifier() == null || request.getIdentifier().isBlank()) {
			throw new SiloException(ErrorMessage.NULL_ID);
		}
	}

	/**
	* Verifies correctness of the trace request
	*
	* @param request contains the object's type and identifier 
	*/
	public void checkRequest(TraceRequest request) {
		if (request.getType() == null) {
			throw new SiloException(ErrorMessage.INVALID_TYPE, "null");
		} else if (request.getType() != ObjectType.PERSON && request.getType() != ObjectType.CAR) {
			throw new SiloException(ErrorMessage.INVALID_TYPE, request.getType().toString());
		}
		if (request.getIdentifier() == null || request.getIdentifier().isBlank()) {
			throw new SiloException(ErrorMessage.NULL_ID);
		}
	}

  @Override
  /**
   * Records a stream of new observations into the server
   * 
   * @param request a gRPC message with the request data
   * @param responseObserver stream where response will be dumped
   */
	public void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {

		if (request.getName() == null || request.getName().isBlank()) {
			logger.warning("Received report with empty camera name.");
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Name cannot be empty!").asRuntimeException());

		} else {
			try {
        // Checks if camera exists
				if (!silo.hasCamera(request.getName())) {
					logger.log(Level.WARNING, ()-> "Received report from camera " + request.getName() + ", but camera does not exist.");
					responseObserver.onError(INVALID_ARGUMENT
							.withDescription("Camera" + request.getName() + "does not exists").asRuntimeException());
					return;
        }

        // Fetches camera
				PersistentCamera camera = silo.getCamera(request.getName());
				StringBuilder bld = new StringBuilder();
        
        // Builds a hash map with all received observations and objects
        // For each observations, adds observation to object and object to map, if not already there
				Map<String, PersistentObject> parsed = new HashMap<>();
				for (Observation o : request.getObservationsList()) {
					PersistentObservation po = new PersistentObservation(Instant.now(), camera);
					if (!parsed.containsKey(o.getIdentifier()))
						parsed.put(o.getIdentifier(), factory.getObject(o.getType(), o.getIdentifier()));
					parsed.get(o.getIdentifier()).addObservation(po);
				}
				
				bld.append("Received observations: ").append(parsed.values());

				if(update(parsed.values().stream().collect(Collectors.toList()), request.getTimestamp().getTimestamp(rID), request.getClientId())){
					// Records data into server
					silo.addObjects(parsed);
				}

				// Create Timestamp
				Timestamp timestamp = Timestamp.newBuilder().addAllTimestamp(currentTimestamp).build();
				// Create ReportResponse
				ReportResponse response = ReportResponse.newBuilder().setTimestamp(timestamp).build();
				logger.info(bld.toString());
				responseObserver.onNext(response);
				responseObserver.onCompleted();
			} catch (SiloException e) {
				logger.severe(e.getMessage());
				responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
			}
		}
  }
  
  /**
   * Logs in a previously registered camera or registers a new one
   * @param request contains the camera to login/register
   * @param responseObserver 
   * 
   * @see SiloServerImpl#checkCamFields(CamJoinRequest request)
   */
	@Override
	public void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver) {
		StringBuilder bld = new StringBuilder();
		try {
			checkCamFields(request);
			bld.append("Received timestamp: ").append(request.getTimestamp().getTimestampList()).append("\n");

			PersistentCamera cam = factory.convert(request.getCam());
			bld.append("Received cam: ").append(cam).append("\n");

			// create client id
			Integer clientId = lastUpdates.size() + 1;
			bld.append("Generated client id: ").append(clientId);
	
			if(silo.addCamera(cam)){
				update(Arrays.asList(cam), request.getTimestamp().getTimestampList().get(rID), clientId);
			}

			// Create Timestamp
			Timestamp timestamp = Timestamp.newBuilder().addAllTimestamp(currentTimestamp).build();
			// Create CamJoinResponse
			CamJoinResponse response = CamJoinResponse.newBuilder().setClientId(clientId).setTimestamp(timestamp).build(); 
			logger.info(bld.toString());
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (SiloException e) {
			logger.severe(e.getMessage());
			responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		}
	}

  /**
   * Receives a camera name and returns its coordinates, in case it exists
   * @param request contains the camera name
   * @param responseObserver
   */
	@Override
	public void camInfo(CamInfoRequest request, StreamObserver<CamInfoResponse> responseObserver) {

		String camName = request.getName();
		if (camName == null || camName.isBlank()) {
			responseObserver
					.onError(INVALID_ARGUMENT.withDescription("Camera name must not be empty!").asRuntimeException());
			return;
		}

		if (silo.hasCamera(camName)) {
			PersistentCamera cam = silo.getCamera(camName);
			Timestamp timestamp = Timestamp.newBuilder().addAllTimestamp(currentTimestamp).build();
			CamInfoResponse response = CamInfoResponse.newBuilder().setCoordinates(
					Coordinates.newBuilder().setLatitude(cam.getLatitude()).setLongitude(cam.getLongitude()).build())
					.setTimestamp(timestamp)
					.build();
			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} else {
			logger.severe("Camera does not exist");
			responseObserver.onError(NOT_FOUND.withDescription("Camera doesn't exist!").asRuntimeException());
		}
	}

  /**
	 * Verifies if the name and coordinates of a received camera are valid.
	 * @param request contains the camera whose name and coordinates will be validated.
	 */
	public void checkCamFields(CamJoinRequest request) {
		Camera reqCam = request.getCam();
		String reqCamName = reqCam.getName();
		float reqCamLat = reqCam.getCoordinates().getLatitude();
		float reqCamLong = reqCam.getCoordinates().getLongitude();

		if (reqCamName == null || reqCamName.isBlank())
			throw new SiloException(ErrorMessage.INVALID_CAM_NAME, reqCamName);

		if ((reqCamName.length() > 15) || (reqCamName.length() < 3))
			throw new SiloException(ErrorMessage.INVALID_CAM_NAME, reqCamName);

		if (reqCamLat > 90 || reqCamLat < -90)
			throw new SiloException(ErrorMessage.INVALID_CAM_LAT);

		if (reqCamLong > 180 || reqCamLong < -180)
			throw new SiloException(ErrorMessage.INVALID_CAM_LONG);
	}

	/**
	 * Gossip request handler. Compares incoming timestamp with self timestamp
	 * and replies with updates that sender does not yet have.
	 * 
	 * @param request GossipRequest, contains sender timestamp.
	 * @param responseObserver stream observer to where we write.
	 */
	@Override
	public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver){
		List<Integer> timestamp = request.getTimestamp().getTimestampList();
		List<GossipUpdate> updates = new ArrayList<>();

		// Each position in timestamp corresponds to a ReplicaID.
		for(int rid=0; rid<timestamp.size(); rid++){
			// The pair (rid, uid) identifies an update that the sender does not yet have
			for(int uid = timestamp.get(rid); uid < currentTimestamp.get(rid); uid++){
				updates.add(factory.convert(updateLog.get(rid).get(uid), rid, uid +1 ));
			}
		}

		GossipResponse response = GossipResponse.newBuilder().addAllUpdates(updates).build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	/**
	 * Sends a gossip request to another random replica.
	 * 
	 * @see GossipService#gossip(GossipRequest)
	 */
	public void sendGossip() {
		try{
			GossipResponse res = gossip.gossip(GossipRequest.newBuilder().setTimestamp(Timestamp.newBuilder().addAllTimestamp(this.currentTimestamp).build()).build());
			for (GossipUpdate u : res.getUpdatesList()){
				update(u);
			}
		}catch(StatusRuntimeException e){
			logger.severe(e.getMessage());
		}
	}

	/**
	 * Updates timestamp and update log after a client update.
	 * 
	 * @param updt Received update.
	 * @param Uid Client side update id of received update.
	 * @param Cid Client id.
	 * @return if the update is new (true) or repeated (false)
	 */
	private boolean update(List<GossipTarget> updt, int Uid, int Cid){
		if(lastUpdates.get(Cid)!= null && lastUpdates.get(Cid) >= Uid)
			return false;
		
		lastUpdates.put(Cid, Uid);
		
		currentTimestamp.set(rID, currentTimestamp.get(rID) + 1);

		updateLog.get(rID).add(updt);
		logger.log(Level.INFO, "New timestamp: {0}", currentTimestamp);
		return true;
	}

	/**
	 * Adds missing updates to Silo and update log after a gossip response.
	 * 
	 * @param updt Received update.
	 */
	private void update(GossipUpdate updt){
		StringBuilder bld = new StringBuilder();
		if(currentTimestamp.get(updt.getRid()) >= updt.getUid()){
			logger.info("Update already inserted.");
			return;
		}
		
		List<GossipTarget> update = new ArrayList<>();
		currentTimestamp.set(updt.getRid(), updt.getUid());
		if(updt.hasCamera()){
			PersistentCamera c = factory.convert(updt.getCamera()); 
			silo.addCamera(c);
			bld.append("Recevied cam_join update: ").append(c);
			update.add(c);
		}else if (updt.hasObjects()){
			for (ReportedObject o : updt.getObjects().getObjectsList()){
				PersistentObject obj = factory.convert(o);
				silo.addObject(obj);
				update.add(obj);
			}
			bld.append("Received report update: ").append(update);
		}
		updateLog.get(updt.getRid()).add(update);
		bld.append("\n").append("New timestamp: ").append(currentTimestamp);
		logger.info(bld.toString());

	}
}
