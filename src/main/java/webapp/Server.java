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

        String masterInfo = "local[*]";
        SparkSession ss = SparkSession.builder()
                .appName("LinkParser")
                .master(masterInfo)
                .getOrCreate();

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

		post("/embedding/:dimension/:window/:iterations",(req,res)->{
			if(MiningApp.pagesLoaded()) {
				int dim = Integer.parseInt(req.params("dimension"));
				int win = Integer.parseInt(req.params("window"));
				int ite = Integer.parseInt(req.params("iterations"));
				MiningApp.wordEmbedding(dim,win,ite);
				return "performing embedding";
			}else {
				return "You need to load a dump first";
			}
		}, jsonTransformer);
		
        get("/graph/bestRank",(req,res)->MiningApp.getBestPageRankGraph(),jsonTransformer);





    }


    private static ResponseTransformer jsonTransformer = new ResponseTransformer() {
        private Gson gson = new Gson();
        @Override
        public String render(Object model) throws Exception {
            return gson.toJson(model);
        }
    };
}
