package pt.tecnico.sauron.spotter;

import java.time.Instant;
import java.util.List;
import java.util.Scanner;

import com.google.protobuf.util.Timestamps;

import io.grpc.StatusRuntimeException;
import pt.tecnico.sauron.silo.client.SiloServerFrontend;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Camera;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.CompleteObservation;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Coordinates;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.InitRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.ObjectType;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Observation;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.PingRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.PingResponse;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TraceRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TraceResponse;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TrackMatchResponse;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TrackRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.TrackResponse;

public class Spotter {

    private SiloServerFrontend frontend;

    public Spotter(SiloServerFrontend frontend) { this.frontend = frontend; }

    /**
     * Main function of the spotter app. Reads input from System.in
     * and redirects accordingly
     * 
     * @see Spotter#spot(String[])
     * @see Spotter#trail(String[])
     * @see Spotter#help(String[])
     * @see Spotter#ping(String[])
     * @see Spotter#init(String[])
     * @see Spotter#clear(String[])
     */
    public void serve() {
        try (Scanner scanner = new Scanner(System.in)) {
            String input;
            System.out.print(">>");
            while (!(input = scanner.nextLine()).equals("quit")) {

                String[] values = input.split(" ", 0);
                if (values.length > 0 && !values[0].equals("")) {
                    String command = values[0];

                    if (command.equals("spot")) {
                        spot(values);
                    } else if (command.equals("trail")) {
                        trail(values);
                    } else if (command.equals("help")) {
                        help(values);
                    } else if (command.equals("ping")) {
                        ping(values);
                    } else if (command.equals("init")) {
                        init(values);
                    } else if (command.equals("clear")) {
                        clear(values);
                    } else {
                        panic("Command not found.");
                    }
                } else {
                    panic("Empty command.");
                }
                System.out.print(">>");
            }
            System.out.println("Goodbye!");
            frontend.shutdown();
        }
    }

    /**
     * Prints an error to std out
     *
     * @param msg A message to be displayed.
     */
    private void panic(String msg) {
        System.err.println("ERROR: " + msg + " Type \"help\" to show usage");
    }

    /**
     * Sends initial information to server. Used for testing purposes
     * @param args data to send to server. Order is `camName, latitude, longitude, objectType, objectId`
     */
    private void init(String[] args) {
        if (args.length < 6) {
            panic("Init command: Not enough args.");
            return;
        } else if (args.length > 6) {
            panic("Init command: Too many args");
            return;
        }

        // Build camera
        String camName = args[1];
        float latitude = Float.parseFloat(args[2]);
        float longitude = Float.parseFloat(args[3]);
        Camera c = Camera.newBuilder().setName(camName).setCoordinates(Coordinates.newBuilder().setLatitude(latitude).setLongitude(longitude).build()).build();

        // Build phony observation
        ObjectType type = getType(args[4]);
        if (type == ObjectType.UNRECOGNIZED) {
            panic("Init command: No object of type " + args[1]);
            return;
        }
        String id = args[5];
        Observation obs = Observation.newBuilder().setIdentifier(id).setType(type)
                .setTimestamp(Timestamps.fromMillis(Instant.now().toEpochMilli())).build();

        CompleteObservation data = CompleteObservation.newBuilder().setCam(c).setObservation(obs).build();

        // Request and response
        InitRequest request = InitRequest.newBuilder().setData(data).build();
        try {
            frontend.ctrlInit(request);
        } catch (StatusRuntimeException e) {
            panic("Init command: Server responded with " + e.getMessage());
        }
    }

    /**
     * Clears information on server. Used for testing purposes
     * 
     * @param args array with a single string, which is the name of the command
     */
    private void clear(String[] args) {
        if (args.length != 1) {
            panic("Clear command: Too many args.");
        } else {
            try {
                frontend.ctrlClear();
            } catch (StatusRuntimeException e) {
                panic("Clear command: Server responded with " + e.getMessage());
            }
        }
    }


    /**
     * Health check request for server
     * @param args array with two strings: Name of command and message to send
     */
    private void ping(String[] args) {
        if (args.length > 2)
            panic("Ping command: Too many args.");
        else if (args.length < 2)
            panic("Ping command: Not enough args.");
        else {
            try {
                PingRequest request = PingRequest.newBuilder().setMessage(args[1]).build();
                PingResponse response = frontend.ctrlPing(request);
                System.out.println(response.getMessage());
            } catch (StatusRuntimeException e) {
                panic("Ping command: Server responded with " + e.getMessage());
            }
        }
    }


    /**
     * Help message, showcasing usage of the spotter app
     * 
     * @param args array with a string, which is the name of the command
     */
    private void help(String[] args) {
        if (args.length != 1)
            panic("Help command: Too many args.");
        else
            System.out.println("SpotterApp: Usage\n" + "help                            Displays this menu.\n"
                    + "ping    <msg>                   Health check of server. Should respond with \"Hello <msg>!\".\n"
                    + "clear                           Clears all server state.\n"
                    + "init    <args>                  Sends initial control information to server.\n"
                    + "spot    <type> <identifier>     Returns information on all objects of <type> that match <identifier>.\n"
                    + "trail   <type> <identifier>     Returns complete information on object of <type> that exactly matches <identifier>.\n");

    }

    /**
     * toString on a list of complete observations
     * @param obs list of observations to be transformed to a string
     * @return formatted string version of given list
     */
    private String extractObservations(List<CompleteObservation> obs) {
        String res = "";
        for (CompleteObservation o : obs) {

            res += o.getObservation().getType().name().toLowerCase() + "," + o.getObservation().getIdentifier() + ","
                    + Instant.ofEpochSecond(Timestamps.toSeconds(o.getObservation().getTimestamp())).toString() + ","
                    + o.getCam().getName() + "," + o.getCam().getCoordinates().getLatitude() + "," + o.getCam().getCoordinates().getLongitude() + "\n";
        }

        return res;
    }

    /**
     * Implements the spot command. Can be used for exact or partial id match
     * @param args array of string containing name of method, object type and object id
     * @see Spotter#track(String[])
     * @see Spotter#trackMatch(String[])
     */
    private void spot(String[] args) {
        if (args.length < 3)
            panic("Spot command: Not enough args.");
        else if (args.length > 3)
            panic("Spot command: Too many args.");
        else {
            if (args[2].contains("*")) {
                // Replaces all `*` with RegEx equivalent `.*`. Allows for multiple `*`.
                args[2] = args[2].replace("*", ".*");
                trackMatch(args);
            } else {
                track(args);
            }
        }
    }

    /**
     * Transforms inputed object types into gRPC ObjectTypes
     * @param type string representation of an object type
     * @return the correct object type
     */
    private ObjectType getType(String type) {
        if (type.equals("person"))
            return ObjectType.PERSON;
        else if (type.equals("car"))
            return ObjectType.CAR;
        else
            return ObjectType.UNDEFINED;
    }

    /**
     * Handler for partial id spot commmand. Deals with frontend communication and prints result
     * @param args array of strings with name of method (spot, in this case), objectType and partial id
     * @see Spotter#extractObservations(List)
     * @see Spotter#spot(String[])
     */
    private void trackMatch(String[] args) {
        ObjectType type = getType(args[1]);
        if (type == ObjectType.UNDEFINED) {
            panic("Spot command: No object of type " + args[1]);
            return;
        }
        try {
            TrackMatchRequest.Builder request = TrackMatchRequest.newBuilder().setType(type).setIdentifier(args[2]);
            TrackMatchResponse response = frontend.trackMatch(request);
            System.out.println(extractObservations(response.getObservationList()));
        } catch (StatusRuntimeException e) {
            panic("Spot command: Server responded with " + e.getMessage());
        }
    }

    /**
     * Handler for exact id spot commmand. Deals with frontend communication and prints result
     * @param args array of strings with name of method (spot, in this case), objectType and exact id
     * @see Spotter#extractObservations(List)
     * @see Spotter#spot(String[])
     */
    private void track(String[] args) {
        ObjectType type = getType(args[1]);
        if (type == ObjectType.UNDEFINED) {
            panic("Spot command: No object of type " + args[1]);
            return;
        }
        try {
            TrackRequest.Builder request = TrackRequest.newBuilder().setType(type).setIdentifier(args[2]);
            TrackResponse response = frontend.track(request);
            System.out.println(extractObservations(response.getObservationList()));
        } catch (StatusRuntimeException e) {
            panic("Spot command: Server responded with " + e.getMessage());
        }
    }

    /**
     * Handler for trail spot commmand. Deals with frontend communication and prints result
     * @param args array of strings with name of method, objectType and id
     * @see Spotter#extractObservations(List)
     */
    private void trail(String[] args) {
        if (args.length < 3)
            panic("Spot command: Not enough args.");
        else if (args.length > 3)
            panic("Spot command: Too many args.");
        else {
            ObjectType type = getType(args[1]);
            if (type == ObjectType.UNDEFINED) {
                panic("Spot command: No object of type " + args[1]);
                return;
            }
            try {
                TraceRequest.Builder request = TraceRequest.newBuilder().setType(type).setIdentifier(args[2]);
                TraceResponse response = frontend.trace(request);
                System.out.println(extractObservations(response.getObservationList()));
            } catch (StatusRuntimeException e) {
                panic("Spot command: Server responded with " + e.getMessage());
            }
        }
    }
}