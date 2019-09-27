package com.eostek.videoviewcontroller;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.eostek.videoviewcontroller.search.QuickScanManager;
import com.eostek.videoviewcontroller.search.ScanResult;
import com.eostek.videoviewcontroller.search.VideoNames;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private List<String> list = new ArrayList<>();
    private ArrayAdapter arrayAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ListView listView = (ListView) findViewById(R.id.list_item);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1,list);
        listView.setAdapter(arrayAdapter);
        QuickScanManager.getQuickScanManager().Init(this).getAllResult(
                VideoNames.nameList,
                new QuickScanManager.OnResultListener() {
                    @Override
                    public void ScanSuccess(List<ScanResult> lists) {
                        list.clear();
                        for(ScanResult scanResult:lists){
                            list.add(scanResult.getPath());
                        }
                        arrayAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void ScanError(String msg) {
                        Toast.makeText(getApplicationContext(),msg, Toast.LENGTH_SHORT).show();
                    }
                });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(MainActivity.this, PlayActivity.class);
                        i.putExtra("uri", (String) arrayAdapter.getItem(position));
                startActivity(i);
            }
        });
    }

    @Override
    protected void onDestroy() {
        QuickScanManager.getQuickScanManager().remove();
        super.onDestroy();
    }
}
