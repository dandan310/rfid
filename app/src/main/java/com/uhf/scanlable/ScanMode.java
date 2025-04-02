package com.uhf.scanlable;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.rfid.trans.ReadTag;
import com.rfid.trans.ReaderParameter;
import com.rfid.trans.TagCallback;
import com.uhf.scanlable.entity.InboundEntity;
import com.uhf.scanlable.entity.Res;
import com.uhf.scanlable.entity.Resp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScanMode extends Activity implements OnClickListener, OnItemSelectedListener {

    private static final int PRINTER_VENDOR_ID = 26728;
    private static final String ACTION_USB_PERMISSION = "com.example.qrcodedemo.USB_PERMISSION";

    @Nullable
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        return super.onWindowStartingActionMode(callback);
    }

    public final Set<String> scannedCurrentDay = new HashSet<>();
    Button scan;
    SwipeListView listView;
    TextView txNum;
    TextView txTime;
    CheckBox chk;
    long beginTime = 0;
    private Timer timer;
    private MyAdapter myAdapter;
    private Handler mHandler;
    private boolean isCanceled = true;
    private static final int SCAN_INTERVAL = 20;
    private static final int MSG_UPDATE_LISTVIEW = 0;
    private static final int MSG_UPDATE_TIME = 1;
    private static final int MSG_UPDATE_BUTTON = 2;
    private static final int MSG_SHOW_RESULT = 3;
    private int MaxAntennaNum = 4;
    private ApiInterface apiService;

    public static class InventoryTagMap {
        @Expose(serialize = false)
        public Boolean isRegistered = false;
        @SerializedName("epc")
        public String strEPC;
        @Expose(serialize = false)
        public String strMem;
        public String remark;
        public String hotel;
        public String hotelId;
        public Integer type;
        public String typeName;
        public Integer classification;
        public String classificationName;
        @Expose(serialize = false)
        public int nReadCount;

        @Override
        public String toString() {
            return "InventoryTagMap{" +
                    "isRegistered=" + isRegistered +
                    ", strEPC='" + strEPC + '\'' +
                    ", strMem='" + strMem + '\'' +
                    ", remark='" + remark + '\'' +
                    ", hotel='" + hotel + '\'' +
                    ", hotelId=" + hotelId +
                    ", type=" + type +
                    ", typeName='" + typeName + '\'' +
                    ", classification=" + classification +
                    ", classificationName='" + classificationName + '\'' +
                    ", nReadCount=" + nReadCount +
                    '}';
        }

        public String getStrEPC() {
            return strEPC;
        }

        public String getRemark() {
            return remark;
        }

        public String getHotelId() {
            return hotelId;
        }

        public Integer getType() {
            return type;
        }

        public Integer getClassification() {
            return classification;
        }

    }

    public static List<InventoryTagMap> lsTagList = new ArrayList<>();
    public static Map<String, InventoryTagMap> tagMap = new HashMap<>();
    private List<InventoryTagMap> data;

    private void rfidInit() {
        initRetrofit();
    }

    private void initRetrofit() {
        // 初始化 Retrofit
        // 创建日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // 记录完整请求和响应

        // 配置 OkHttpClient
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor) // 添加日志拦截器
                .build();
        this.apiService = new Retrofit.Builder()
//                .baseUrl("https://www.shaolin.site/")
                .baseUrl("http://192.168.1.171:8080/retrieval/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build().create(ApiInterface.class);
    }

    private boolean sendData() {
        if (lsTagList.isEmpty()) {
            return true;
        }
        InboundEntity inboundEntity = new InboundEntity();
        List<String> data = lsTagList.stream().map(InventoryTagMap::getStrEPC).collect(Collectors.toList());
        inboundEntity.setDataList(data);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> {
            try {
                Call<Resp> call = apiService.retrieval(inboundEntity);
                Response<Resp> response = call.execute();
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        });

        try {
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        rfidInit();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        AutoSubmitManager autoSubmitManager = new AutoSubmitManager();
        autoSubmitManager.startAutoSubmit();
        NightlyTaskManager nightlyTaskManager = new NightlyTaskManager();
        nightlyTaskManager.startTask();
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.query);
            scan = (Button) findViewById(R.id.button_scan);
            scan.setOnClickListener(this);
            listView = (SwipeListView) findViewById(R.id.tag_real_list_view);
            data = new ArrayList<InventoryTagMap>();
            txNum = (TextView) findViewById(R.id.tx_num);
            txTime = (TextView) findViewById(R.id.tx_time);
            chk = (CheckBox) findViewById(R.id.check_phase);
            mHandler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    // TODO Auto-generated method stub
                    switch (msg.what) {
                        case MSG_UPDATE_LISTVIEW:
                            if (isCanceled) return;
                            data = lsTagList;
                            if (myAdapter == null) {
                                myAdapter = new MyAdapter(ScanMode.this, new ArrayList(data));
                                listView.setAdapter(myAdapter);
                            } else {
                                myAdapter.mList = new ArrayList(data);
                            }
                            txNum.setText(String.valueOf(myAdapter.getCount()));
                            myAdapter.notifyDataSetChanged();
                            break;
                        case MSG_UPDATE_TIME:
                            long endTime = System.currentTimeMillis();
                            txTime.setText(String.valueOf(endTime - beginTime));
                            break;
                        case MSG_UPDATE_BUTTON:
                            scan.setText(getString(R.string.btscan));
                            break;
                        default:
                            break;
                    }
                    super.handleMessage(msg);
                }

            };
        } catch (Exception e) {

        }
    }


    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        scan.setText(getString(R.string.btscan));
    }

    @Override
    public void onClick(View arg0) {
        try {

            if (scan.getText().toString().equals(getString(R.string.btscan))) {
                isCanceled = false;
                MsgCallback callback = new MsgCallback();
                Reader.rrlib.SetCallBack(callback);
                ReaderParameter param = Reader.rrlib.GetInventoryPatameter();

                if (chk.isChecked()) {
                    param.QValue |= 0x10;
                } else {
                    param.QValue &= 0x0F;
                }
                Reader.rrlib.SetInventoryPatameter(param);
                if (Reader.rrlib.StartRead() == 0) {
                    MaxAntennaNum = Reader.rrlib.GetInventoryPatameter().MaxAntennaNum;
                    if (myAdapter != null) {
                        txNum.setText("0");
                        txTime.setText("0");
                        myAdapter.notifyDataSetChanged();
                        mHandler.removeMessages(MSG_UPDATE_LISTVIEW);
                        mHandler.sendEmptyMessage(MSG_UPDATE_LISTVIEW);
                    }
                    beginTime = System.currentTimeMillis();
                    timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mHandler.removeMessages(MSG_UPDATE_TIME);
                            mHandler.sendEmptyMessage(MSG_UPDATE_TIME);
                        }
                    }, 0, SCAN_INTERVAL);
                    scan.setText(getString(R.string.btstop));
                    lsTagList = new ArrayList<InventoryTagMap>();
                    tagMap = new HashMap<>();
                }


            } else {
                cancelScan();
            }
        } catch (Exception e) {
            cancelScan();
        }
    }

    private void cancelScan() {
        Reader.rrlib.StopRead();
        scan.setText(getString(R.string.btstoping));
        isCanceled = true;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public class MsgCallback implements TagCallback {

        @Override
        public void tagCallback(ReadTag arg0) {
            // TODO Auto-generated method stub
            String epc = arg0.epcId.toUpperCase();
            String memid = arg0.memId.toUpperCase();
            String DevName = arg0.DevName;
            InventoryTagMap m;
            //已存在 不管
            if (scannedCurrentDay.contains(epc)) {
                return;
            }
            m = new InventoryTagMap();
            tagMap.put(epc, m);
            m.strEPC = epc;
            m.strMem = arg0.memId;
            m.nReadCount = 1;
            lsTagList.add(m);
            scannedCurrentDay.add(epc);
            mHandler.removeMessages(MSG_UPDATE_LISTVIEW);
            mHandler.sendEmptyMessage(MSG_UPDATE_LISTVIEW);
        }

        @Override
        public int tagCallbackFailed(int reason) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int BoxInvenoryCallBack(int i, int i1) {
            return 0;
        }

        @Override
        public void ReadOver() {
            mHandler.removeMessages(MSG_UPDATE_BUTTON);
            mHandler.sendEmptyMessage(MSG_UPDATE_BUTTON);
        }
    }


    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        cancelScan();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    class MyAdapter extends BaseAdapter {

        private Context mContext;
        private List<InventoryTagMap> mList;
        private LayoutInflater layoutInflater;

        // 删除数据的方法
        public void removeItem(int position) {
            mList.remove(position);
            lsTagList.remove(position);
            notifyDataSetChanged();
        }

        public MyAdapter(Context context, List<InventoryTagMap> list) {
            mContext = context;
            mList = list;
            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return mList.get(position);
        }

        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }


        @Override
        public View getView(int position, View view, ViewGroup viewParent) {
            InventoryTagMap inventoryTagMap = mList.get(position);
            assert inventoryTagMap != null;
            // TODO Auto-generated method stub
            ItemView iv = null;
            if (view == null) {
                iv = new ItemView();
                view = layoutInflater.inflate(R.layout.list, null);
                iv.tvId = (TextView) view.findViewById(R.id.id_text);
                iv.tvEpc = (TextView) view.findViewById(R.id.epc_text);
                view.setTag(iv);
            } else {
                iv = (ItemView) view.getTag();
            }

            String epc = inventoryTagMap.strEPC;
            String memid = inventoryTagMap.strMem;
//            Integer findIndex = dtIndexMap.get(epc + "," + memid);
//            assert findIndex!=null;

            iv.tvId.setText(String.valueOf(position + 1));
            if (memid == null || memid.length() == 0)
                iv.tvEpc.setText(epc);
            else
                iv.tvEpc.setText(epc + "," + memid);
//                iv.tvTime.setText(String.valueOf(mList.get(position).nReadCount));
            return view;
        }

        public class ItemView {
            TextView tvId;
            TextView tvEpc;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
                               long arg3) {
    }


    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub

    }

    public class AutoSubmitManager {
        private final Handler handler = new Handler();
        private final Runnable submitTask = new Runnable() {
            @Override
            public void run() {
                submitForm();  // 你的表单提交逻辑
                handler.postDelayed(this, 1 * 60 * 1000); // 5分钟后再次执行
            }
        };

        public void startAutoSubmit() {
            handler.postDelayed(submitTask, 1 * 60 * 1000); // 第一次执行
        }

        public void stopAutoSubmit() {
            handler.removeCallbacks(submitTask);
        }

        private void submitForm() {
            if (sendData()) {
                lsTagList = new ArrayList<>();
                myAdapter.mList = new ArrayList<>();
                myAdapter.notifyDataSetChanged();
            } else {
                Log.d("ERROR", "提交布草失败");
            }
        }
    }


    public class NightlyTaskManager {
        private final Handler handler = new Handler();
        private final Runnable taskRunnable = new Runnable() {
            @Override
            public void run() {
                executeTask(); // 执行定时任务
                scheduleNextExecution(); // 任务执行后重新调度
            }
        };

        public void startTask() {
            long delay = calculateInitialDelay(); // 计算当前时间到 22:00 的时间差
            handler.postDelayed(taskRunnable, delay);
        }

        private long calculateInitialDelay() {
            Calendar now = Calendar.getInstance();
            Calendar nextRun = Calendar.getInstance();
            nextRun.set(Calendar.HOUR_OF_DAY, 15); // 设置执行时间 22:00
            nextRun.set(Calendar.MINUTE, 45);
            nextRun.set(Calendar.SECOND, 0);

            if (now.after(nextRun)) {
                // 如果当前时间已经过了 22:00，则安排在明天晚上执行
                nextRun.add(Calendar.DAY_OF_MONTH, 1);
            }

            return nextRun.getTimeInMillis() - now.getTimeInMillis();
        }

        private void executeTask() {
            // 这里放你的任务代码，比如清理缓存
            scannedCurrentDay.clear();
            Log.d("Schedule", "Executed");
        }

        private void scheduleNextExecution() {
            handler.postDelayed(taskRunnable, 24 * 60 * 60 * 1000); // 每 24 小时执行一次
        }

        public void stopTask() {
            handler.removeCallbacks(taskRunnable);
        }
    }
}
