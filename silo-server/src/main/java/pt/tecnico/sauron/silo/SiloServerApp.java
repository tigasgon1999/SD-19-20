package pt.tecnico.sauron.silo;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.Scanner;
import java.util.Timer;

import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class SiloServerApp {

  public static void main(String[] args)
      throws IOException, InterruptedException, ZKNamingException {
    System.out.println(SiloServerApp.class.getSimpleName());

    // receive and print arguments
    System.out.printf("Received %d arguments%n", args.length);
    for (int i = 0; i < args.length; i++) {
      System.out.printf("arg[%d] = %s%n", i, args[i]);
    }

    // check arguments
    if (args.length < 6 || args.length > 7) {
      System.err.println("Argument(s) missing!");
      System.err.printf("Usage: java %s zooHost zooPort instanceNumber host port nReplicas [gossipInterval]%n", SiloServerApp.class.getName());
      return;
    }

    final String BASEPATH = "/grpc/sauron/silo";
    final String zooHost = args[0];
		final String zooPort = args[1];
		final String instance = args[2];
		final String host = args[3];
    final String port = args[4];
    final int nReplicas = Integer.parseInt(args[5]);
    final long gossipTime;
    if(args.length == 7){
      gossipTime = Long.parseLong(args[6]);
    }else{
      gossipTime = 30;
    }

    final BindableService impl;
    ZKNaming zkNaming = null;
    GossipService gossip = null;
    final String path = BASEPATH + "/" + instance;

    try {

      zkNaming = new ZKNaming(zooHost, zooPort);
      // publish
      zkNaming.rebind(path, host, port);

      gossip = new GossipService(zkNaming, Integer.parseInt(instance));

      impl = new SiloServerImpl(nReplicas, gossip, Integer.parseInt(instance));
      // Create a new server to listen on port
      Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService(impl).build();

      // Start the server
      server.start();

      // create timer object as daemon
      Timer timer = new Timer(true);

      // create timer task object
      MyTimerTask ttask = new MyTimerTask(impl);

      if(gossipTime > 0){
        // schedule timer task periodically
        timer.schedule(ttask, gossipTime*1000 , gossipTime*1000);
      }

      // Server threads are running in the background.
      System.out.printf("Server started on port %s\n", port);
      new Thread(() -> {
        System.out.println("<Press enter to shutdown>");
        new Scanner(System.in).nextLine();

  
        timer.cancel();
        ttask.cancel();
        server.shutdown();
      }).start();

      // Do not exit the main thread. Wait until server is terminated.
      server.awaitTermination();
    } finally {
      if (zkNaming != null) {
        // remove
        zkNaming.unbind(path,host,port);
      }
    }
  }
}
