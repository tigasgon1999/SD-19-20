package pt.tecnico.sauron.silo.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import org.junit.jupiter.api.*;

import io.grpc.StatusRuntimeException;
import static io.grpc.Status.INVALID_ARGUMENT;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Camera;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.CompleteObservation;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.InitRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.ObjectType;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Observation;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TrackRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TrackResponse;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Coordinates;

import java.io.IOException;

public class TrackIT extends BaseIT {

    private static final Instant inst = Instant.now();
    private static final Timestamp now = Timestamps.fromMillis(inst.toEpochMilli());
    private static final Timestamp after = Timestamps.fromMillis(inst.plusSeconds(100).toEpochMilli());
    private static final Observation PERSON_1 = Observation.newBuilder().setIdentifier("000000000").setTimestamp(now)
            .setType(ObjectType.PERSON).build();
    private static final Observation PERSON_2 = Observation.newBuilder().setIdentifier("000000001").setTimestamp(now)
            .setType(ObjectType.PERSON).build();
    private static final Observation CAR_1 = Observation.newBuilder().setIdentifier("ABCD12").setTimestamp(now)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_2 = Observation.newBuilder().setIdentifier("AB12CD").setTimestamp(now)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_2_2 = Observation.newBuilder().setIdentifier("AB12CD").setTimestamp(after)
            .setType(ObjectType.CAR).build();
    private static final String WRONG_ID = "A";
    private static final Coordinates COORDS = Coordinates.newBuilder().setLatitude(1).setLongitude(1).build();
    private static final Camera CAM = Camera.newBuilder().setName("CAM").setCoordinates(COORDS).build();

    private static SiloServerFrontend frontend;
    // one-time initialization and clean-up

    @BeforeAll
    public static void oneTimeSetUp() {
      try{
        frontend = new SiloServerFrontend(testProps.getProperty("zoo.host"), testProps.getProperty("zoo.port"), 1);
      } catch (IOException e){
        System.err.println(e.getMessage());
        System.exit(1);
      }

      frontend.ctrlInit(InitRequest.newBuilder()
              .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(PERSON_1).build()).build());
      frontend.ctrlInit(InitRequest.newBuilder()
              .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(PERSON_2).build()).build());
      frontend.ctrlInit(InitRequest.newBuilder()
              .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_1).build()).build());
      frontend.ctrlInit(InitRequest.newBuilder()
              .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_2).build()).build());
      frontend.ctrlInit(InitRequest.newBuilder()
              .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_2_2).build()).build());
    }

    @AfterAll
    public static void oneTimeTearDown() {
        frontend.ctrlClear();
        frontend.shutdown();
    }

    // initialization and clean-up for each test

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    // Positive results

    @Test
    public void testTrackPersonOK() {
        TrackRequest.Builder request = TrackRequest.newBuilder().setIdentifier(PERSON_1.getIdentifier())
                .setType(PERSON_1.getType());
        TrackResponse response = frontend.track(request);
        assertEquals(1, response.getObservationCount(), "trackPersonOK, getObservationCount");
        CompleteObservation o = response.getObservationList().get(0);
        assertEquals(CAM, o.getCam(), "trackPersonOK, camera");
        assertEquals(PERSON_1, o.getObservation(), "trackPersonOK, observation");
    }

    @Test
    public void testTrackPersonEmpty() {
        TrackRequest.Builder request = TrackRequest.newBuilder().setIdentifier(WRONG_ID).setType(PERSON_1.getType());
        TrackResponse response = frontend.track(request);
        assertEquals(0, response.getObservationCount(), "trackPersonEmpty, getObservationCount");
    }

    @Test
    public void testTrackCarOK() {
        TrackRequest.Builder request = TrackRequest.newBuilder().setIdentifier(CAR_1.getIdentifier()).setType(CAR_1.getType());
        TrackResponse response = frontend.track(request);
        assertEquals(1, response.getObservationCount(), "trackCarOK, getObservationCount");
        CompleteObservation o = response.getObservationList().get(0);
        assertEquals(CAM, o.getCam(), "trackCarOK, camera");
        assertEquals(CAR_1, o.getObservation(), "trackCarOK, observation");
    }

    @Test
    public void testTrackCarEmpty() {
        TrackRequest.Builder request = TrackRequest.newBuilder().setIdentifier(WRONG_ID).setType(CAR_1.getType());
        TrackResponse response = frontend.track(request);
        assertEquals(0, response.getObservationCount(), "trackCarEmpty, getObservationCount");
    }

    @Test
    public void testMultipleObservations() {
        TrackRequest.Builder request = TrackRequest.newBuilder().setIdentifier(CAR_2.getIdentifier()).setType(CAR_2.getType());
        TrackResponse response = frontend.track(request);
        assertEquals(1, response.getObservationCount(), "trackCarOK, getObservationCount");
        CompleteObservation o = response.getObservationList().get(0);
        assertEquals(CAM, o.getCam(), "trackCarOK, camera");
        assertEquals(CAR_2_2, o.getObservation(), "trackCarOK, observation");
    }

    // Negative results

    @Test
    public void testTrackWrongType() {
        TrackRequest.Builder request = TrackRequest.newBuilder().setIdentifier(CAR_1.getIdentifier())
                .setType(ObjectType.UNDEFINED);
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.track(request)).getStatus().getCode());
    }

    @Test
    public void testTrackNullType() {
        TrackRequest.Builder request = TrackRequest.newBuilder().setIdentifier(CAR_1.getIdentifier());
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.track(request)).getStatus().getCode());
    }

    @Test
    public void testTrackNullId() {
        TrackRequest.Builder request = TrackRequest.newBuilder().setType(ObjectType.CAR);
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.track(request)).getStatus().getCode());
    }

    @Test
    public void testTrackEmptyId() {
        TrackRequest.Builder request = TrackRequest.newBuilder().setType(ObjectType.CAR).setIdentifier("");
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.track(request)).getStatus().getCode());
    }
}
