# Silo server

## About

This is a gRPC server defined by the protobuf specification.

The server runs in a stand-alone process.


## Instructions for using Maven

Make sure that the parent POM was installed first.

To compile and run:

```
mvn compile exec:java
```

When running, the server awaits connections from clients.


## To configure the Maven project in Eclipse

'File', 'Import...', 'Maven'-'Existing Maven Projects'

'Select root directory' and 'Browse' to the project base folder.

Check that the desired POM is selected and 'Finish'.


----

