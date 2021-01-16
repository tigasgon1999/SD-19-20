package pt.tecnico.sauron.spotter;

import java.io.IOException;
import pt.tecnico.sauron.silo.client.SiloServerFrontend;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;


public class SpotterApp {

  public static void main(String[] args) throws ZKNamingException, IOException{
    System.out.println(SpotterApp.class.getSimpleName());

    // receive and print arguments
    System.out.printf("Received %d arguments%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("arg[%d] = %s%n", i, args[i]);
    }
    if (args.length < 3 || args.length > 4) {
      System.err.println("Wrong number of args");
    }

    final String zooHost = args[0];
    final String zooPort = args[1];
    final int nRep = Integer.parseInt(args[2]);

    SiloServerFrontend frontend;

    try{
      // If instance number is given
      if(args.length == 4){
          frontend = new SiloServerFrontend(zooHost, zooPort,nRep, args[3]);
        } 
      // If not, choose random instance
      else{
          frontend = new SiloServerFrontend(zooHost, zooPort, nRep);
      }

      Spotter spotter = new Spotter(frontend);
      spotter.serve();

    } catch (IOException e){
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
