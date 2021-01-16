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
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TraceRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TraceResponse;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Coordinates;

import java.io.IOException;

public class TraceIT extends BaseIT {

    private static final Instant inst = Instant.now();
    private static final Timestamp now = Timestamps.fromMillis(inst.toEpochMilli());
    private static final Timestamp after = Timestamps.fromMillis(inst.plusSeconds(100).toEpochMilli());
    private static final Timestamp later = Timestamps.fromMillis(inst.plusSeconds(10000).toEpochMilli());
    private static final Observation PERSON_1_1 = Observation.newBuilder().setIdentifier("000000000").setTimestamp(now)
            .setType(ObjectType.PERSON).build();
    private static final Observation PERSON_1_2 = Observation.newBuilder().setIdentifier("000000000")
            .setTimestamp(after).setType(ObjectType.PERSON).build();
    private static final Observation PERSON_1_3 = Observation.newBuilder().setIdentifier("000000000")
            .setTimestamp(later).setType(ObjectType.PERSON).build();
    private static final Observation PERSON_2_1 = Observation.newBuilder().setIdentifier("000000001").setTimestamp(now)
            .setType(ObjectType.PERSON).build();
    private static final Observation PERSON_2_2 = Observation.newBuilder().setIdentifier("000000001")
            .setTimestamp(after).setType(ObjectType.PERSON).build();
    private static final Observation PERSON_2_3 = Observation.newBuilder().setIdentifier("000000001")
            .setTimestamp(later).setType(ObjectType.PERSON).build();
    private static final Observation CAR_1_1 = Observation.newBuilder().setIdentifier("ABCD12").setTimestamp(now)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_1_2 = Observation.newBuilder().setIdentifier("ABCD12").setTimestamp(after)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_1_3 = Observation.newBuilder().setIdentifier("ABCD12").setTimestamp(later)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_2_1 = Observation.newBuilder().setIdentifier("AB12CD").setTimestamp(now)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_2_2 = Observation.newBuilder().setIdentifier("AB12CD").setTimestamp(after)
            .setType(ObjectType.CAR).build();
    private static final Observation CAR_2_3 = Observation.newBuilder().setIdentifier("AB12CD").setTimestamp(later)
            .setType(ObjectType.CAR).build();
    private static final String WRONG_ID = "A";
    private static final Coordinates COORDS = Coordinates.newBuilder().setLatitude(1).setLongitude(1).build();
    private static final Camera CAM = Camera.newBuilder().setName("CAM").setCoordinates(COORDS).build();

    private static SiloServerFrontend frontend;


    // one-time initialization and clean-up

    @BeforeAll
    public static void oneTimeSetUp(){
        try{
          frontend = new SiloServerFrontend(testProps.getProperty("zoo.host"), testProps.getProperty("zoo.port"), 1);
        } catch (IOException e){
          System.err.println(e.getMessage());
          System.exit(1);
        }

        // Person 1
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(PERSON_1_1).build()).build());
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(PERSON_1_2).build()).build());
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(PERSON_1_3).build()).build());
        // Person 2
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(PERSON_2_1).build()).build());
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(PERSON_2_2).build()).build());
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(PERSON_2_3).build()).build());
        // Car 1
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_1_1).build()).build());
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_1_2).build()).build());
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_1_3).build()).build());
        // Car 2
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_2_1).build()).build());
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_2_2).build()).build());
        frontend.ctrlInit(InitRequest.newBuilder()
                .setData(CompleteObservation.newBuilder().setCam(CAM).setObservation(CAR_2_3).build()).build());
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
    public void testTracePersonOK() {
        TraceRequest.Builder request = TraceRequest.newBuilder().setIdentifier(PERSON_1_1.getIdentifier())
                .setType(PERSON_1_1.getType());
        TraceResponse response = frontend.trace(request);
        assertEquals(3, response.getObservationCount(), "tracePersonOK, getObservationCount");
        CompleteObservation o = response.getObservationList().get(0);
        assertEquals(CAM, o.getCam(), "tracePersonOK, camera");
        assertEquals(PERSON_1_3, o.getObservation(), "tracePersonOK, observation");
        o = response.getObservationList().get(1);
        assertEquals(CAM, o.getCam(), "tracePersonOK, camera");
        assertEquals(PERSON_1_2, o.getObservation(), "tracePersonOK, observation");
        o = response.getObservationList().get(2);
        assertEquals(CAM, o.getCam(), "tracePersonOK, camera");
        assertEquals(PERSON_1_1, o.getObservation(), "tracePersonOK, observation");
    }

    @Test
    public void testTracePersonEmpty() {
        TraceRequest.Builder request = TraceRequest.newBuilder().setIdentifier(WRONG_ID).setType(PERSON_1_1.getType());
        TraceResponse response = frontend.trace(request);
        assertEquals(0, response.getObservationCount(), "tracePersonEmpty, getObservationCount");
    }

    @Test
    public void testTraceCarOK() {
        TraceRequest.Builder request = TraceRequest.newBuilder().setIdentifier(CAR_1_1.getIdentifier())
                .setType(CAR_1_1.getType());
        TraceResponse response = frontend.trace(request);
        assertEquals(3, response.getObservationCount(), "tracePersonOK, getObservationCount");
        CompleteObservation o = response.getObservationList().get(0);
        assertEquals(CAM, o.getCam(), "tracePersonOK, camera");
        assertEquals(CAR_1_3, o.getObservation(), "tracePersonOK, observation");
        o = response.getObservationList().get(1);
        assertEquals(CAM, o.getCam(), "tracePersonOK, camera");
        assertEquals(CAR_1_2, o.getObservation(), "tracePersonOK, observation");
        o = response.getObservationList().get(2);
        assertEquals(CAM, o.getCam(), "tracePersonOK, camera");
        assertEquals(CAR_1_1, o.getObservation(), "tracePersonOK, observation");
    }

    @Test
    public void testTraceCarEmpty() {
        TraceRequest.Builder request = TraceRequest.newBuilder().setIdentifier(WRONG_ID).setType(CAR_1_1.getType());
        TraceResponse response = frontend.trace(request);
        assertEquals(0, response.getObservationCount(), "traceCarEmpty, getObservationCount");
    }

    // Negative results

    @Test
    public void testTraceWrongType() {
        TraceRequest.Builder request = TraceRequest.newBuilder().setIdentifier(CAR_1_1.getIdentifier())
                .setType(ObjectType.UNDEFINED);
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.trace(request)).getStatus().getCode());
    }

    @Test
    public void testTraceNullType() {
        TraceRequest.Builder request = TraceRequest.newBuilder().setIdentifier(CAR_1_1.getIdentifier());
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.trace(request)).getStatus().getCode());
    }

    @Test
    public void testTraceNullId() {
        TraceRequest.Builder request = TraceRequest.newBuilder().setType(ObjectType.CAR);
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.trace(request)).getStatus().getCode());
    }

    @Test
    public void testTraceEmptyId() {
        TraceRequest.Builder request = TraceRequest.newBuilder().setType(ObjectType.CAR).setIdentifier("");
        assertEquals(INVALID_ARGUMENT.getCode(),
                assertThrows(StatusRuntimeException.class, () -> frontend.trace(request)).getStatus().getCode());
    }
}