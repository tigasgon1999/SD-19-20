package pt.tecnico.sauron.silo.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.util.Properties;


public class BaseIT {

	private static final String TEST_PROP_FILE = "/test.properties";
	protected static Properties testProps;
	
	@BeforeAll
	public static void oneTimeSetup () throws IOException {
		testProps = new Properties();
		
		try {
			testProps.load(BaseIT.class.getResourceAsStream(TEST_PROP_FILE));
			System.out.println("Test properties:");
			System.out.println(testProps);
		}catch (IOException e) {
			final String msg = String.format("Could not load properties file {}", TEST_PROP_FILE);
			System.out.println(msg);
			throw e;
		}
	}
	
	@AfterAll
	public static void cleanup() {
		
	}

}
