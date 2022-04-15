# multiclass-classification
Multiclass classification with binary classifier (Data Mining Project)

Per lo svolgimento della seguente esercitazione ho sviluppo un applicativo Java con interfaccia grafica. Essa permette di selezionare i path dei file CSV in modo da processarli ed analizzarli tramite WEKA.

Per compiere tutto il processo di sviluppo sono stati utilizzati i seguenti componenti software:
  - Java 1.8
  -	Spring Boot 2.6.3
  -	Weka 3.8.6
  -	Weka SMOTE 1.0.3
  -	Maven

Per poter compilare ed eseguire nella propria macchina l’applicativo software occorre avere installato:
  - Java 1.8
  - Maven

Ed eseguire nel root della cartella del progetto Java i seguenti comandi nel terminale:
  - `mvn clean install -U`
  - `./mvnw -X spring-boot:run`

In questo modo l’applicativo partirà senza intoppi.

Esso genererà i report e i risultati della classificazione dentro la cartella `resources/irisSingleCl`.
