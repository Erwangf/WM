package webapp;

import app.MiningApp;
import org.apache.spark.sql.SparkSession;
import spark.ResponseTransformer;
import com.google.gson.Gson;

import java.io.FileNotFoundException;

import static spark.Spark.*;

public class Server {
    public static void main(String[] args) {
        port(8080);
        staticFileLocation("webapp");


        // Spark Config
        @SuppressWarnings("ConstantConditions")
        String filePath = Server.class.getClassLoader().getResource("xml.txt").getPath();
        String masterInfo = "local[*]";
        SparkSession ss = SparkSession.builder().appName("LinkParser").master(masterInfo).getOrCreate();

        MiningApp.init(ss);


        // Json transformer
        ResponseTransformer jsonTransformer = new ResponseTransformer() {
            private Gson gson = new Gson();
            @Override
            public String render(Object model) throws Exception {
                return gson.toJson(model);
            }
        };


        get("/count", (req, res) -> MiningApp.pageCount());

        get("/find/:title", (req, res) -> {
            MiningApp.Page p = MiningApp.getPage(req.params("title"));
            return p == null ? new Object() : p;
        }, jsonTransformer);

        get("/import/local",(req,res)->{
            try{
                MiningApp.importPages();
            } catch (FileNotFoundException f){
                return "FILE NOT FOUND";
            }
            return "OK";
        },jsonTransformer);

        post("/import/dump",(req,res)->{
            MiningApp.importWikiDump(req.queryParams("path"));
            return "OK";
        }, jsonTransformer);

    }
}
