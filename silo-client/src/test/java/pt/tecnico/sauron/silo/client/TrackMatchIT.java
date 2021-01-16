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
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TrackMatchResponse;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Coordinates;

import java.io.IOException;

public class TrackMatchIT extends BaseIT {

    private static final Timestamp now = Timestamps.fromMillis(Instant.now().toEpochMilli());
    private static final Observation PERSON_1 = Observation.newBuilder().setIdentifier("000000011").setTimestamp(now)
            .setType(ObjectType.PERSON).build();
    private static final Observation PERSON_2 = Observation.newBuilder().setIdentifier("000000001").setTimestamp(now)
            .setType(ObjectType.PERSON).build();
    private static final Observation CAR_1 = Observation.newBuilder().setIdentifier("ABCD12").setTimestamp(now)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_2 = Observation.newBuilder().setIdentifier("AB12CD").setTimestamp(now)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_3 = Observation.newBuilder().setIdentifier("12ABCD").setTimestamp(now)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_4 = Observation.newBuilder().setIdentifier("1234AB").setTimestamp(now)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_5 = Observation.newBuilder().setIdentifier("12AB34").setTimestamp(now)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_6 = Observation.newBuilder().setIdentifier("AB1234").setTimestamp(now)
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
              .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_3).build()).build());
      frontend.ctrlInit(InitRequest.newBuilder()
              .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_4).build()).build());
      frontend.ctrlInit(InitRequest.newBuilder()
              .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_5).build()).build());
      frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_6).build()).build());
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
    public void testTrackMatch1PersonOK() {
        TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setIdentifier("00000001.*").setType(PERSON_1.getType());
        TrackMatchResponse response = frontend.trackMatch(request);
        assertEquals(1, response.getObservationCount(), "trackMatchPersonOK, getObservationCount");
        CompleteObservation o = response.getObservationList().get(0);
        assertEquals(CAM, o.getCam(), "trackMatchPersonOK, camera");
        assertEquals(PERSON_1, o.getObservation(), "trackMatch1PersonOK, observation");
    }

    @Test
    public void testTrackMatch2PersonOK() {
        TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setIdentifier("0000000.*").setType(PERSON_1.getType());
        TrackMatchResponse response = frontend.trackMatch(request);
        assertEquals(2, response.getObservationCount(), "trackMatch2PersonOK, getObservationCount");
    }

    @Test
    public void testTrackMatchPersonEmpty() {
        TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setIdentifier(WRONG_ID).setType(PERSON_1.getType());
        TrackMatchResponse response = frontend.trackMatch(request);
        assertEquals(0, response.getObservationCount(), "trackMatchPersonEmpty, getObservationCount");
    }

    @Test
    public void testTrackMatch1CarOK() {
        TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setIdentifier("ABCD.*").setType(CAR_1.getType());
        TrackMatchResponse response = frontend.trackMatch(request);
        assertEquals(1, response.getObservationCount(), "trackMatchCarOK, getObservationCount");
        CompleteObservation o = response.getObservationList().get(0);
        assertEquals(CAM, o.getCam(), "trackMatchCarOK, camera");
        assertEquals(CAR_1, o.getObservation(), "trackMatch1CarOK, observation");
    }

    @Test
    public void testTrackMatch3CarOK() {
        TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setIdentifier("AB.*").setType(CAR_1.getType());
        TrackMatchResponse response = frontend.trackMatch(request);
        assertEquals(3, response.getObservationCount(), "trackMatch3CarOK, getObservationCount");
    }

    @Test
    public void testTrackMatch6CarOK() {
        TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setIdentifier(".*").setType(CAR_1.getType());
        TrackMatchResponse response = frontend.trackMatch(request);
        assertEquals(6, response.getObservationCount(), "trackMatch6CarOK, getObservationCount");
    }

    @Test
    public void testTrackMatchCarEmpty() {
        TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setIdentifier(WRONG_ID).setType(CAR_1.getType());
        TrackMatchResponse response = frontend.trackMatch(request);
        assertEquals(0, response.getObservationCount(), "trackMatchCarEmpty, getObservationCount");
    }

    // Negative results

    @Test
    public void testTrackMatchWrongType() {
        TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setIdentifier(CAR_1.getIdentifier())
                .setType(ObjectType.UNDEFINED);
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(request)).getStatus().getCode());
    }

    @Test
    public void testTrackMatchNullType() {
        TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setIdentifier(CAR_1.getIdentifier());
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(request)).getStatus().getCode());
    }

    @Test
    public void testTrackMatchNullId() {
        TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setType(ObjectType.CAR);
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(request)).getStatus().getCode());
    }

    @Test
    public void testTrackMatchEmptyId() {
        TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setType(ObjectType.CAR).setIdentifier("");
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(request)).getStatus().getCode());
    }
}
