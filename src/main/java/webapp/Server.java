package webapp;
import app.MiningApp;
import org.apache.spark.sql.SparkSession;
import spark.ResponseTransformer;
import com.google.gson.Gson;

import java.io.FileNotFoundException;

import static spark.Spark.*;

public class Server {
    public static void main(String[] args) {
        port(8199);
        staticFileLocation("webapp");

        // Cluster :



        SparkSession ss = SparkSession.builder()
                .appName("LinkParser")
                .master("spark://master.atscluster:7077")
                .config("spark.executor.memory","5g")
                .config("spark.driver.memory","4g")
                .config("spark.jars","hdfs://master.atscluster:8020/wikipedia-mining-0.0-jar-with-dependencies.jar")
                .config("spark.executor.cores","4")
                .getOrCreate();


        // Local
//        SparkSession ss = SparkSession.builder()
//                .appName("LinkParser")
//                .master("local[*]")
//                .config("spark.sql.warehouse.dir", "./spark-warehouse")
//                .getOrCreate();


        MiningApp.init(ss);



        get("/count", (req, res) -> MiningApp.pageCount());

        get("/status",(req,res)->MiningApp.getStatus());

        get("/loadedFile",(req,res)->MiningApp.getLoadedFile());
        
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
