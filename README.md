# Wikipedia Mining
[Readme en français](./README.FR.md)

## Presentation

This project aim at analysing a french Wikipedia Dump, using two different approaches : 
* text-mining : building a vector representation of the corpus, using well-known VSM and word embedding method.
* graph-mining : build an atlas based on the cross references.

## Installation

### Prerequisites

Before installing the project, you'll need
* Maven (version > 3.3.*) - [Web site](https://maven.apache.org)
* Java (hereby, Java 8) - [Web site](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

You can check your current versions of the two softwares using the linux commands :

```
mvn --version
java -version
```

### Building

Building the Maven project :

```
mvn clean install
```


## Authors
[ArcToScience](arctoscience.com) Team, M2 Data Mining, University Lyon 2, France  :

* **Antoine Gourru** - [GitHub](https://github.com/AntoineGourru) - [Web site](antoinegourru.com)
* **Erwan Giry-Fouquet**  -  [GitHub](https://github.com/Erwangf) - [Web site](erwangf.com)


## License

This project is licensed under the MIT License - see the [LICENSE.md](./LICENSE.md) file for details