package webapp;

import app.MiningApp;
import org.apache.spark.sql.SparkSession;
import spark.ResponseTransformer;
import com.google.gson.Gson;

import static spark.Spark.*;

public class Server {
    public static void main(String[] args) {
        port(8080);
        staticFileLocation("webapp");


        // Spark Config
        String filePath = Server.class.getClassLoader().getResource("xml.txt").getPath();
        String masterInfo = "local[*]";
        SparkSession ss = SparkSession.builder().appName("LinkParser").master(masterInfo).getOrCreate();

        MiningApp.init(ss);
        MiningApp.importDump(filePath);


        // Json transformer
        ResponseTransformer jsonTransformer = new ResponseTransformer() {
            private Gson gson = new Gson();
            @Override
            public String render(Object model) throws Exception {
                return gson.toJson(model);
            }
        };


        get("/count",(req,res)-> MiningApp.pageCount());
        get("/find/:title",(req,res)-> MiningApp.getPage(req.params("title")),jsonTransformer);

    }
}
