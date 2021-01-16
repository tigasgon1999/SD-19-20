package pt.tecnico.sauron.silo.client;

import static io.grpc.Status.INVALID_ARGUMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.*;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.PingRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.PingResponse;

import java.io.IOException;

public class PingIT extends BaseIT {

  private static SiloServerFrontend frontend;
  // one-time initialization and clean-up
  @BeforeAll
  public static void oneTimeSetUp() {
    try{
      frontend = new SiloServerFrontend(testProps.getProperty("zoo.host"), testProps.getProperty("zoo.port"), 1);
    }catch(IOException e){
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  @AfterAll
  public static void oneTimeTearDown() {
  }

  // initialization and clean-up for each test

  @BeforeEach
  public void setUp() {
  }

  @AfterEach
  public void tearDown() {
  }

  @Test
  public void testPingOK() {
    PingRequest request = PingRequest.newBuilder().setMessage("friend").build();
    PingResponse response = frontend.ctrlPing(request);
    assertEquals("Hello friend!", response.getMessage());
  }

  @Test
  public void testPingNOK() {
    PingRequest request = PingRequest.newBuilder().setMessage("").build();
    assertEquals(INVALID_ARGUMENT.getCode(),
        assertThrows(StatusRuntimeException.class, () -> frontend.ctrlPing(request)).getStatus().getCode());
  }
}
