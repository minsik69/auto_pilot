package com.example.capstone_car;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class EndFragment extends Fragment {
    private static final String[] spinner_list = {"전체", "보낸 배달", "받은 배달"};  // spinner array
    ArrayList<List> list = new ArrayList<>();                                         // data array list
    private RecyclerView endList;
    private EndListAdapter mAdapter;

    Button button_week, button_month, button_sixmonth, button_check, textView_startDate, button_refresh;
    LinearLayout select_date;
    Spinner spinner;

    String first_date, second_date, user_id, term, date_start, date_end;

    Context mContext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_end, container, false);

        mContext = container.getContext();      // Fragment can't use 'this' to get context differently from activities

        user_id = SharedPreferenceUtil.getString(mContext, "user_id");      // User ID logged in
        term = "all";       // Value to send to server, Basic date
        date_start = "0";   // Value to send to server, Basic date
        date_end = "0";     // Value to send to server, Basic date

        // RecyclerView
        endList = (RecyclerView) v.findViewById(R.id.endList);
        endList.setHasFixedSize(true);
        mAdapter = new EndListAdapter(list);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        endList.setLayoutManager(mLayoutManager);
        endList.setItemAnimator(new DefaultItemAnimator());;
        endList.setAdapter(mAdapter);

        button_week = (Button)v.findViewById(R.id.button_week);
        button_month = (Button)v.findViewById(R.id.button_month);
        button_sixmonth = (Button)v.findViewById(R.id.button_sixmonth);
        button_check = (Button)v.findViewById(R.id.button_check);
        select_date = v.findViewById(R.id.select_date);
        textView_startDate = (Button)v.findViewById(R.id.textView_startDate);
        button_refresh = (Button)v.findViewById( R.id.button_refresh );

        // Spinner
        spinner = (Spinner)v.findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, spinner_list);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < spinner_list.length) { getSelectedCategory(position); }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        /*
        Reasons to get a date tomorrow
        >> if you set the maximum date to today when you stop selecting future dates in the calendar, you can select only the day before today.
        >> Set the maximum date to tomorrow to be selectable until today
         */
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);

        // Calendar (Custom date lookup)
        textView_startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
                CalendarConstraints.Builder constraintBuilder = new CalendarConstraints.Builder();

                CalendarConstraints.DateValidator dateValidatorMax = DateValidatorPointBackward.before( tomorrow.getTimeInMillis() );
                ArrayList<CalendarConstraints.DateValidator> listValidators = new ArrayList<CalendarConstraints.DateValidator>();
                listValidators.add(dateValidatorMax);
                CalendarConstraints.DateValidator validator = CompositeDateValidator.allOf( listValidators );
                constraintBuilder.setValidator( validator );

                builder.setCalendarConstraints( constraintBuilder.build() );
                MaterialDatePicker<Pair<Long, Long>> picker = builder.build();

                assert getFragmentManager() != null;
                picker.show(getFragmentManager(), picker.toString());
                picker.addOnPositiveButtonClickListener( new MaterialPickerOnPositiveButtonClickListener<Pair<Long, Long>>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onPositiveButtonClick(Pair<Long, Long> selection) {

                        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        first_date = dateFormat.format( selection.first );       // start date
                        second_date = dateFormat.format( selection.second );     // End date

                        textView_startDate.setText( first_date + " - " + second_date );

                        term = "check";     // Value to send to server, Detailed inquiry

                        termCheck(user_id, term, second_date, first_date);
                    }
                } );
            }
        });

        // All lookup button
        button_refresh.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                term = "all";       // Value to send to server, All date
                date_start = "0";
                date_end = "0";

                termCheck(user_id, term, date_end, date_start);

                select_date.setVisibility(View.GONE);
            }
        } );

        // 1 week lookup button
        button_week.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                term = "week";      // Value to send to server, 1 week
                date_start = "0";
                date_end = "0";

                termCheck(user_id, term, date_end, date_start);

                select_date.setVisibility(View.GONE);
                textView_startDate.setText( "날짜 선택하기" );
            }
        } );

        // 1 month lookup button
        button_month.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                term = "month";     // Value to send to server, 1 month
                date_start = "0";
                date_end = "0";

                termCheck(user_id, term, date_end, date_start);

                select_date.setVisibility(View.GONE);
                textView_startDate.setText( "날짜 선택하기" );
            }
        } );

        // 6 month lookup button
        button_sixmonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                term = "6month";    // Value to send to server, 6 month
                date_start = "0";
                date_end = "0";

                termCheck(user_id, term, date_end, date_start);

                select_date.setVisibility(View.GONE);
                textView_startDate.setText( "날짜 선택하기" );
            }
        });

        // Custom date lookup button
        button_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (select_date.getVisibility() == View.GONE) {
                    select_date.setVisibility(View.VISIBLE);    // Show calendar
                } else {
                    select_date.setVisibility(View.GONE);       // Hide calendar
                }
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        // Minimize duplication and make loading faster by executing code only once
        termCheck(user_id, term, date_end, date_start);

        super.onResume();
    }

    // Data preparation
    private void termCheck(String user_id, String term, String date_end, String date_start) {   // Send user_id, term, date_start, date_end to the server
        new JSONTask1().execute("https://b8c98bce1df6.ngrok.io/api/dlvy/completedlvy/"+user_id+"/"+term+"/"+date_end+"/"+date_start);
    }

    public class JSONTask1 extends AsyncTask<String, String, String> {
        String user_id = SharedPreferenceUtil.getString(mContext, "user_id");

        @Override
        protected String doInBackground(String... urls) {   // GET 방식
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate( "user_id", user_id );
                jsonObject.accumulate( "term", term );
                jsonObject.accumulate( "date_start", date_start );
                jsonObject.accumulate( "date_end", date_end );
                HttpURLConnection con = null;
                BufferedReader reader = null;

                try {
                    URL url = new URL( urls[0] );
                    con = (HttpURLConnection) url.openConnection();
                    con.connect();
                    InputStream stream = con.getInputStream();
                    reader = new BufferedReader( new InputStreamReader(stream) );
                    StringBuffer buffer = new StringBuffer();
                    String line = "";

                    while((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    return buffer.toString();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }

                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @SuppressLint("LongLogTag")
        @Override
        protected void onPostExecute(String result) {   // Add number after list.add (1 - Sent delivery, 2 - Received Delivery)
            super.onPostExecute(result);

            try {
                JSONObject obj = new JSONObject(result);

                JSONArray completed_dlvy= obj.getJSONArray("completed_dlvy");

                list.clear();

                for (int i = 0; i < completed_dlvy.length(); i++) {
                    JSONObject complete = completed_dlvy.getJSONObject( i );

                    if (complete.has("receiver_name")) {
                        list.add(new List(complete.getString( "receiver_name")+ "(받은 사람)", complete.getString( "dlvy_start_point" ), complete.getString("dlvy_end_point"), complete.getString("dlvy_status"), complete.getString("dlvy_date"), 1));
                    } else if (complete.has("sender_name")) {
                        list.add(new List(complete.getString( "sender_name")+ "(보낸 사람)", complete.getString( "dlvy_start_point" ), complete.getString("dlvy_end_point"), complete.getString("dlvy_status"), complete.getString("dlvy_date"), 2));
                    }
                }

                mAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Spinner function (All, Sent Delivery, Received Delivery)
    private void getSelectedCategory(int categoryID) {
        // Create another temporary list to show the data contained in the temporary list when there is a spinner classification
        ArrayList<List> lists = new ArrayList<>();

        if (categoryID == 0) {
            mAdapter = new EndListAdapter(list);
        } else {
            for (List adapter : list) {
                if (adapter.getCategory() == categoryID) {
                    lists.add(adapter);
                }
            }
            mAdapter = new EndListAdapter(lists);
        }
        endList.setAdapter(mAdapter);
    }
}