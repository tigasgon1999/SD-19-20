# Sauron

Distributed Systems 2019-2020, 2nd semester project


## Authors

**Group A53**


### Team members


| Number | Name              | User                              | Email                                               |
| -------|-------------------|-----------------------------------| ----------------------------------------------------|
| 89414  | Andreia Pereira   | <https://github.com/decasppereira>| <mailto:andreia.sofia.pereira@tecnico.ulisboa.pt>   |
| 89433  | Diogo Pacheco     | <https://github.com/DPac99>       | <mailto:diogo.a.pacheco@tecnico.ulisboa.pt>         |
| 89559  | Tiago Lé          | <https://github.com/tigasgon199>  | <mailto:tiagopgle@tecnico.ulisboa.pt>               |

### Task leaders


| Task set | To-Do                         | Leader              |
| ---------|-------------------------------| --------------------|
| core     | protocol buffers, silo-client | _(whole team)_      |
| T1       | cam_join, cam_info, eye       | _Andreia Pereira_   |
| T2       | report, spotter               | _Diogo Pacheco_     |
| T3       | track, trackMatch, trace      | _Tiago Lé_          |
| T4       | test T1                       | _Tiago Lé_          |
| T5       | test T2                       | _Andreia Pereira_   |
| T6       | test T3                       | _Diogo Pacheco_     |


## Getting Started

The overall system is composed of multiple modules.
The main server is the _silo_.
The clients are the _eye_ and _spotter_.

See the [project statement](https://github.com/tecnico-distsys/Sauron/blob/master/README.md) for a full description of the domain and the system.

### Prerequisites

Java Developer Kit 11 is required running on Linux, Windows or Mac.
Maven 3 is also required.

To confirm that you have them installed, open a terminal and type:

```
javac -version

mvn -version
```

### Installing

To compile and install all modules:

```
mvn clean install -DskipTests
```

The integration tests are skipped because they require the servers to be running.


## Built With

* [Maven](https://maven.apache.org/) - Build Tool and Dependency Management
* [gRPC](https://grpc.io/) - RPC framework


## Versioning

We use [SemVer](http://semver.org/) for versioning. 
