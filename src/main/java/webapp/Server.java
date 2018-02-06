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

        /* ###############  Configuration  ############### */

        AppParams params = AppParams.getInstance();
        params.loadParamsFromJSON(args[0]);

        // starting to build spark session : appName and master
        SparkSession.Builder sparkSessionBuilder = SparkSession.builder()
                .appName(params.getAppName())
                .master(params.getMaster());

        // spark options
        for (Map.Entry<String, String> entry : params.getSparkOptions().entrySet()) {
            sparkSessionBuilder = sparkSessionBuilder.config(entry.getKey(),entry.getValue());
        }

        // build SparkSession
        SparkSession ss = sparkSessionBuilder.getOrCreate();

        // initialize MiningApp
        MiningApp.init(ss);

        // initialize Server
        port(8199);
        staticFileLocation("webapp");

        /* ##################  Routes  ################## */

        // return the status of the application
        get("/status",(req,res)-> {
                    if (MiningApp.isStarted()) {
                        return MiningApp.getStatus();
                    } else return "not initialized";
                });

        // return the name of the loaded wikipedia dump
        get("/loadedFile",(req,res)->{
            if(MiningApp.isStarted()){
                return MiningApp.getLoadedFile();
            }
            else return "";
        });

        // return the number of pages
        get("/count", (req, res) -> MiningApp.pageCount());

        //return the page corresponding to the title
        get("/find/:title", (req, res) -> {
            MiningApp.Page p = MiningApp.getPage(req.params("title"));
            return p == null ? new Object() : p;
        }, jsonTransformer);

        // start the import procedure of the dump saved in the local cache
        get("/import/local",(req,res)->{
            try{
                MiningApp.importLocal();
            } catch (FileNotFoundException f){
                return "FILE NOT FOUND";
            }
            return "OK";
        },jsonTransformer);

        // start the dump found at the asked path (in query parameters, "path")
        post("/import/dump",(req,res)->{
            MiningApp.importWikiDumpInBackground(req.queryParams("path"));
            return "Import started";
        }, jsonTransformer);

        // start the word embedding procedure, with 3 parameters :
        // - dimension : the number of dimension of the vector space (ex : 50)
        // - window : the window for selecting multiple words (ex : 5)
        // - iterations : the number of iteration for the neural network
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

		// get the best pages, based on the PageRank index
        get("/graph/bestRank",(req,res)->MiningApp.getBestPageRankGraph(),jsonTransformer);

        // WORK IN PROGRESS
        get("/wordEmbedding/sum",(req,res)->{

            return null;
        },jsonTransformer);

        // WORK IN PROGRESS
        // TODO : word embedding sum operation
        get("/embedding/query/:query/:num",(req,res)->{
            String query = req.params("query");
            int num_result = Integer.parseInt(req.params("num"));
            return MiningApp.findSynonymsForQuery(query,num_result);
        },jsonTransformer);

    }


    /**
     * A ResponseTransformer, converting any object to his JSON equivalent.
     */
    private static ResponseTransformer jsonTransformer = new ResponseTransformer() {
        private Gson gson = new Gson();
        @Override
        public String render(Object model) throws Exception {
            return gson.toJson(model);
        }
    };
}
