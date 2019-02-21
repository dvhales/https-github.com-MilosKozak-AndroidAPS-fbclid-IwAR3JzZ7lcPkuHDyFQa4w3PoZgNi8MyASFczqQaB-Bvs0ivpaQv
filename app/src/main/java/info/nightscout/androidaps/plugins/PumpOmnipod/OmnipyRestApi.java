package info.nightscout.androidaps.plugins.PumpOmnipod;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class OmnipyRestApi {

    private String REST_URL_TOKEN = "/omnipy/token";
    private String REST_URL_TAKEOVER_EXISTING_POD = "/omnipy/takeover";
    private String REST_URL_STATUS = "/pdm/status";
    private String REST_URL_GET_VERSION = "/omnipy/version";
    private String REST_URL_CHECK_PASSWORD = "/omnipy/pwcheck";
    private String REST_URL_SET_POD_PARAMETERS = "/omnipy/parameters";
    private String REST_URL_SET_LIMITS = "/omnipy/limits";
    private String REST_URL_RL_BATTERY = "/rl/battery";
    private String REST_URL_ACK_ALERTS = "/pdm/ack";
    private String REST_URL_DEACTIVATE_POD = "/pdm/deactivate";
    private String REST_URL_BOLUS = "/pdm/bolus";
    private String REST_URL_CANCEL_BOLUS = "/pdm/cancelbolus";
    private String REST_URL_SET_TEMP_BASAL = "/pdm/settempbasal";
    private String REST_URL_CANCEL_TEMP_BASAL = "/pdm/canceltempbasal";

    private String _baseUrl;
    private OmnipyApiSecret _apiSecret;


    public OmnipyRestApi(String baseUrl, String passphrase) {
        _baseUrl = baseUrl;
        _apiSecret = OmnipyApiSecret.fromPassphrase(passphrase);
    }


    public String RestStatus(){
        return RestStatus(0);
    }

    public String RestStatus(int requestType)
    {
        ArrayList<Pair<String,String>> parameters = getAuthenticationParameters();
        parameters.add(new Pair<>("type", Integer.toString(requestType)));
        return getApiResult(REST_URL_STATUS, parameters);
    }

    private ArrayList<Pair<String, String>> getAuthenticationParameters() {
        OmnipyApiToken token = getToken();
        String authToken = Base64.encode(token.getAuthenticationToken());
        String iv = Base64.encode(token.getIV());
        ArrayList<Pair<String,String>> parameters = new ArrayList<>();
        parameters.add(new Pair<>("auth", authToken));
        parameters.add(new Pair<>("iv", iv));
        return parameters;
    }

    private OmnipyApiToken getToken() {
        OmnipyApiToken apiToken = null;
        try {
            String response = getApiResult(REST_URL_TOKEN, null);
            JSONObject json = new JSONObject(response);
            String tokenStr = json.getString("result");
            apiToken = new OmnipyApiToken(Base64.decode(tokenStr), _apiSecret);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return apiToken;
    }

    private String getApiResult(String path, ArrayList<Pair<String, String>> parameters)
    {
        String response = null;
        try
        {
            int parameterCount = 1;
            if (parameters != null)
                parameterCount += parameters.size() * 2;
            String[] stringParams = new String[parameterCount];
            stringParams[0] = _baseUrl + path;

            if (parameters != null)
            {
                int index = 1;
                for (Pair<String,String> param : parameters) {
                    stringParams[index++] = param.first;
                    stringParams[index++] = param.second;
                }
            }
            RestApiTask task = (RestApiTask) new RestApiTask().execute(stringParams);
            response = task.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return response;
    }
}

class RestApiTask extends AsyncTask<String, Void,String> {
    @Override
    protected String doInBackground(String... strings) {
        String response = null;
        try {
            Uri.Builder ub = Uri.parse(strings[0]).buildUpon();
            if (strings.length > 1)
            {
                int index = 1;
                while(index < strings.length)
                {
                    ub.appendQueryParameter(strings[index], strings[index+1]);
                    index += 2;
                }
            }

            URL url = new URL(ub.toString());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            int responseCode = urlConnection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                response = readStream(urlConnection.getInputStream());
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }
}