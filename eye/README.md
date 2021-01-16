# Eye application

## About

This is a CLI (Command-Line Interface) application that can send data to server.


## Instructions for using Maven

Make sure that the parent POM was installed first.

To compile and run using _exec_ plugin:

```
mvn compile exec:java
```

To generate launch scripts for Windows and Linux
(the POM is configured to attach appassembler:assemble to the _install_ phase):

```
mvn install
```

To run using appassembler plugin on Linux:

```
./target/appassembler/bin/eye arg0 arg1 arg2
```

To run using appassembler plugin on Windows:

```
target\appassembler\bin\eye arg0 arg1 arg2
```


## To configure the Maven project in Eclipse

'File', 'Import...', 'Maven'-'Existing Maven Projects'

'Select root directory' and 'Browse' to the project base folder.

Check that the desired POM is selected and 'Finish'.


----

