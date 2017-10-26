# Wikipedia Mining
[Readme in english](./README.md)

## Présentation

L'objectif de ce projet est d'analyser un dump de donnée Wikipedia, en langue française, suivant deux approches : 
* text-mining : construction d'un espace vectoriel (mot,vecteur), word embedding
* graph-mining : exploitation des lien entre les pages

## Installation

### Prérequis

Afin d'installer ce projet, il est nécessaire d'avoir 
* Maven (version > 3.3.*) - [Site web](https://maven.apache.org/)
* Java (ici, Java 8) - [Site web](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

Pour vérifier que l'installation de ces deux logiciels a réussi :
```
mvn --version
java -version
```

### Assemblage

Assemblez le projet Maven :

```
mvn clean install
```


## Exécuter le programme en local

Il est possible d'exécuter le projet en local, sans cluster Spark. Les classes principales exécutables en local ont 
pour package : run.local.*

### Word2VecHP

Il s'agit d'un exemple de l'application de Word2Vec, appliqué sur le corpus d'Harry Potter 1. L'algorithme construit un
espace vectoriel à partir du corpus, puis plonge un mot fourni en paramètre dans ce corpus, et en affiche les 20 mots 
les plus proches, et leur distance.

Exemple :
```
java -jar target/word-embedding-0.0.jar run.local.Word2VecHP "Hermione"
```

## Exécuter le programme sur un cluster

Pour exécuter le projet sur le cluster, il faut déployer le JAR sur un cluster Spark disposant d'HDFS, puis d'effectuer
un spark-submit.
**A documenter**

## Auteurs
Equipe [ArcToScience](arctoscience.com), M2 DM Université Lyon 2  :

* **Antoine Gourru** - [GitHub](https://github.com/AntoineArctos) - [Site web](antoinegourru.com)
* **Erwan Giry-Fouquet**  -  [GitHub](https://github.com/Erwangf) - [Site web](erwangf.com)


## License

This project is licensed under the MIT License - see the [LICENSE.md](./LICENSE.md) file for details