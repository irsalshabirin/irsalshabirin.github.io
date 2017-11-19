package co.dust.wakaiparser;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import co.dust.wakaiparser.databinding.ActivityMainBinding;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);


        binding.etInput.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                v.getParent().requestDisallowInterceptTouchEvent(true);
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                return false;
            }
        });

        binding.btnParse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isValidInput()) {
                    try {
                        doParse();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        binding.tvOutput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(MainActivity.this)
                        .items(Arrays.asList("Copy"))
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                doCopyToClipboard();
                            }
                        })
                        .show();
            }
        });
    }

    private void doCopyToClipboard() {
        //for copy
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("data", binding.tvOutput.getText().toString());

        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "berhasil di copy", Toast.LENGTH_SHORT).show();
    }

    private void doParse() throws Exception {

        String str = binding.etInput.getText().toString().trim().toLowerCase();
        BufferedReader bufReader = new BufferedReader(new StringReader(str));
        String line;

        HashMap<String, String> gender = new HashMap<>();
        gender.put("ce", "Cewek");
        gender.put("co", "Cowok");

        HashMap<String, Set<String>> hashMap = new HashMap<>();

        while ((line = bufReader.readLine()) != null) {
            // System.out.println(line);

            if (line.startsWith("ready size")) {
                Set<String> setCodes = new LinkedHashSet<>();
                String[] split = line.split(":");
                String type = "";
                String size = "";

                if (split[0] != null && !split[0].equals("")) {
                    // System.out.println("split 0 : " + split[0]);
                    if (split[0].contains("ce")) {
                        type = "ce";
                    } else if (split[0].contains("co")) {
                        type = "co";
                    } else {
                        if (BuildConfig.DEBUG) {
                            // System.out.println("Tidak ada tipe cowok / cewek");
                            Log.e(TAG, "Tidak ada tipe cowok / cewek");
                        }

                        throw new Exception();
                    }

                    size = split[0].replaceAll("[^0-9]+", "");
                    // System.out.println("size : " + size);
                }

                // continue;
                if (split[1] != null && !split[1].equals("")) {
                    // String codes = split[1].replaceAll("[a-zA-Z ]+", "");
                    String codes = split[1].replaceAll("[^0-9,-]+", "");
                    //System.out.println("code : " + codes);

                    String[] code = codes.split(",");
                    setCodes.addAll(checkCode(code, type));
                }

                for (String setCode : setCodes) {
                    if (hashMap.containsKey(setCode)) {
                        Set<String> sizes = hashMap.get(setCode);
                        sizes.add(size);
                        hashMap.put(setCode, sizes);
                    } else {
                        Set<String> sizes = new LinkedHashSet<>();
                        sizes.add(size);
                        hashMap.put(setCode, sizes);
                    }
                }
            }
        }

        SortedSet<String> keys = new TreeSet<>(hashMap.keySet());
        String output = "";
        int i = 1;
        for (String key : keys) {
            Set<String> value = hashMap.get(key);
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "key : " + key + ", val : " + value.toString());
                // System.out.println("key : " + key + ", val : " + value.toString());
            }

            // key = 121:ce, 124:co
            output += i + ".\t\t" + key.split(":")[0] + " : " + gender.get(key.split(":")[1]) + ",\t\tsize : " + value.toString() + "\n";
            i++;
        }

        binding.tvOutput.setText(output);
    }

    private static ArrayList<String> checkCode(String[] code, String type) {
        ArrayList<String> cc = new ArrayList<>();

        for (String string : code) {
            if (string.contains("-")) {
                String[] c = string.split("-");
                int code1, code2;

                try {
                    code1 = Integer.parseInt(c[0]);
                    code2 = Integer.parseInt(c[1]);
                } catch (Exception e) {
                    System.out.println("gagal parsing integer");
                    throw e;
                }

                if (code1 > code2) {
                    int temp = code2;
                    code2 = code1;
                    code1 = temp;
                }

                for (int j = code1; j <= code2; j++) {
                    cc.add(String.format("%03d", j) + ":" + type);
                }

            } else {
                cc.add(String.format("%03d", Integer.parseInt(string)) + ":" + type);
            }
        }

        return cc;
    }

    private boolean isValidInput() {
        if (binding.etInput.getText().toString().trim().equals("")) {
            binding.tilInput.setErrorEnabled(true);
            binding.tilInput.setError("Harus Diisi");
            return false;
        } else {
            binding.tilInput.setErrorEnabled(false);
            binding.tilInput.setError("");
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (binding.etInput.getText().toString().trim().equals("")) {
            // If it does contain data, decide if you can handle the data.
            if (!(clipboard.hasPrimaryClip())) {
            } else if (!(clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN))) {
                // since the clipboard has data but it is not plain text
            } else {

                //since the clipboard contains plain text.
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

                // Gets the clipboard as text.
                String pasteData = item.getText().toString();
                binding.etInput.setText(pasteData);
            }
        }
    }
}
