package com.example.apps_ventilator;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

public class ClinicalFragment extends Fragment{

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

}

