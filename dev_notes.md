# Notes
Pour après avoir installé Hadoop + Spark, lorsque le cluster est prêt, pour run l'application :
````
java -jar monfichier.jar 
````

Pour importer un jeu de données, lorsque le serveur tourne, aller dans load, puis indiquer comme champs de fichier une adresse sous la forme suivante :
````
hdfs://adresse_ip_namenode:port_namenode/chemin_fichier_sur_hdfs
````
Exemple : 
````
hdfs://189.84.146.105:8020/wiki1.txt
````