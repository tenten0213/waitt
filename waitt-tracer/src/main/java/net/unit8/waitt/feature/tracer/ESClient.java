package net.unit8.waitt.feature.tracer;

import com.google.gson.*;
import net.unit8.waitt.feature.tracer.util.ISO8601Formatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Elasticsearch REST client
 * 
 * @author kawasima
 */
public class ESClient {
    private static final Logger LOG = Logger.getLogger(ESClient.class.getName());
    private final String baseUrl;

    private final JsonSerializer<Date> dateSerializer = new JsonSerializer<Date>() {
        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext
                context) {
            return src == null ? null : new JsonPrimitive(ISO8601Formatter.format(src));
        }
    };

    
    public ESClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void post(String path, Serializable entry) {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .create();

        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.connect();
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(gson.toJson(entry));
            out.close();
            
            // Consume a response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while(true) {
                String line = in.readLine();
                if (line == null) break;
            }
        } catch(IOException e) {
            LOG.log(Level.WARNING, "Fail to post elasticsearch", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
