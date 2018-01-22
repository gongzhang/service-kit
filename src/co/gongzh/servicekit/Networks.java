package co.gongzh.servicekit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author Gong Zhang
 */
public final class Networks {

    private static int connectTimeout = 0;
    private static int readTimeout = 0;

    public static int getConnectTimeout() {
        return connectTimeout;
    }

    public static void setConnectTimeout(int connectTimeout) {
        Networks.connectTimeout = connectTimeout;
    }

    public static long getReadTimeout() {
        return readTimeout;
    }

    public static void setReadTimeout(int readTimeout) {
        Networks.readTimeout = readTimeout;
    }

    public interface JSONResponseHandler {
        void handleResponse(@Nullable JSONResponse resp, @Nullable Exception ex);
    }

    public static void asyncRequestJSON(@NotNull String urlString, @NotNull String method, @Nullable Map<String, String> headers, @Nullable Object body, @NotNull JSONResponseHandler handler) {
        asyncRequestJSON(ThreadPool.global(), urlString, method, headers, body, handler);
    }

    public static void asyncRequestJSON(@NotNull Executor executor, @NotNull String urlString, @NotNull String method, @Nullable Map<String, String> headers, @Nullable Object body, @NotNull JSONResponseHandler handler) {
        executor.execute(() -> {
            try {
                JSONResponse response = requestJSON(urlString, method, headers, body);
                handler.handleResponse(response, null);
            } catch (Exception ex) {
                handler.handleResponse(null, ex);
            }
        });
    }

    public static class Response {
        public int statusCode;
        public boolean succeed; // code == 2xx
        public byte[] data; // not null
    }

    public static class JSONResponse extends Response {
        public JSONArray json; // not null
        public JSONResponse(Response r) {
            this.statusCode = r.statusCode;
            this.succeed = r.succeed;
            this.data = r.data;
        }

        @Override
        public String toString() {
            return String.format("Code: %d, JSON: %s", statusCode, json.toString(2));
        }
    }

    @NotNull
    public static JSONResponse requestJSON(@NotNull String urlString, @NotNull String method, @Nullable Map<String, String> headers, @Nullable Object body) throws Exception {
        byte[] data = null;
        if (body != null) {
            data = body.toString().getBytes("UTF-8");
        }
        if (headers != null) {
            headers = new HashMap<>(headers);
        } else {
            headers = new HashMap<>();
        }
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
        Response resp = request(urlString, method, headers, data);
        JSONResponse jr = new JSONResponse(resp);
        String jsonText = new String(resp.data, Charset.forName("UTF-8"));
        try {
            jr.json = new JSONArray(jsonText);
        } catch (JSONException ignored) {
            try {
                JSONObject obj = new JSONObject(jsonText);
                jr.json = new JSONArray(Collections.singleton(obj));
            } catch (JSONException ex) {
                if (jsonText.length() > 128) {
                    jsonText = jsonText.substring(0, 128) + "...";
                }
                throw new JSONException("JSON text is neither an object nor an array: " + jsonText);
            }
        }
        return jr;
    }

    @NotNull
    public static Response request(@NotNull String urlString, @NotNull String method, @Nullable Map<String, String> headers, @Nullable byte[] body) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (method.equals("PATCH")) {
            conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            conn.setRequestMethod("POST");
        } else {
            conn.setRequestMethod(method);
        }

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);

        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);

        try {
            if (body != null) {
                BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
                out.write(body);
                out.flush();
                out.close();
            }

            Response response = new Response();
            response.statusCode = conn.getResponseCode();
            response.succeed = (response.statusCode >= 200 && response.statusCode < 300);
            InputStream is;
            if(!response.succeed)
                is = conn.getErrorStream();
            else
                is = conn.getInputStream();

            if (is == null) {
                response.data = new byte[0];
                return response;
            }

            ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                bytesStream.write(buffer, 0, length);
            }
            bytesStream.flush();
            response.data = bytesStream.toByteArray();
            bytesStream.close();
            return response;

        } finally {
            conn.disconnect();
        }
    }

    private Networks() {}

}
