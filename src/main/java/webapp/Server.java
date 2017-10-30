package webapp;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.staticFileLocation;

public class Server {
    public static void main(String[] args) {
        port(8080);
        staticFileLocation("webapp");
        get("/hello",(req,res)-> "yololo");
    }
}
