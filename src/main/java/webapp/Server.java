package webapp;
import app.MiningApp;
import org.apache.spark.sql.SparkSession;
import spark.ResponseTransformer;
import com.google.gson.Gson;
import webapp.config.AppParams;

import java.io.FileNotFoundException;
import java.util.Map;

import static spark.Spark.*;

public class Server {
    public static void main(String[] args) {
        if(args.length!=1){
          throw new IllegalArgumentException("Incorrect number of parameters. A configuration json file is needed. Ex : myConfigFile.json");
        }
        port(8199);
        staticFileLocation("webapp");

        // Cluster :



//        SparkSession ss = SparkSession.builder()
//                .appName("LinkParser")
//                .master("spark://master.atscluster:7077")
//                .config("spark.executor.memory","5g")
//                .config("spark.driver.memory","4g")
//                .config("spark.jars","hdfs://master.atscluster:8020/wikipedia-mining-0.0-jar-with-dependencies.jar")
//                .config("spark.executor.cores","4")
//                .getOrCreate();


        AppParams params = AppParams.getInstance();
        params.loadParamsFromJSON(args[0]);

        SparkSession.Builder sparkSessionBuilder = SparkSession.builder()
                .appName(params.getAppName())
                .master(params.getMaster());

        for (Map.Entry<String, String> entry : params.getSparkOptions().entrySet()) {
            sparkSessionBuilder = sparkSessionBuilder.config(entry.getKey(),entry.getValue());
        }

        SparkSession ss = sparkSessionBuilder.getOrCreate();



//        MiningApp.init(ss);
        get("/status",(req,res)-> {
                    if (MiningApp.isStarted()) {
                        return MiningApp.getStatus();
                    } else return "not initialized";
                });

        get("/loadedFile",(req,res)->{
            if(MiningApp.isStarted()){
                return MiningApp.getLoadedFile();
            }
            else return "";
        });


        get("/count", (req, res) -> MiningApp.pageCount());

        
        get("/find/:title", (req, res) -> {
            MiningApp.Page p = MiningApp.getPage(req.params("title"));
            return p == null ? new Object() : p;
        }, jsonTransformer);

        get("/import/local",(req,res)->{
            try{
                MiningApp.importLocal();
            } catch (FileNotFoundException f){
                return "FILE NOT FOUND";
            }
            return "OK";
        },jsonTransformer);

        post("/import/dump",(req,res)->{
            MiningApp.importWikiDumpInBackground(req.queryParams("path"));
            return "Import started";
        }, jsonTransformer);

		get("/embedding/start",(req,res)->{
			if(MiningApp.pagesLoaded()) {
				int dim = Integer.parseInt(req.queryParams("dimension"));
				int win = Integer.parseInt(req.queryParams("window"));
				int ite = Integer.parseInt(req.queryParams("iterations"));
				MiningApp.startWordEmbedding(dim,win,ite);
				return "performing embedding";
			}else {
				return "You need to load a dump first";
			}
		}, jsonTransformer);
		
        get("/graph/bestRank",(req,res)->MiningApp.getBestPageRankGraph(),jsonTransformer);

        get("/wordEmbedding/sum",(req,res)->{

            return null;
        },jsonTransformer);

        get("/embedding/query/:query/:num",(req,res)->{
            String query = req.params("query");
            int num_result = Integer.parseInt(req.params("num"));
            return MiningApp.findSynonymsForQuery(query,num_result);
        },jsonTransformer);

    }


    private static ResponseTransformer jsonTransformer = new ResponseTransformer() {
        private Gson gson = new Gson();
        @Override
        public String render(Object model) throws Exception {
            return gson.toJson(model);
        }
    };
}
