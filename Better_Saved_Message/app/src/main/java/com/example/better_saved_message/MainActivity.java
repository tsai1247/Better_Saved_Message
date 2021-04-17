package com.example.better_saved_message;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {

    // copy-object
    Intent appIntent;
    AtomicReference<ClipboardManager> clipboard;
    AtomicReference<ClipData> clip;

    private EditText tf;
    private final TextView[] tvs = new TextView[6];
    private final Button[] labels = new Button[3];
    private Button btn_enter;
    private Button btn_clear;
    private Button btn_search;
    private int Cur_Classification = 0;
    private int Cur_Selected_TextView = -1;

    private final String[][] localData = new String[labels.length][tvs.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // init
        InitDeclare();
        InitAssign();
        btn_clear.setOnClickListener(e->{
            for(TextView t:tvs)
                t.setText("");
            Arrays.fill(localData[Cur_Classification], "");
        });

        btn_search.setOnClickListener(e->{
            if(Cur_Selected_TextView!=-1 && !tvs[Cur_Selected_TextView].getText().equals(""))
            {
                String s_uri;
                Intent intent;
                if(Cur_Classification==2)
                {
                    try {
                        intent = getPackageManager().getLaunchIntentForPackage("com.termux");
                        startActivity(intent);
                    } catch (Exception ex) {
                        s_uri = "https://www.google.com/search?q="
                                + tvs[Cur_Selected_TextView].getText().toString();
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse((s_uri)));
                        startActivity(intent);
                    }
                }
                else {
                    if (Cur_Classification == 0) {
                        s_uri = tvs[Cur_Selected_TextView].getText().toString();
                        if (!s_uri.contains("http"))
                            s_uri = "https://" + s_uri;
                    } else {
                        s_uri = "https://www.google.com/search?q="
                                + tvs[Cur_Selected_TextView].getText().toString();
                    }
                    Uri uri = Uri.parse(s_uri);
                    intent = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        startActivity(intent);
                    } catch (Exception ex) {
                        uri = Uri.parse("https://www.google.com/search?q="
                                + tvs[Cur_Selected_TextView].getText().toString());
                        intent.setData(uri);
                        startActivity(intent);
                    }
                }
            }
        });

        btn_enter.setOnClickListener(e->{
            if(Cur_Selected_TextView!=-1 && !tvs[Cur_Selected_TextView].getText().equals("")) {
                localData[Cur_Classification][Cur_Selected_TextView] = String.valueOf(tf.getText());
                tvs[Cur_Selected_TextView].setText(localData[Cur_Classification][Cur_Selected_TextView]);
                SetSelected(false);
                Cur_Selected_TextView = -1;
            }
            else
            {
                for(int i=0; i<tvs.length; i++)
                {
                    if(tvs[i].getText().equals(""))
                    {
                        tvs[i].setText(tf.getText());
                        localData[Cur_Classification][i] = String.valueOf(tf.getText());
                        break;
                    }
                }
            }
            writelocalfile();
            tf.setText("");
        });

        for(int i=0; i<tvs.length; i++) {
            int finalI = i;
            tvs[i].setOnClickListener(e -> {
                copy(tvs[finalI].getText().toString());
                if(Cur_Selected_TextView!=-1) {
                    SetSelected(false);
                }
                if(tvs[finalI].getText().equals(""))
                {
                    Cur_Selected_TextView = -1;
                }
                else
                {
                    Cur_Selected_TextView = finalI;
                    SetSelected(true);
                }
            });
        }

        for(int i=0; i<labels.length; i++)
        {
            int finalI = i;
            labels[i].setOnClickListener(e->{
                labels[Cur_Classification].setTextColor(Color.WHITE);
                Cur_Classification = finalI;
                labels[Cur_Classification].setTextColor(Color.parseColor("#FF0000"));

                writeToFile("TextData_3.txt", String.valueOf(finalI));
                for(int j=0; j<tvs.length; j++)
                {
                    tvs[j].setText(localData[Cur_Classification][j]);
                }
                SetSelected(false);
                Cur_Selected_TextView = -1;
            });

        }

    }

    private void SetSelected(boolean b)
    {
        if(Cur_Selected_TextView!=-1) {
            if (b) {
                tvs[Cur_Selected_TextView].setTypeface(null, Typeface.BOLD);
                tvs[Cur_Selected_TextView].setTextColor(Color.parseColor("#FF0000"));
            } else {
                tvs[Cur_Selected_TextView].setTypeface(null, Typeface.NORMAL);
                tvs[Cur_Selected_TextView].setTextColor(Color.parseColor("#000000"));
            }
        }
    }

    private void writelocalfile()
    {
        StringBuilder data = new StringBuilder();
        for (String s : localData[Cur_Classification]) {
            data.append(s).append("\n");
        }

        writeToFile("TextData_" + Cur_Classification + ".txt", data.toString());

    }
    private void InitAssign() {

        for(int c = 0; c<labels.length; c++) {
            String[] text_data = readFromFile("TextData_" + c + ".txt").split("\n");
            for (int i = 0; i < tvs.length; i++) {
                if(i<Math.min(text_data.length-1, tvs.length))
                    localData[c][i] = text_data[i+1];
                else
                    localData[c][i] = "";
            }
        }

        Log.e("TAG", readFromFile("TextData_3.txt"));
        String[] tmp = readFromFile("TextData_3.txt").split("\n");
        if(tmp.length<2)
            tmp = new String[]{"0", "0"};
        else if(tmp[1].equals(""))
            tmp[1] = "0";
        Cur_Classification = Integer.parseInt(tmp[1]);
        labels[Cur_Classification].setTextColor(Color.parseColor("#FF0000"));
        runOnUiThread(()->{
            for(int i=0; i<localData[Cur_Classification].length; i++)
            {
                tvs[i].setText(localData[Cur_Classification][i]);
            }
        });



    }

    private void InitDeclare()
    {
        // copy object
        appIntent = new Intent(this, com.example.better_saved_message.MainActivity.class);
        clipboard = new AtomicReference<>((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE));
        clip = new AtomicReference<>(ClipData.newIntent("Intent", appIntent));

        // text for debugger
//        dbg = findViewById(R.id.dbgtext);

        // view object
        tf = findViewById(R.id.tf);
        tvs[0] = findViewById(R.id.tv1);
        tvs[1] = findViewById(R.id.tv2);
        tvs[2] = findViewById(R.id.tv3);
        tvs[3] = findViewById(R.id.tv4);
        tvs[4] = findViewById(R.id.tv5);
        tvs[5] = findViewById(R.id.tv6);

        labels[0] = findViewById(R.id.label_link);
        labels[1] = findViewById(R.id.label_text);
        labels[2] = findViewById(R.id.label_cmd);
        btn_enter = findViewById(R.id.btn_enter);
        btn_clear = findViewById(R.id.btn_clear);
        btn_search = findViewById(R.id.btn_search);


    }

    public void copy(String str)
    {
        clipboard.set((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE));
        clip.set(ClipData.newPlainText(null, str));
        clipboard.get().setPrimaryClip(clip.get());
    }

    private void writeToFile(String filename, String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getApplicationContext().openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String readFromFile(String filename) {

        String ret = "";

        try {
            InputStream inputStream = getApplicationContext().openFileInput(filename);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

}





