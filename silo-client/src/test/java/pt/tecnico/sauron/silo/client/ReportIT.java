package pt.tecnico.sauron.silo.client;

import com.google.protobuf.util.Timestamps;
import static io.grpc.Status.INVALID_ARGUMENT;
import io.grpc.StatusRuntimeException;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

import pt.tecnico.sauron.silo.grpc.SiloOuterClass.CompleteObservation;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Observation;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Camera;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.InitRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.ReportRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TrackRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TrackResponse;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.ObjectType;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Coordinates;

import java.io.IOException;

public class ReportIT extends BaseIT {

  private static SiloServerFrontend frontend;

  private static InitRequest initRequest;
  private static Coordinates.Builder coord = Coordinates.newBuilder().setLatitude(37.750898f).setLongitude(-25.6448817f);
  private static Camera cam = Camera.newBuilder().setName("camara1").setCoordinates(coord.build()).build();
  private static List<Observation> observations = new ArrayList<>();

  // one-time initialization and clean-up
  @BeforeAll
  public static void oneTimeSetUp() {
    try{
      frontend = new SiloServerFrontend(testProps.getProperty("zoo.host"), testProps.getProperty("zoo.port"), 1);
    } catch (IOException e){
      System.err.println(e.getMessage());
      System.exit(1);
    }

    Observation observation = Observation.newBuilder().setType(ObjectType.PERSON)
        .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli())).setIdentifier("1").build();
    CompleteObservation comObs = CompleteObservation.newBuilder().setCam(cam).setObservation(observation).build();
    initRequest = InitRequest.newBuilder().setData(comObs).build();
  }

  @AfterAll
  public static void oneTimeTearDown() {
  }

  // initialization and clean-up for each test

  @BeforeEach
  public void setUp() {
    frontend.ctrlInit(initRequest);
  }

  @AfterEach
  public void tearDown() {
    observations.clear();
    frontend.ctrlClear();
  }

  // tests

  @Test
  public void ReportTestOK_person() {
    Observation observation = Observation.newBuilder().setType(ObjectType.PERSON).setIdentifier("2").build();
    observations.add(observation);

    ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(cam.getName()).addAllObservations(observations);
    frontend.report(reportRequest);

    TrackRequest.Builder trackRequest = TrackRequest.newBuilder().setType(ObjectType.PERSON).setIdentifier("2");
    TrackResponse trackResponse = frontend.track(trackRequest);

    assertEquals(observation.getType(), trackResponse.getObservation(0).getObservation().getType());
    assertEquals(observation.getIdentifier(), trackResponse.getObservation(0).getObservation().getIdentifier());
  }

  @Test
  public void ReportTestOK_car() {
    Observation observation = Observation.newBuilder().setType(ObjectType.CAR).setIdentifier("11AA11").build();
    observations.add(observation);

    ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(cam.getName()).addAllObservations(observations);
    frontend.report(reportRequest);

    TrackRequest.Builder trackRequest = TrackRequest.newBuilder().setType(ObjectType.CAR).setIdentifier("11AA11");
    TrackResponse trackResponse = frontend.track(trackRequest);

    assertEquals(observation.getType(), trackResponse.getObservation(0).getObservation().getType());
    assertEquals(observation.getIdentifier(), trackResponse.getObservation(0).getObservation().getIdentifier());
  }

  @Test
  public void ReportTestOK_multipleObs() {
    Observation observation = Observation.newBuilder().setType(ObjectType.CAR).setIdentifier("11AA11").build();

    Observation observation1 = Observation.newBuilder().setType(ObjectType.CAR).setIdentifier("22AA11").build();

    observations.add(observation);
    observations.add(observation1);

    ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(cam.getName()).addAllObservations(observations);
    frontend.report(reportRequest);

    TrackRequest.Builder trackRequest = TrackRequest.newBuilder().setType(ObjectType.CAR).setIdentifier("11AA11");
    TrackResponse trackResponse = frontend.track(trackRequest);
    assertEquals(observation.getType(), trackResponse.getObservation(0).getObservation().getType());
    assertEquals(observation.getIdentifier(), trackResponse.getObservation(0).getObservation().getIdentifier());

    TrackRequest.Builder trackRequest1 = TrackRequest.newBuilder().setType(ObjectType.CAR).setIdentifier("22AA11");
    TrackResponse trackResponse1 = frontend.track(trackRequest1);
    assertEquals(observation1.getType(), trackResponse1.getObservation(0).getObservation().getType());
    assertEquals(observation1.getIdentifier(), trackResponse1.getObservation(0).getObservation().getIdentifier());
  }

  @Test
  public void ReportTestNOK_unsignedCamera() {
    // Failure test where the camera in the Report Request isn't registred yet.
    Coordinates coord1 = Coordinates.newBuilder().setLatitude(1f).setLongitude(1f).build();
    Camera cam1 = Camera.newBuilder().setName("camara2").setCoordinates(coord1).build();

    Observation observation = Observation.newBuilder().setType(ObjectType.CAR).setIdentifier("11AA11").build();
    observations.add(observation);

    ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(cam1.getName()).addAllObservations(observations);

    assertEquals(INVALID_ARGUMENT.getCode(),
        assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest)).getStatus().getCode());

  }

  @Test
  public void ReportTestNOK_noCamera() {
    Observation observation = Observation.newBuilder().setType(ObjectType.CAR).setIdentifier("11AA11").build();
    observations.add(observation);

    ReportRequest.Builder reportRequest = ReportRequest.newBuilder().addAllObservations(observations);

    assertEquals(INVALID_ARGUMENT.getCode(),
        assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest)).getStatus().getCode());
  }

  @Test
  public void ReportTestNOK_noType() {
    Observation observation = Observation.newBuilder().setIdentifier("11AA11").build();
    observations.add(observation);

    ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(cam.getName()).addAllObservations(observations);

    assertEquals(INVALID_ARGUMENT.getCode(),
        assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest)).getStatus().getCode());
  }

  @Test
  public void ReportTestNOK_noId() {
    Observation observation = Observation.newBuilder().setType(ObjectType.CAR).build();
    observations.add(observation);

    ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(cam.getName()).addAllObservations(observations);

    assertEquals(INVALID_ARGUMENT.getCode(),
        assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest)).getStatus().getCode());
  }

  @Test
  public void ReportTestNOK_wrongPersonId() {
    // This test tries to report two person obervations with an id that includes
    // letters

    Observation observation = Observation.newBuilder().setType(ObjectType.PERSON).setIdentifier("123aaa").build();
    observations.add(observation);

    ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(cam.getName()).addAllObservations(observations);

    assertEquals(INVALID_ARGUMENT.getCode(),
        assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest)).getStatus().getCode());

  }

  @Test
  public void ReportTestNOK_onlyLettersCarId() {
    // This test tries to report two car obervations with an id composed only by
    // letters

    Observation observation = Observation.newBuilder().setType(ObjectType.CAR).setIdentifier("AABBAA").build();
    observations.add(observation);

    ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(cam.getName()).addAllObservations(observations);

    assertEquals(INVALID_ARGUMENT.getCode(),
        assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest)).getStatus().getCode());

  }

  @Test
  public void ReportTestNOK_onlyNumbersCarId() {
    // This test tries to report two car obervations with an id composed only by
    // numbers

    Observation observation = Observation.newBuilder().setType(ObjectType.CAR).setIdentifier("112211").build();
    observations.add(observation);

    ReportRequest.Builder reportRequest = ReportRequest.newBuilder().setName(cam.getName()).addAllObservations(observations);
    assertEquals(INVALID_ARGUMENT.getCode(),
        assertThrows(StatusRuntimeException.class, () -> frontend.report(reportRequest)).getStatus().getCode());
  }
}