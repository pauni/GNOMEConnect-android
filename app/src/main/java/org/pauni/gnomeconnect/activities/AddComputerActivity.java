package org.pauni.gnomeconnect.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.pauni.gnomeconnect.R;
import org.pauni.gnomeconnect.adapters.ComputerListAdapter;
import org.pauni.gnomeconnect.models.Computer;
import org.pauni.gnomeconnect.network.GnomeLover;
import org.pauni.gnomeconnect.network.GnomeSpotter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 *      The AddComputerActivity, as the name suggests, >discovers<
 *      Computers running GNOME Connect Desktop. It shows the computers
 *      found by the GnomeSpotter in a ListView. The user can click to add them.
 */

public class AddComputerActivity extends AppCompatActivity {
    public static int SEARCH_TIME = 60*1000; // Search for 1 min after onCreate()/ib_refresh click


    // Discovering computers
    BroadcastReceiver receiver;
    GnomeSpotter spotter;


    // List of computers
    ArrayList<Computer> computers = new ArrayList<>();
    ComputerListAdapter adapter;
    ListView    lv_discoveries;
    View        footer; // footer for lv_discoveries



    // Just some views...
    ProgressBar pb_searching;
    ImageButton ib_refresh;
    Toolbar     toolbar;



    /*
     *       OVERRIDE METHODS
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discovery);
        setSupportActionBar(toolbar);


        initializeViews();
        registerReceiver();
        registerListeners();

        spotter = new GnomeSpotter(this);
        adapter = new ComputerListAdapter(this, computers);


        // set adapter and loading/refresh layout as footer view
        lv_discoveries.setAdapter(adapter);
        lv_discoveries.addFooterView(footer);


        // after everything is set, start searching the network for GNOME Connect Desktops
        startSearchingComputers();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
        spotter.stopSpotting();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    /*
         *       VIEWS AND LISTENERS
         */
    private void initializeViews() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Stringextra = jsonString of ComputerObj generated by jackson
                displayComputer(intent.getStringExtra(GnomeSpotter.EXTRA_COMPUTER_INFO));
            }
        };

        // pb_footer first because pb_searching and ib_refresh are children of it
        footer = View.inflate(this, R.layout.loading_refresh_footer, null);
        ib_refresh     = footer.findViewById(R.id.ib_refresh);
        pb_searching   = footer.findViewById(R.id.pb_searching);

        lv_discoveries = findViewById(R.id.lv_discoveries);
        toolbar        = findViewById(R.id.toolbar_discovery);
        toolbar.setTitle("Computer hinzufügen");
    }

    private void registerListeners() {
        /*
         *      ITEM CLICK LISTENER FOR LIST OF AVAILABLE COMPUTERS
         */
        lv_discoveries.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // get the computer item the user clicked on
                Computer selectedComputer = (Computer) adapterView.getItemAtPosition(position);

                GnomeLover lover = new GnomeLover(AddComputerActivity.this, view);
                lover.setLovedOne(selectedComputer);
                lover.sendPairRequest();

            }
        });


        /*
         *      BUTTON CLICK LISTENER FOR REFRESH BUTTON
         */
        ib_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSearchingComputers();
            }
        });

    }








    /*
     *       SEARCH, SHOW, CONNECT, SAVE COMPUTERS
     */
    private void startSearchingComputers() {
        spotter.startSpotting();

        // show loading circle & hide refresh button
        pb_searching.setVisibility(View.VISIBLE);
        ib_refresh.setVisibility(View.INVISIBLE);



        // stop search after SEARCH_TIME ms
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopSearchingComputers();
            }
        }, SEARCH_TIME);
    }

    private void stopSearchingComputers() {
        spotter.stopSpotting(); // stop scanning for computers

        // hide loading circle & show refresh button
        pb_searching.setVisibility(View.INVISIBLE);
        ib_refresh.setVisibility(View.VISIBLE);
    }

    private void displayComputer(String jackson) {
        // show the found computer in the listView
        try {
            // Convert jackson-string back to Computer-obj
            ObjectMapper mapper = new ObjectMapper();
            Computer computer = mapper.readValue(jackson, Computer.class);

            // add computer to list
            computers.add(computer);

            // remove duplicates
            Set<Computer> computersNoDup = new HashSet<>(computers);
            computers = new ArrayList<>(computersNoDup);

            adapter.notifyDataSetChanged();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    /*
     *       ADDING & REMOVING BROADCAST RECEIVER
     */
    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(GnomeSpotter.ACTION_DESKTOP_DISCOVERED);
        registerReceiver(receiver, filter);
    }

    private void unregisterReceiver() {
        unregisterReceiver(receiver);

    }
}
