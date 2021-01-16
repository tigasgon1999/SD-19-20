package pt.tecnico.sauron.eye;

import java.io.IOException;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.tecnico.sauron.silo.client.SiloServerFrontend;


public class EyeApp {

	public static void main(String[] args) throws ZKNamingException, IOException {
		System.out.println(EyeApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		if (args.length <6 || args.length > 7){
			System.err.print("Wrong number of arguments!");
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String camName = args[2];
		final float latitude = Float.parseFloat(args[3]);
		final float longitude = Float.parseFloat(args[4]);
		final int nRep = Integer.parseInt(args[5]);

		SiloServerFrontend frontend;

		if((args[2].length() < 3) || (args[2].length() > 15) ){
			System.err.print("Camera name must be at lest 3 characters and at most 15 characters long.");
		}

		try{
			// If instance number is given
			if(args.length == 7){
					frontend = new SiloServerFrontend(zooHost, zooPort, nRep, args[6]);
			}
			// If not, choose random instance
			else{
					frontend = new SiloServerFrontend(zooHost, zooPort, nRep);
				}

			frontend.setCamera(camName, latitude, longitude);
			Eye eye = new Eye(frontend, camName, latitude, longitude);
			eye.watch();
		}catch (IOException e){
			System.err.println(e.getMessage());
			System.exit(1);
		}	
	}
	
}
