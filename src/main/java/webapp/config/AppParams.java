package webapp.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.*;

public class AppParams {


    public String getMaster() {
        return master;
    }

    public void setMaster(String master) {
        this.master = master;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Map<String, String> getSparkOptions() {
        return sparkOptions;
    }

    public void setSparkOptions(Map<String, String> sparkOptions) {
        this.sparkOptions = sparkOptions;
    }

    private String master;
    private String appName;
    private Map<String, String> sparkOptions;


    private AppParams() {
        // empty constructor
    }

    /**
     * load parameters from a JSON file
     *
     * @param jsonPath a path (String) to the json file
     */
    public void loadParamsFromJSON(String jsonPath) {
        try (FileInputStream inputStream = new FileInputStream(jsonPath)) {
            // read the file (entirely)
            String raw = IOUtils.toString(inputStream);
            JSONObject obj = new JSONObject(raw);

            // important parameters
            this.appName = obj.getString("appName");
            this.master = obj.getString("master");

            // spark options ( a json array : [ { "name" : "blablabla", "value" : "blablabla" } ]
            this.sparkOptions = new LinkedHashMap<>();
            JSONArray sparkOptionsJSON = obj.getJSONArray("sparkOptions");
            for (int i = 0; i < sparkOptionsJSON.length(); i++) {
                String name = sparkOptionsJSON.getJSONObject(i).getString("name");
                String value = sparkOptionsJSON.getJSONObject(i).getString("value");
                this.sparkOptions.put(name, value);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // loading app params on JVM startup
    private static AppParams INSTANCE = new AppParams();

    public static AppParams getInstance() {
        return INSTANCE;
    }


}
