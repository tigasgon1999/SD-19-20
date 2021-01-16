package pt.tecnico.sauron.silo.client;

import java.time.Instant;

import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.*;
import com.google.protobuf.util.Timestamps;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.InitRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Coordinates;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Camera;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Observation;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.CompleteObservation;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.ObjectType;

import java.io.IOException;

public class CamIT extends BaseIT {

	private static SiloServerFrontend frontend;
	private static InitRequest initRequest;
	private static final ObjectType INIT_TYPE = ObjectType.PERSON;
	private static final String INIT_IDENTIFIER = "1";
	private static final Instant INIT_TIMESTAMP = Instant.now();
	private static final String INIT_CAMERA_NAME = "Camera1";
	private static final float INIT_LATITUDE = 41.4115894F;
	private static final float INIT_LONGITUDE = 2.2243139F;
	private static final String CAMERA_NAME = "Camera2";
	private static final float LATITUDE = 0.0F;
	private static final float LONGITUDE = 0.0F;

	// one-time initialization and clean-up
	@BeforeAll
	public static void oneTimeSetUp() {
    try{
      frontend = new SiloServerFrontend(testProps.getProperty("zoo.host"), testProps.getProperty("zoo.port"), 1);
    }catch(IOException e){
      System.err.println(e.getMessage());
      System.exit(1);
    }

		// Create Observation
		Observation observation = Observation.newBuilder().setType(INIT_TYPE).setIdentifier(INIT_IDENTIFIER)
				.setTimestamp(Timestamps.fromMillis(INIT_TIMESTAMP.toEpochMilli())).build();

		// Create coordinates
		Coordinates coords = Coordinates.newBuilder().setLatitude(INIT_LATITUDE).setLongitude(INIT_LONGITUDE).build();
		// Create Camera
		Camera camera = Camera.newBuilder().setName(INIT_CAMERA_NAME).setCoordinates(coords).build(); 
		// Create CompleteObservation
		CompleteObservation completeObservation = CompleteObservation.newBuilder().setObservation(observation)
				.setCam(camera).build();
		// Add CompleteObservation to response
		initRequest = InitRequest.newBuilder().setData(completeObservation).build();
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
		frontend.ctrlClear();
	}

	@Test
	// Create camera and show its info
	public void createCameraAndShowInfo() {
		// Create Coordinates
		Coordinates coordinates = Coordinates.newBuilder().setLatitude(LATITUDE).setLongitude(LONGITUDE).build();
		// Create Camera
		Camera testCam = Camera.newBuilder().setName(CAMERA_NAME).setCoordinates(coordinates).build();
		CamJoinRequest.Builder camJoinRequest = CamJoinRequest.newBuilder().setCam(testCam);
		frontend.camJoin(camJoinRequest);

		CamInfoRequest.Builder camInfoRequest = CamInfoRequest.newBuilder().setName(CAMERA_NAME);
		CamInfoResponse response = frontend.camInfo(camInfoRequest);
		assertEquals(LATITUDE, response.getCoordinates().getLatitude());
		assertEquals(LONGITUDE, response.getCoordinates().getLongitude());
	}

	@Test
	// Create same camera twice and show its info
	public void createCameraTwiceAndShowInfo() {
		/// Create Coordinates
		Coordinates coordinates = Coordinates.newBuilder().setLatitude(LATITUDE).setLongitude(LONGITUDE).build();
		// Create Camera
		Camera testCam = Camera.newBuilder().setName(CAMERA_NAME).setCoordinates(coordinates).build();
		CamJoinRequest.Builder camJoinRequest = CamJoinRequest.newBuilder().setCam(testCam);
		frontend.camJoin(camJoinRequest);

		frontend.camJoin(camJoinRequest);

		CamInfoRequest.Builder camInfoRequest = CamInfoRequest.newBuilder().setName(CAMERA_NAME);
		CamInfoResponse response = frontend.camInfo(camInfoRequest);
		assertEquals(LATITUDE, response.getCoordinates().getLatitude());
		assertEquals(LONGITUDE, response.getCoordinates().getLongitude());
	}

	@Test
	// Create two cameras with same name but differente coordinates
	public void createCamerasWithSameName() {
		float latitude2 = 1.0F;
		float longitude2 = 1.0F;

		// Create Coordinates for Camera 1
		Coordinates coordinates1 = Coordinates.newBuilder().setLatitude(LATITUDE).setLongitude(LONGITUDE).build();
		// Create Camera 1
		Camera testCam1 = Camera.newBuilder().setName(CAMERA_NAME).setCoordinates(coordinates1)
				.build();
		CamJoinRequest.Builder camJoinRequest1 = CamJoinRequest.newBuilder().setCam(testCam1);
		frontend.camJoin(camJoinRequest1);

		// Create Coordinates for Camera 2
		Coordinates coordinates = Coordinates.newBuilder().setLatitude(latitude2).setLongitude(longitude2).build();
		// Create Camera 2
		Camera testCam2 = Camera.newBuilder().setName(CAMERA_NAME).setCoordinates(coordinates).build();
		CamJoinRequest.Builder camJoinRequest2 = CamJoinRequest.newBuilder().setCam(testCam2);

		assertEquals(INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(camJoinRequest2)).getStatus()
						.getCode());
	}

	@Test
	// Create camera with name smaller than 3 characters
	public void createCameraWithNameTooSmall() {
		// Create Coordinates
		Coordinates coordinates = Coordinates.newBuilder().setLatitude(LATITUDE).setLongitude(LONGITUDE).build();
		// Create Camera
		Camera testCam = Camera.newBuilder().setName("C").setCoordinates(coordinates).build();
		CamJoinRequest.Builder camJoinRequest = CamJoinRequest.newBuilder().setCam(testCam);

		assertEquals(INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(camJoinRequest)).getStatus()
						.getCode());
	}

	@Test
	// Create camera with name biggers than 15 characts
	public void createCameraWithNameTooBig() {
		// Create Coordinates
		Coordinates coordinates = Coordinates.newBuilder().setLatitude(LATITUDE).setLongitude(LONGITUDE).build();
		// Create Camera
		Camera testCam = Camera.newBuilder().setName("Camera22222222222222").setCoordinates(coordinates).build();
		CamJoinRequest.Builder camJoinRequest = CamJoinRequest.newBuilder().setCam(testCam);

		assertEquals(INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(camJoinRequest)).getStatus()
						.getCode());
	}

	@Test
	// Create camera with latitude smaller than -90
	public void createCameraWithLatitudeTooSmall() {
		float incorrectLatitude = -100.0F;

		// Create Coordinates
		Coordinates coordinates = Coordinates.newBuilder().setLatitude(incorrectLatitude).setLongitude(LONGITUDE).build();
		// Create Camera
		Camera testCam = Camera.newBuilder().setName(CAMERA_NAME).setCoordinates(coordinates)
				.build();
		CamJoinRequest.Builder camJoinRequest = CamJoinRequest.newBuilder().setCam(testCam);

		assertEquals(INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(camJoinRequest)).getStatus()
						.getCode());
	}

	@Test
	// Create camera with latitude smaller than 90
	public void createCameraWithLatitudeTooBig() {
		float incorrectLatitude = 100.0F;

		// Create Coordinates
		Coordinates coordinates = Coordinates.newBuilder().setLatitude(incorrectLatitude).setLongitude(LONGITUDE).build();
		// Create Camera
		Camera testCam = Camera.newBuilder().setName(CAMERA_NAME).setCoordinates(coordinates)
				.build();
		CamJoinRequest.Builder camJoinRequest = CamJoinRequest.newBuilder().setCam(testCam);

		assertEquals(INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(camJoinRequest)).getStatus()
						.getCode());
	}

	@Test
	// Create camera with longitude smaller than 180
	public void createCameraWithLongitudeTooSmall() {
		float incorrectLongitude = -190.0F;

		// Create Coordinates
		Coordinates coordinates = Coordinates.newBuilder().setLatitude(LATITUDE).setLongitude(incorrectLongitude).build();
		// Create Camera
		Camera testCam = Camera.newBuilder().setName(CAMERA_NAME).setCoordinates(coordinates)
				.build();
		CamJoinRequest.Builder camJoinRequest = CamJoinRequest.newBuilder().setCam(testCam);

		assertEquals(INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(camJoinRequest)).getStatus()
						.getCode());
	}

	@Test
	// Create camera with longitude bigger than 180
	public void createCameraWithLongitudeTooBig() {
		float incorrectLongitude = 190.0F;

		// Create Coordinates
		Coordinates coordinates = Coordinates.newBuilder().setLatitude(LATITUDE).setLongitude(incorrectLongitude).build();
		// Create Camera
		Camera testCam = Camera.newBuilder().setName(CAMERA_NAME).setCoordinates(coordinates)
				.build();
		CamJoinRequest.Builder camJoinRequest = CamJoinRequest.newBuilder().setCam(testCam);

		assertEquals(INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(camJoinRequest)).getStatus()
						.getCode());
	}

	@Test
	// Get info for inexistent camera
	public void getInfoForInexistentCamera() {
		CamInfoRequest.Builder camInfoRequest = CamInfoRequest.newBuilder().setName(CAMERA_NAME);

		assertEquals(NOT_FOUND.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camInfo(camInfoRequest)).getStatus()
						.getCode());
	}

	@Test
	// Create camera without setting camera name
	public void createCameraWithoutName() {
		// Create Coordinates
		Coordinates coordinates = Coordinates.newBuilder().setLatitude(LATITUDE).setLongitude(LONGITUDE).build();
		// Create Camera
		Camera testCam = Camera.newBuilder().setCoordinates(coordinates).build();
		CamJoinRequest.Builder camJoinRequest = CamJoinRequest.newBuilder().setCam(testCam);

		assertEquals(INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(camJoinRequest)).getStatus()
						.getCode());
	}

	@Test
	// Get info for without setting camera name
	public void getInfoWithoutCameraName() {
		CamInfoRequest.Builder camInfoRequest = CamInfoRequest.newBuilder();

		assertEquals(INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camInfo(camInfoRequest)).getStatus()
						.getCode());
	}
}