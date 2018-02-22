package webapp;
import app.MiningApp;
import org.apache.spark.sql.SparkSession;
import spark.Request;
import spark.Response;
import spark.ResponseTransformer;
import com.google.gson.Gson;
import webapp.config.AppParams;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        get("/status",(req,res)->  MiningApp.getStatus());

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

		get("/embedding/start/:dimension/:window/:iterations",(req,res)->{
			if(MiningApp.pagesLoaded()) {
				int dim = Integer.parseInt(req.params("dimension"));
				int win = Integer.parseInt(req.params("window"));
				int ite = Integer.parseInt(req.params("iterations"));
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
        get("/embedding/query",(req,res)->{
            String query = req.queryParams("query");
            System.out.println(query);
            int num_result = Integer.parseInt(req.queryParams("num"));
            return MiningApp.findSynonymsForQuery(query,num_result);
        },jsonTransformer);

        get("/export", Server::getFile);
        get("/exportVSM", Server::getFileEmbedding);

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


//    public static void main(String[] args) {
//        //setPort(8080);
//        get("/hello", (request, responce) -> getFile(request,responce));
//    }

    private static Object getFile(Request request, Response responce) {
        MiningApp.createTempPagesExport();
        File dir = new File(AppParams.getInstance().getLocalSaveDir() + "tmpExportPages.json");
        File[] files = dir.listFiles();
        File file = new File("");
        assert files != null;
        for (File f : files) {
            if (f.getName().endsWith(".json")){
                file = f;
                break;
            }
        }
        responce.raw().setContentType("application/octet-stream");
        responce.raw().setHeader("Content-Disposition", "attachment; filename=export.zip");
        try {

            try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(responce.raw().getOutputStream()));
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
                ZipEntry zipEntry = new ZipEntry(file.getName());

                zipOutputStream.putNextEntry(zipEntry);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = bufferedInputStream.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, len);
                }
            }
        } catch (Exception e) {
            halt(405, "server error");
        }

        return null;
    }

    private static Object getFileEmbedding(Request request, Response responce) {
        MiningApp.createTempVSMExport();
        File dir = new File(AppParams.getInstance().getLocalSaveDir() + "tmpExportVSM.json");
        File[] files = dir.listFiles();
        File file = new File("");
        assert files != null;
        for (File f : files) {
            if (f.getName().endsWith(".json")){
                file = f;
                break;
            }
        }
        responce.raw().setContentType("application/octet-stream");
        responce.raw().setHeader("Content-Disposition", "attachment; filename=export.zip");
        try {

            try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(responce.raw().getOutputStream()));
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
                ZipEntry zipEntry = new ZipEntry(file.getName());

                zipOutputStream.putNextEntry(zipEntry);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = bufferedInputStream.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, len);
                }
            }
        } catch (Exception e) {
            halt(405, "server error");
        }

        return null;
    }
}
