package pt.tecnico.sauron.silo;

import pt.tecnico.sauron.silo.domain.exceptions.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import pt.tecnico.sauron.silo.domain.*;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Camera;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Coordinates;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.GossipUpdate;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.ObjectType;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.ReportedObject;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.ReportedObjects;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.ReportedObservation;

/**
 * Bridge between server domain and gRPC domain. Checks and transforms gRPC Objects into domain objects
 */
public class PersistentObjectFactory {
    // Pattern 1 is 4 letters and 2 numbers. AA00AA, AAAA00 and 00AAAA
    private static final String PATTERN1 = "([A-Z]{2}[0-9]{2}[A-Z]{2}|[A-Z]{4}[0-9]{2}|[0-9]{2}[A-Z]{4})";
    // Pattern 2 is 2 letters and 4 numbers. 00AA00, AA0000 and 0000AA
    private static final String PATTERN2 = "([0-9]{2}[A-Z]{2}[0-9]{2}|[A-Z]{2}[0-9]{4}|[0-9]{4}[A-Z]{2})";

    /**
     * Builds a domain object from gRPC data
     * @param type gRPC ObjectType
     * @param sId ID of object
     * @return an silo-domain object
     * @see PersistentObjectFactory#convertType(ObjectType)
     * @see PersistentObjectFactory#convertType(pt.tecnico.sauron.silo.domain.PersistentObject.ObjectType)
     * @see PersistentObjectFactory#parseCarId(String)
     */
    public PersistentObject getObject(ObjectType type, String sId) {
        if (type == ObjectType.PERSON) {
            try {
                // If it fits a long, it is a long
                Long.parseLong(sId);
                return new PersistentObject(sId, PersistentObject.ObjectType.PERSON);
            } catch (NumberFormatException e) {
                throw new SiloException(ErrorMessage.PERSON_ID_FORMAT, sId);
            }
        } else if (type == ObjectType.CAR) {
            parseCarId(sId);
            return new PersistentObject(sId, PersistentObject.ObjectType.CAR);
        } else {
            throw new SiloException(ErrorMessage.INVALID_TYPE, type.toString());
        }
    }

    /**
     * Checks if a given id is compatible with a car id 
     * @param id a string to be evaluated
     * @throws e a custom SiloException
     */
    private void parseCarId(String id) {

        if (id.length() != 6 || (!id.matches(PATTERN1) && !id.matches(PATTERN2))) {
            throw new SiloException(ErrorMessage.CAR_ID_FORMAT, id);
        }
    }

    /**
     * Builds a silo-domain object type from a gRPC object type
     * @param type gRPC object type
     * @return the corresponding Silo-domain object type
     */
    public PersistentObject.ObjectType convert(ObjectType type) {
        switch (type) {
            case PERSON:
                return PersistentObject.ObjectType.PERSON;
            case CAR:
                return PersistentObject.ObjectType.CAR;
            default:
                return null;
        }
    }

    /**
     * Builds a gRPC object type from a silo-domain object type
     * @param type silo-domain object type
     * @return  the corresponding gRPC object type
     */
    public ObjectType convert(PersistentObject.ObjectType type) {
        switch (type) {
            case PERSON:
                return ObjectType.PERSON;
            case CAR:
                return ObjectType.CAR;
            default:
                return null;
        }
    }

    public Camera convert(PersistentCamera c){
        Coordinates coord = Coordinates.newBuilder().setLatitude(c.getLatitude()).setLongitude(c.getLongitude()).build();
        return Camera.newBuilder().setCoordinates(coord).setName(c.getName()).build();
    }

    public PersistentCamera convert(Camera c){
       return new PersistentCamera(c.getName(), c.getCoordinates().getLatitude(),c.getCoordinates().getLongitude());
    }

    public Timestamp convert(Instant t){
        return Timestamps.fromMillis(t.toEpochMilli());
    }

    public Instant convert (Timestamp t){
        return Instant.ofEpochSecond(Timestamps.toSeconds(t));
    }

    public ReportedObservation convert(PersistentObservation o){
        Camera c = convert(o.getCamera());
        Timestamp ts = convert(o.getTimestamp());
        return ReportedObservation.newBuilder().setTimestamp(ts).setCamera(c).build();
    }

    public PersistentObservation convert(ReportedObservation o){
        PersistentCamera c = convert(o.getCamera());
        Instant i = convert(o.getTimestamp());
        return new PersistentObservation(i, c);
    }

    public ReportedObject convert(PersistentObject o){
        List<ReportedObservation> observations = new ArrayList<>();
        for (PersistentObservation obs : o.getObservations() ){
            observations.add(convert(obs));
        }
        return ReportedObject.newBuilder().addAllObservations(observations).setType(convert(o.getType())).setId(o.getId()).build();
    }

    public PersistentObject convert(ReportedObject o){
        PersistentObject res = new  PersistentObject(o.getId(), convert(o.getType()));
        for( ReportedObservation obs: o.getObservationsList()){
            res.addObservation(convert(obs));
        }
        return res;
    }


    public GossipUpdate convert(List<GossipTarget> target, int replicaID,int updateID){
        GossipUpdate.Builder gossip = GossipUpdate.newBuilder();
        if(target.size() == 1 && target.get(0) instanceof PersistentCamera){
            gossip.setCamera(convert((PersistentCamera)target.get(0)));
        }
    
        else{
            List<ReportedObject> objs = new ArrayList<>();
            for(GossipTarget t: target){
                objs.add(convert((PersistentObject)t));
            }
            gossip.setObjects(ReportedObjects.newBuilder().addAllObjects(objs).build());
        }
        return gossip.setRid(replicaID).setUid(updateID).build();
    }

}
