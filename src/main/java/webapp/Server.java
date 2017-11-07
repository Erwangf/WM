package webapp;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.staticFileLocation;

public class Server {
    public static void main(String[] args) {
        port(8080);
        staticFileLocation("webapp");

//        Tuple2<Dataset<Row>, SparkSession> t = TestSparkActive.getDFandSS();
//        Dataset<Row> ds = t._1;
//        SparkSession ss = t._2;
//
//        get("/count",(req,res)-> ds.count());
    }
}
