# Silo client

## About

This is a gRPC client that provides a frontend and performs integration tests on a running server.
The integration tests verify the responses of the server to a set of requests.


## Instructions for using Maven

Make sure that the parent POM was installed first.

To compile and install:

```
mvn install
```

To compile and run integration tests, first start the servers, and then:

```
mvn verify
```


## To configure the Maven project in Eclipse

'File', 'Import...', 'Maven'-'Existing Maven Projects'

'Select root directory' and 'Browse' to the project base folder.

Check that the desired POM is selected and 'Finish'.


----

