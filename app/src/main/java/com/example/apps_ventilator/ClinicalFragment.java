package com.example.apps_ventilator;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ClinicalFragment extends Fragment implements OnChartValueSelectedListener {

    LineChart mChart;
    LineChart mChart1;
    String pesanutuh="";
    int dataFI02, dataTidalVol, dataRespRate, dataPEEP, dataIERatio, dataMaxPlanPress;
    double maskP, flow, PeakPress, PEEP, Tidal_Vol_INS, Tidal_Vol_EXP, Min_Vol_EXP = 0;
    public String pesan = "";
    public String pesanascii = "";
    private Button start, stop, ChangeParameter, Spontaneous, Timed, Combined;
    private EditText editText, FIO2, Tidal_Vol_Edit, Resp_Rate, PEEPSet, IERatio, MaxPlanPress;
    private TextView output;
    private TextView NilaiPeakPressure;
    private TextView PEEPText;
    private TextView INS_View, EXP_View, Min_View;
    private InputStream inputStream;
    private OkHttpClient client;
    WebSocket ws;
    public static final String SHARED_PREFS = "sharedPrefs";

    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }

    private class EchoWebSocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;
        @Override
        public void onOpen(WebSocket ws, Response response) {
            ws.send("1");
//            JSON_Parameter();
//            webSocket.send("What's up ?");
//            webSocket.send(ByteString.decodeHex("deadbeef"));
//            webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !");
        }
        @Override
        public void onMessage(WebSocket webSocket, String text) {
            output(text);
        }
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            output("Receiving bytes : " + bytes.hex());
        }
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            output("Closing : " + code + " / " + reason);
        }
        @Override
        public void onFailure(@NotNull WebSocket webSocket, Throwable t, Response response) {
            output("Error : " + t.getMessage());
        }
    }

    private class EchoWebSocketListenerStop extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;
        @Override
        public void onOpen(WebSocket webSocket, Response responseStop) {
            webSocket.send("0");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clinical, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onViewCreated(@NotNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateDataMonitoring();
        TextClock txClock = getView().findViewById(R.id.timeView);
        TextClock txDate = getView().findViewById(R.id.dateView);
        txClock.setFormat12Hour("hh:mm a");
        txDate.setFormat12Hour("EEE, d MMM");

        start = (Button) getView().findViewById(R.id.startButton);
        stop = (Button) getView().findViewById(R.id.stopButton);
        ChangeParameter = (Button) getView().findViewById(R.id.changeParameterButton);
        Spontaneous  = (Button) getView().findViewById(R.id.spontanegusButton);
        Timed = (Button) getView().findViewById(R.id.timedButton);
        Combined = (Button) getView().findViewById(R.id.combinedButton);

        output = (TextView) getView().findViewById(R.id.output);
        NilaiPeakPressure = (TextView) getView().findViewById(R.id.valuePeakPressure);
        PEEPText = (TextView) getView().findViewById(R.id.valuePEEP);
        INS_View = (TextView) getView().findViewById(R.id.valueTidalVolumeIns);
        EXP_View = (TextView) getView().findViewById(R.id.valueTidalVolumeExp);
        Min_View = (TextView) getView().findViewById(R.id.valueMinuteVolumeExp);

        FIO2 = (EditText) getView().findViewById(R.id.inputFI02);
        Tidal_Vol_Edit = (EditText) getView().findViewById(R.id.inputTidalVolume);
        Resp_Rate = (EditText) getView().findViewById(R.id.inputRespRate);
        PEEPSet = (EditText) getView().findViewById(R.id.inpuPEEP);
        IERatio = (EditText) getView().findViewById(R.id.inputIE);
        MaxPlanPress = (EditText) getView().findViewById(R.id.inputMaxPlateau);

        client = new OkHttpClient();
        SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        int FIO2NEW = prefs.getInt("dataFIO2", 0);
        int TidalNEW = prefs.getInt("dataTidalVol", 0);
        int RespNEW = prefs.getInt("dataRespRate", 0);
        int PEEPNEW = prefs.getInt("dataPEEP", 0);
        int IENEW = prefs.getInt("dataIERation", 0);
        int MaxPlanNEW = prefs.getInt("dataMaxPlanPress", 0);

        FIO2.setText(String.valueOf(FIO2NEW));
        Tidal_Vol_Edit.setText(String.valueOf(TidalNEW));
        Resp_Rate.setText(String.valueOf(RespNEW));
        PEEPSet.setText(String.valueOf(PEEPNEW));
        IERatio.setText(String.valueOf(IENEW));
        MaxPlanPress.setText(String.valueOf(MaxPlanNEW));

        ChangeParameter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share_prefs();
            }
        });

        Timed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FIO2.setEnabled(true);
                FIO2.setTextColor(Color.parseColor("#FFFFFF"));
                Tidal_Vol_Edit.setEnabled(false);
                Tidal_Vol_Edit.setTextColor(Color.parseColor("#E61725"));
                Resp_Rate.setEnabled(true);
                Resp_Rate.setTextColor(Color.parseColor("#FFFFFF"));
                PEEPSet.setEnabled(true);
                PEEPSet.setTextColor(Color.parseColor("#FFFFFF"));
                IERatio.setEnabled(true);
                IERatio.setTextColor(Color.parseColor("#FFFFFF"));
                MaxPlanPress.setEnabled(false);
                MaxPlanPress.setTextColor(Color.parseColor("#E61725"));
            }
        });

        Spontaneous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FIO2.setEnabled(true);
                FIO2.setTextColor(Color.parseColor("#FFFFFF"));
                Tidal_Vol_Edit.setEnabled(true);
                Tidal_Vol_Edit.setTextColor(Color.parseColor("#FFFFFF"));
                Resp_Rate.setEnabled(false);
                Resp_Rate.setTextColor(Color.parseColor("#E61725"));
                PEEPSet.setEnabled(true);
                PEEPSet.setTextColor(Color.parseColor("#FFFFFF"));
                IERatio.setEnabled(false);
                IERatio.setTextColor(Color.parseColor("#E61725"));
                MaxPlanPress.setEnabled(true);
                MaxPlanPress.setTextColor(Color.parseColor("#FFFFFF"));
            }
        });

        Combined.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FIO2.setEnabled(true);
                FIO2.setTextColor(Color.parseColor("#FFFFFF"));
                Tidal_Vol_Edit.setEnabled(true);
                Tidal_Vol_Edit.setTextColor(Color.parseColor("#FFFFFF"));
                Resp_Rate.setEnabled(true);
                Resp_Rate.setTextColor(Color.parseColor("#FFFFFF"));
                PEEPSet.setEnabled(true);
                PEEPSet.setTextColor(Color.parseColor("#FFFFFF"));
                IERatio.setEnabled(true);
                IERatio.setTextColor(Color.parseColor("#FFFFFF"));
                MaxPlanPress.setEnabled(true);
                MaxPlanPress.setTextColor(Color.parseColor("#FFFFFF"));
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
            }
        });

        chartmChart();
        chartmChart1();

//        long now = 0;
//        now = System.currentTimeMillis();
//                if(now >= 600000){
//                    output.setText(R.string.Default);
//                    now = 0;
//                    mChart.clearvalue();
//                    mChart1.clearvalue();
//                    mChart2.clearvalue();
//                }
    }

    private void updateDataMonitoring() {
        TextView Pressure = Objects.requireNonNull(getView()).findViewById(R.id.valuePeakPressure);
        TextView PEEP = getView().findViewById(R.id.valuePEEP);
        TextView MinVolExp = getView().findViewById(R.id.valueMinuteVolumeExp);
        TextView TidalVolExp = getView().findViewById(R.id.valueTidalVolumeExp);
        TextView TidalVolIns = getView().findViewById(R.id.valueTidalVolumeIns);

        Pressure.setText(String.valueOf(17));
        PEEP.setText(String.valueOf(33));
        MinVolExp.setText(String.valueOf(22));
        TidalVolExp.setText(String.valueOf(37));
        TidalVolIns.setText(String.valueOf(31));
    }

    public void chartmChart(){
        mChart = getView().findViewById(R.id.line_chart);
        mChart.setOnChartValueSelectedListener(this);
        // enable description text
        mChart.getDescription().setEnabled(true);
        // enable touch gestures
        mChart.setTouchEnabled(true);
        // enable scaling and dragging
        Description description = new Description();
        description.setText("Tekanan Sensor di Masker");
        description.setTextColor(Color.WHITE);
        description.setTextSize(8);
        mChart.setDescription(description);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(false);
        // set an alternative background color
//        mChart.setBackgroundColor(Color.BLACK);
        mChart.setDrawBorders(true);
        mChart.setBorderColor(Color.WHITE);
        mChart.setBorderWidth(1);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);
        // add empty data
        mChart.setData(data);
        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();
        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
//        l.setTypeface(tfLight);
        l.setTextColor(Color.WHITE);
        XAxis xl = mChart.getXAxis();
//        xl.setTypeface(tfLight);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
        YAxis leftAxis = mChart.getAxisLeft();
//        leftAxis.setTypeface(tfLight);
        leftAxis.setTextColor(Color.WHITE);
//        leftAxis.setAxisMaximum(100);
//        leftAxis.setAxisMinimum(-50);
        leftAxis.setDrawGridLines(true);
        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    public void chartmChart1(){
        mChart1 = getView().findViewById(R.id.line_chart1);
        mChart1.setOnChartValueSelectedListener(this);
        mChart1.getDescription().setEnabled(false);
        mChart1.setTouchEnabled(true);
        Description description = new Description();
        description.setText("Aliran Udara");
        description.setTextColor(Color.WHITE);
        description.setTextSize(8);
        mChart1.setDescription(description);
        mChart1.setDragEnabled(true);
        mChart1.setScaleEnabled(true);
        mChart1.setDrawGridBackground(false);
        mChart1.setPinchZoom(false);
//        mChart1.setBackgroundColor(Color.BLACK);
        mChart1.setDrawBorders(true);
        mChart1.setBorderColor(Color.WHITE);
        mChart1.setBorderWidth(1);
        LineData data1 = new LineData();
        data1.setValueTextColor(Color.WHITE);
        mChart1.setData(data1);
        Legend l = mChart1.getLegend();
        l.setForm(Legend.LegendForm.LINE);
//        l.setTypeface(tfLight);
        l.setTextColor(Color.WHITE);
        XAxis xl = mChart1.getXAxis();
//        xl.setTypeface(tfLight);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
//        xl.setLabelCount(10);
        YAxis leftAxis = mChart1.getAxisLeft();
//        leftAxis.setTypeface(tfLight);
        leftAxis.setTextColor(Color.WHITE);
//        leftAxis.setAxisMaximum(100f);
//        leftAxis.setAxisMinimum(-50f);
        leftAxis.setDrawGridLines(true);
        YAxis rightAxis = mChart1.getAxisRight();
        rightAxis.setEnabled(false);
    }

    public LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Tekanan Masker");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(ColorTemplate.getHoloBlue());
        set.setLineWidth(1f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        return set;
    }
    public LineDataSet createSet1() {
        LineDataSet set1 = new LineDataSet(null, "Aliran Udara");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setColor(Color.rgb(0, 255, 0));
        set1.setCircleColor(ColorTemplate.getHoloBlue());
        set1.setLineWidth(1f);
        set1.setCircleRadius(4f);
        set1.setFillAlpha(65);
        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setHighLightColor(Color.rgb(244, 117, 117));
        set1.setValueTextColor(Color.WHITE);
        set1.setValueTextSize(9f);
        set1.setDrawValues(false);
        set1.setDrawCircles(false);
        return set1;
    }

    public void addEntry(float value) {
        LineData data = mChart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(), value), 0);
//            data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 76) + 30f), 0);
//            System.out.println(set.getEntryCount());
            data.notifyDataChanged();
            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();
            // limit the number of visible entries
//            mChart.setVisibleXRangeMaximum(400);
            // move to the latest entry
            mChart.setVisibleXRange(0,400);
            mChart.moveViewToX(data.getEntryCount());
            mChart.invalidate();
            // this automatically refreshes the chart (calls invalidate())
            // AxisDependency.LEFT);
        }
    }

    public void addEntry1(float value) {
        LineData data1 = mChart1.getData();
        if (data1 != null) {
            ILineDataSet set = data1.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well
            if (set == null) {
                set = createSet1();
                data1.addDataSet(set);
            }
            data1.addEntry(new Entry(set.getEntryCount(), value), 0);
//            data1.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 45) + 21f), 0);
//            System.out.println(set.getEntryCount());
            data1.notifyDataChanged();
            // let the chart know it's data has changed
            mChart1.notifyDataSetChanged();
            // limit the number of visible entries
            mChart1.setVisibleXRange(0,400);
            // move to the latest entry
            mChart1.moveViewToX(data1.getEntryCount());
            mChart1.invalidate();
            // this automatically refreshes the chart (calls invalidate())
            // chart.moveViewTo(data1.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    public void start() {
        Request request = new Request.Builder().url("ws://192.168.137.201:80/test").build();
        EchoWebSocketListener listener = new EchoWebSocketListener();
        ws = client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();
    }

    public void stop(){
        ws.close(1000,null);
    }

    public void JSON_Parameter(){
        JSONObject Kirim_Parameter = new JSONObject();
        try {
            Kirim_Parameter.put("dataFIO2", dataFI02);
            Kirim_Parameter.put("dataTidalVol", dataTidalVol);
            Kirim_Parameter.put("dataRespRate", dataRespRate);
            Kirim_Parameter.put("dataPEEP", dataPEEP);
            Kirim_Parameter.put("dataIERation", dataIERatio);
            Kirim_Parameter.put("dataMaxPlanPress", dataMaxPlanPress);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ws.send(String.valueOf(Kirim_Parameter));
    }

    public void share_prefs(){
        dataFI02 = Integer.parseInt(String.valueOf(FIO2.getText()));
        dataTidalVol = Integer.parseInt(String.valueOf(Tidal_Vol_Edit.getText()));
        dataRespRate = Integer.parseInt(String.valueOf(Resp_Rate.getText()));
        dataPEEP = Integer.parseInt(String.valueOf(PEEPSet.getText()));
        dataIERatio = Integer.parseInt(String.valueOf(IERatio.getText()));
        dataMaxPlanPress = Integer.parseInt(String.valueOf(MaxPlanPress.getText()));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ClinicalFragment.this);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt("dataFIO2", dataFI02);
        editor.putInt("dataTidalVol", dataTidalVol);
        editor.putInt("dataRespRate", dataRespRate);
        editor.putInt("dataPEEP", dataPEEP);
        editor.putInt("dataIERation", dataIERatio);
        editor.putInt("dataMaxPlanPress", dataMaxPlanPress);
        editor.apply();

        Toast.makeText(this,"Data Terganti dan Tersimpan", Toast.LENGTH_SHORT).show();
    }

    public void output(String txt) {
        runOnUiThread(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                output.setText(txt);
//                String[] isisemua = output.getText().toString().split("\\}");
//                String isi = isisemua[isisemua.length - 1] + "}";
                try {
                    String isi = output.getText().toString();
                    JSONObject OBJ = new JSONObject(isi);
                    maskP= OBJ.optDouble("maskP", 0.0);
                    flow = OBJ.optDouble("flow", 0.0);
                    addEntry((float) maskP);
                    addEntry1((float) flow);

                    PeakPress = OBJ.optDouble("PeakPress", 0.0);
                    PEEP = OBJ.optDouble("PEEP", 0.0);
                    Tidal_Vol_INS = OBJ.optDouble("Tidal_Vol_INS", 0.0);
                    Tidal_Vol_EXP = OBJ.optDouble("Tidal_Vol_EXP", 0.0);
                    Min_Vol_EXP = OBJ.optDouble("Min_Vol_EXP", 0.0);
//                        Thread.sleep(500);
                    NilaiPeakPressure.setText(Integer.toString((int) PeakPress));
                    PEEPText.setText(Integer.toString((int) PEEP));
                    INS_View.setText(Integer.toString((int) Tidal_Vol_INS));
                    EXP_View.setText(Integer.toString((int) Tidal_Vol_EXP));
                    Min_View.setText(Double.toString(Min_Vol_EXP));

                } catch (JSONException e) {
                    e.printStackTrace();
//                    ((EditText) MainActivity.this.findViewById(R.id.editText)).setText("\n---Pesan---\n" + e.getMessage() + "\n---Errornya---\n" + e.toString());
                }
            }
        });

    }

    private void runOnUiThread(Runnable runnable) {
    }

}

//    private void dateAndTime(){
//        TextView displayDate = getView().findViewById(R.id.dateView);
//        TextView displayTime = getView().findViewById(R.id.timeView);
//        Calendar calendar = Calendar.getInstance();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM");
//        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a");
//        String date = dateFormat.format(calendar.getTime());
//        String time = timeFormat.format(calendar.getTime());
//        displayDate.setText(date);
//        displayTime.setText(time);
//    }



