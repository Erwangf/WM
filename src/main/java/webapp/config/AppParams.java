package webapp.config;

public class AppParams {


    private String sparkMaster;
    private int numExecutors;
    private String sparkExecutorsMemory;
    private int sparkExecutorCores;


    private AppParams() {
        // TODO : load config from file ?
    }

    // loading app params on JVM startup
    private static AppParams INSTANCE = new AppParams();

    public static AppParams getInstance() {
        return INSTANCE;
    }


}
