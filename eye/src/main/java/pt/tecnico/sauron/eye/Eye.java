package pt.tecnico.sauron.eye;

import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

import io.grpc.StatusRuntimeException;

import pt.tecnico.sauron.silo.client.SiloServerFrontend;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Camera;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Coordinates;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.ObjectType;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.Observation;
import pt.tecnico.sauron.silo.grpc.SiloOuterClass.ReportRequest;

public class Eye {
    private Camera _camera;
    private SiloServerFrontend _frontend;
    private List<Observation> _unsentObservations;
    
    public Eye(SiloServerFrontend frontend, String camName, float latitude, float longitude) {
        _camera = Camera.newBuilder().setName(camName).setCoordinates(Coordinates.newBuilder().setLatitude(latitude).setLongitude(longitude).build()).build();
        _unsentObservations = new ArrayList<>();
        this._frontend = frontend; 
    }

    /**
    * Main function of the Eye app. The Eye is associated with a camera and registers its observations
    * Reads commands from System.in and proceeds accordingly
    *
    * @see Eye#sleep(String[] args)
    * @see Eye#saveObservation(String[] obs)
    * @see Eye#report()
    * @see Eye#login()
    */
    public void watch() {
        //The camera needs to login to silo server before it can 
        login();
        try (Scanner scanner = new Scanner(System.in)) {
            String input;
            System.out.print(">>");

            //Reads commands continuosly until the "quit" command.
            while (!(input = scanner.nextLine()).equals("quit")) {

                //On an empty line, all previously unsent observations are reported to the silo server
                if (input.equals("")) {                         
                    report();               
                } 
                else {
                    String[] values = input.split(",", 0);

                    if (values.length > 0) {
                        String command = values[0];
                        
                        if (command.charAt(0) == '#') {         //This starts a inline comment.
                            // Ignore the rest of the line
                        } else if (command.equals("zzz")) {     //This recognizes the sleep command
                            sleep(values);
                        } else if (command.equals("person")) {  //This recognizes the command to register a person observation
                            saveObservation(values);
                        } else if (command.equals("car")) {     //This recognizes the command to register a person observation
                            saveObservation(values);
                        } else {
                            panic("Object type not accepted.");
                        }
                    } else 
                        panic("Bad input!");
                }                
                System.out.print(">>");
            }
            //Before exiting, all previously unsent observations are reported to the silo server
            report();
            System.out.println("Goodbye!");
            _frontend.shutdown();

        } catch (StatusRuntimeException e) { // This will catch Exceptions when the Eye needs to be shut down. (i.e. in
                                             // the login)
            panic(e.getMessage());
            _frontend.shutdown();
        }
    }

    private void panic(String msg) {
        System.err.println("ERROR: " + msg);
    }

    /**
     * Pauses the Eye process for an amount of time
     * 
     * @param args the sleep command: 'zzz sleepTime'. sleepTime is in milliseconds
     */
    private void sleep(String[] args) {
        if (args.length == 2) {
            int sleepTime = Integer.parseInt(args[1]);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                System.out.println("Execution thread was interrupted!");
            }
        } else {
            panic("Invalid sleep time input!");
        }

    }

    /**
     * Saves a new observation to the list of unsent observations (_unsentObservations)
     * @param obs an observation command
     */
    private void saveObservation(String[] obs) {
        ObjectType type = getObjectType(obs[0]);

        if (obs.length == 2) {
            String id = obs[1].trim();
            Observation observation = Observation.newBuilder().setType(type).setIdentifier(id).build();
            _unsentObservations.add(observation);
        } else {
            panic("Wrong observation input!");
        }
    }

    /**
     * Reports all unsent observations to silo server
     */
    private void report() {
        if (!_unsentObservations.isEmpty()) {
            try {
                ReportRequest.Builder request = ReportRequest.newBuilder().setName(_camera.getName())
                        .addAllObservations(_unsentObservations);
                _frontend.report(request);
            } catch (StatusRuntimeException e) {
                panic(e.getMessage());
            }
            _unsentObservations.clear();
        }
    }

    /**
     * Logs in the camera to silo server
     * @param camera the camera associated wit
     */
    private void login() {
        CamJoinRequest.Builder request = CamJoinRequest.newBuilder().setCam(_camera);
        _frontend.camJoin(request);
    }

    private ObjectType getObjectType(String type) {
        if (type.equalsIgnoreCase("person"))
            return ObjectType.PERSON;
        else
            return ObjectType.CAR;
    }
}