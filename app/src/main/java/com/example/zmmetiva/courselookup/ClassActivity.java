package com.example.zmmetiva.courselookup;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by zmmetiva on 10/2/15.
 */
public class ClassActivity extends Activity {

    String courseNumber;
    String coursePrefix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classes);

        coursePrefix = getIntent().getStringExtra("COURSE_PREFIX");
        courseNumber = getIntent().getStringExtra("COURSE_NUMBER");

        TextView title = (TextView) findViewById(R.id.courseName);


        SaveTheFeed feed = new SaveTheFeed();
        feed.setCourseNum(getIntent().getStringExtra("COURSE_NUMBER"));
        feed.setPrefix(getIntent().getStringExtra("COURSE_PREFIX"));

        try {
            feed.execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        TextView desc = (TextView) findViewById(R.id.courseDesc);
        title.setText(coursePrefix + " " + courseNumber + " - " + feed.getTitle());
        desc.setText("\n" + feed.getDescription());

        ListView instructList = (ListView) findViewById(R.id.listView);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, feed.getInstructorsArray());

        instructList.setAdapter(adapter2);
        instructList.setClickable(true);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    class SaveTheFeed extends AsyncTask<Void, Void, Void> {

        String title = "";

        String description = "";

        String courseNum = "";

        // Holds JSON data in String format
        String jsonString = "";

        // Will hold the translations that will be displayed on the screen
        List<String> result = new ArrayList<String>();
        List<String> instructors = new ArrayList<String>();

        String prefix = "";

        public void setPrefix(String p) {
            prefix = p;
        }

        // Everything that should execute in the background goes here
        // You cannot edit the user interface from this method
        @Override
        protected Void doInBackground(Void... voids) {

            // Client used to grab data from a provided URL
            DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());

            // Provide the URL for the post request
            HttpPost httpPost = new HttpPost("http://api.svsu.edu/courses?prefix=" + prefix + "&courseNumber=" + courseNum);

            // Define that the data expected is in JSON format
            httpPost.setHeader("Content-type", "application/json");

            // Allows you to input a stream of bytes from the URL
            InputStream inputStream = null;

            try {

                // The client calls for the post request to execute and sends the results back
                HttpResponse response = httpClient.execute(httpPost);

                // Holds the message sent by the response
                HttpEntity entity = response.getEntity();

                // Get the content sent
                inputStream = entity.getContent();

                // A BufferedReader is used because it is efficient
                // The InputStreamReader converts the bytes into characters
                // My JSON data is UTF-8 so I read that encoding
                // 8 defines the input buffer size
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);

                // Storing each line of data in a StringBuilder
                StringBuilder sb = new StringBuilder();

                String line = null;

                // readLine reads all characters up to a \n and then stores them
                while ((line = reader.readLine()) != null) {

                    sb.append(line + "\n");

                }

                // Save the results in a String
                jsonString = sb.toString();

                // Create a JSONObject by passing the JSON data
                JSONObject jObject = new JSONObject(jsonString);

                // Get the Array named translations that contains all the translations
                JSONArray jArray = jObject.getJSONArray("courses");

                // Cycles through every translation in the array
                outputTranslations(jArray);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        // Called after doInBackground finishes executing
        @Override
        protected void onPostExecute(Void aVoid) {

        }

        protected void outputTranslations(JSONArray jsonArray){

            StringBuilder sb = new StringBuilder();

            // Save all the translations by getting them with the key
            try{

                JSONObject temp = jsonArray.getJSONObject(0);

                title = temp.getString("title");
                description = temp.getString("description");

                for(int i = 0; i < jsonArray.length(); i++){


                    System.out.println(jsonArray.length());
                    JSONObject translationObject =
                            jsonArray.getJSONObject(i);

                    JSONArray instructor = translationObject.getJSONArray("instructors");
                    JSONArray meetingTimes = translationObject.getJSONArray("meetingTimes");

                    JSONObject instructorName = instructor.getJSONObject(0);
                    JSONObject meetingTimesArray = meetingTimes.getJSONObject(0);

                    if(!instructorName.getString("name").equals("Staff")) {

                        instructors.add(instructorName.getString("name") + "\n" +
                                        meetingTimesArray.getString("days") + ": " +meetingTimesArray.getString("startTime") + " - " + meetingTimesArray.getString("endTime") + "\n" +
                                        meetingTimesArray.getString("building") + meetingTimesArray.getString("room"));

                        sb.setLength(0);
                        sb.append(translationObject.getString("courseNumber"));
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        public void setCourseNum(String num) {
            courseNum = num;
        }

        public List<String> getArray() {
            return result;

        }

        public List<String> getInstructorsArray() {
            return instructors;

        }

        public String getDescription() {
            return description + "\n";
        }

        public String getTitle() {
            return title;
        }
    }
}
