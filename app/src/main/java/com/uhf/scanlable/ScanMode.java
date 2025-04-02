package com.uhf.scanlable;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.rfid.trans.ReadTag;
import com.rfid.trans.ReaderParameter;
import com.rfid.trans.TagCallback;
import com.uhf.scanlable.entity.BarCode;
import com.uhf.scanlable.entity.Hotel;
import com.uhf.scanlable.entity.InboundEntity;
import com.uhf.scanlable.entity.Res;
import com.uhf.scanlable.entity.Resp;
import com.uhf.scanlable.entity.Rfid;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;


import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScanMode extends Activity implements OnClickListener, OnItemClickListener, OnItemSelectedListener {
    private static final int PRINTER_VENDOR_ID = 26728;
    private static final String ACTION_USB_PERMISSION = "com.example.qrcodedemo.USB_PERMISSION";
    private static String[] hotelOptions;

    private static final String[] typeOptions = {"请选择类型", "传统", "共享"};
    private static final String SHARE = "共享";

    private final Map<String, Integer> typeMap = new HashMap<>();
    private final Map<String, String> hotelMap = new LinkedHashMap<>();
    private final Map<String, String> hotelNameMap = new LinkedHashMap<>();
    private final Map<String, Integer> hotelIndexMap = new LinkedHashMap<>();

    private final static InboundEntity lastInbound = new InboundEntity();

    static {
        lastInbound.setType(0);
        lastInbound.setBarCode("sdfsdf");
    }

    private UsbPrinter printer;

    @Nullable
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        return super.onWindowStartingActionMode(callback);
    }

    private static final String[] classificationOptions = {"请选择种类", "1米2床单", "1米5床单", "1米8床单", "1米2被套", "1米5被套", "1米8被套", "浴巾", "面巾", "枕套"};
    private final Map<String, Integer> classificationMap = new HashMap<>();
    ;

    private final Map<String, Rfid> epcMap = new HashMap<>();

    Button scan;
    Button btAdd;
    Button btShow;
    SwipeListView listView;
    TextView txNum;
    TextView txTime;
    CheckBox chk;
    long beginTime = 0;
    private Timer timer;
    private MyAdapter myAdapter;
    private Handler mHandler;
    private boolean isCanceled = true;

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private Bitmap qrCodeBitmap;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    Spinner s_mem;
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
    public Map<String, Integer> dtIndexMap = new LinkedHashMap<String, Integer>();

    private void rfidInit() {
        initRetrofit();
        loadData();

    }

    private void loadData() {
        if (this.apiService == null) {
            throw new Resources.NotFoundException("apiService初始化错误");
        }
        Call<Res<Rfid>> rfidList = this.apiService.getRfidList();
        rfidList.enqueue(new Callback<Res<Rfid>>() {
            @Override
            public void onResponse(Call<Res<Rfid>> call, Response<Res<Rfid>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    List<Rfid> items = response.body().getData();
                    for (Rfid item : items) {
                        epcMap.put(item.getEpc(), item);
                    }
                } else {
                    playAlarmSound(ScanMode.this);
                    Toast.makeText(ScanMode.this, "Failed to load rfid data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Res<Rfid>> call, Throwable t) {
                playAlarmSound(ScanMode.this);
                Toast.makeText(ScanMode.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        hotelMap.put("请选择酒店", null);
        Call<Res<Hotel>> hotel = this.apiService.getHotelList();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Res<Hotel>> future = executor.submit(() -> {
            try {
                Response<Res<Hotel>> response = hotel.execute();
                if (response.isSuccessful() && response.body() != null) {
                    return response.body();
                }
            } catch (IOException e) {
                playAlarmSound(ScanMode.this);
                Toast.makeText(ScanMode.this, "Failed to load hotel data", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            throw new RuntimeException("Failed to load hotel data");
        });

        try {
            Res<Hotel> items = future.get();
            for (Hotel item : items.getData()) {
                hotelMap.put(item.getName(), item.getId());
                hotelNameMap.put(item.getId(), item.getName());
            }
            hotelOptions = hotelMap.keySet().toArray(new String[0]);
            for (int i = 0; i < hotelOptions.length; i++) {
                hotelIndexMap.put(hotelOptions[i], i);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load hotel data");
        }
//        hotel.enqueue(new Callback<Res<Hotel>>() {
//            @Override
//            public void onResponse(Call<Res<Hotel>> call, Response<Res<Hotel>> response) {
//                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
//                    List<Hotel> items = response.body().getData();
//                    for (Hotel item : items) {
//                        hotelMap.put(item.getName(), item.getId());
//                        hotelNameMap.put(item.getId(), item.getName());
//                    }
//                } else {
//                    playAlarmSound(ScanMode.this);
//                    Toast.makeText(ScanMode.this, "Failed to load hotel data", Toast.LENGTH_SHORT).show();
//                }
//                hotelOptions = hotelMap.keySet().toArray(new String[0]);
//            }
//
//            @Override
//            public void onFailure(Call<Res<Hotel>> call, Throwable t) {
//                playAlarmSound(ScanMode.this);
//                Toast.makeText(ScanMode.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
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
                .baseUrl("https://www.shaolin.site/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build().create(ApiInterface.class);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        rfidInit();
        initUsb();
        initSelect();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.query);
            scan = (Button) findViewById(R.id.button_scan);
            scan.setOnClickListener(this);
            listView = (SwipeListView) findViewById(R.id.tag_real_list_view);
            listView.setOnItemClickListener(this);
            Button inbound = (Button) findViewById(R.id.btn_inbound);
            Button printBtn = (Button) findViewById(R.id.btn_print);
            Button reconnectBtn = (Button) findViewById(R.id.btn_reconnect);
            inbound.setOnClickListener(this::inbound);
            reconnectBtn.setOnClickListener(view -> this.initUsb());
            printBtn.setOnClickListener(this::rePrint);
            txNum = (TextView) findViewById(R.id.tx_num);
            txTime = (TextView) findViewById(R.id.tx_time);
            chk = (CheckBox) findViewById(R.id.check_phase);
            mHandler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    // TODO Auto-generated method stub
                    switch (msg.what) {
                        case MSG_UPDATE_LISTVIEW:
//                            if (isCanceled) return;
                            if (myAdapter == null) {
                                myAdapter = new MyAdapter(ScanMode.this, new ArrayList(lsTagList));
                                listView.setAdapter(myAdapter);
                            } else {
                                myAdapter.mList = new ArrayList(lsTagList);
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
    protected void onStart() {
        super.onStart();
        EditText remark = (EditText) findViewById(R.id.remark_select);
        remark.setOnClickListener(v -> {
            remark.requestFocus();
//                    showKeyboard(v);
        });
        // **编辑完成后保存**

        remark.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                lsTagList.stream().filter(tag -> !tag.isRegistered).forEach(inventoryTagMap -> inventoryTagMap.remark = remark.getText().toString());
                mHandler.removeMessages(MSG_UPDATE_LISTVIEW);
                mHandler.sendEmptyMessage(MSG_UPDATE_LISTVIEW);
            }
        });
        Spinner hotelSpin = (Spinner) findViewById(R.id.hotel_select);
        Spinner typeSpin = (Spinner) findViewById(R.id.type_select);
        Spinner classificationSpin = (Spinner) findViewById(R.id.classification_select);
        TextView hotelText = (TextView) findViewById(R.id.hotel_text_select);
        setSelectSpin(hotelSpin, typeSpin, classificationSpin, hotelText);


    }

    private void initSelect() {
        for (int i = 1; i < typeOptions.length; i++) {
            typeMap.put(typeOptions[i], i - 1);
        }

        for (int i = 1; i < classificationOptions.length; i++) {
            classificationMap.put(classificationOptions[i], i - 1);
        }
    }

    private void initUsb() {
        // 初始化 USB 管理器
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (connectToPrinter()) {
            printer = new UsbPrinter(usbManager, usbDevice);
        }

        // 注册 USB 权限广播接收器
//        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
//        registerReceiver(usbReceiver, filter);


    }

    public boolean connectToPrinter() {
        // 注册 USB 权限广播接收器
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);
        boolean connected = false;
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            // 检查设备是否是打印机（根据厂商 ID 和产品 ID）
            if (isPrinter(device)) {
                connected = true;
                usbDevice = device;
                PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0,
                        new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT);
                usbManager.requestPermission(usbDevice, permissionIntent);
                break;
            }
        }
        return connected;
    }

    private boolean isPrinter(UsbDevice device) {
        // 根据设备的厂商 ID 和产品 ID 判断是否是打印机
        return device.getVendorId() == PRINTER_VENDOR_ID;
    }

    private void inbound(View view) {
//        if (usbDevice == null) {
//            playAlarmSound(this);
//            Toast.makeText(ScanMode.this, "打印机未连接，无法入库", Toast.LENGTH_SHORT).show();
//            return;
//        }
        Log.d("inBound,tagList:", lsTagList.stream().map(String::valueOf).collect(Collectors.joining()));
        if (lsTagList.isEmpty()) {
            playAlarmSound(this);
            Toast.makeText(ScanMode.this, "标签页为空，无法入库", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lsTagList.stream().anyMatch(inventoryTagMap -> inventoryTagMap.classification == null)) {
            playAlarmSound(this);
            Toast.makeText(ScanMode.this, "标签种类未设置", Toast.LENGTH_SHORT).show();
            return;
        }
        List<InventoryTagMap> traditional = lsTagList.stream().filter(inventoryTagMap -> inventoryTagMap.type == 0).collect(Collectors.toList());
        List<InventoryTagMap> share = lsTagList.stream().filter(inventoryTagMap -> inventoryTagMap.type == 1).collect(Collectors.toList());
        if (!traditional.isEmpty() && !share.isEmpty()) {
            playAlarmSound(this);
            Toast.makeText(ScanMode.this, "无法同时入库 传统与共享布草", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!traditional.isEmpty()) {
            //传统类型，酒店必填
            if (traditional.stream().anyMatch(inventoryTagMap -> inventoryTagMap.hotelId == null)) {
                playAlarmSound(this);
                Toast.makeText(ScanMode.this, "传统布草，酒店必填，并且一次只能入库同一个酒店", Toast.LENGTH_SHORT).show();
                return;
            }
            //传统类型 打包的酒店必须为同一家
            List<String> hotel = traditional.stream().map(inventoryTagMap -> inventoryTagMap.hotelId).distinct().collect(Collectors.toList());
            if (hotel.size() != 1) {
                playAlarmSound(this);
                Toast.makeText(ScanMode.this, "传统布草，酒店必填，并且一次只能入库同一个酒店", Toast.LENGTH_SHORT).show();
                return;
            }
            lastInbound.setHotelId(hotel.get(0));
            lastInbound.setHotelName(traditional.get(0).hotel);
            lastInbound.setType(0);
        } else {
            lastInbound.setType(1);
        }
        //类型，型号，尺寸 必填校验 ?
        if (lsTagList.stream().anyMatch(inventoryTagMap -> inventoryTagMap.type == null || inventoryTagMap.classification == null)) {
            playAlarmSound(this);
            Toast.makeText(ScanMode.this, "类型，型号，尺寸 必填", Toast.LENGTH_SHORT).show();
            return;
        }
        lastInbound.setDataList(lsTagList);
        String barCode = sendInboundInfo(lastInbound);
        if (barCode == null || barCode.isEmpty()) {
            playAlarmSound(this);
            Toast.makeText(ScanMode.this, "入库请求失败", Toast.LENGTH_SHORT).show();
            return;
        }
        lastInbound.setBarCode(barCode);
        if (printQRCode(barCode, lastInbound.getType() == 0 ? lastInbound.getHotelName() : SHARE)) {
            renewTagMap(lsTagList);
            Toast.makeText(ScanMode.this, "入库成功", Toast.LENGTH_SHORT).show();
            tagMap = new HashMap<>();
            myAdapter.mList = new ArrayList<>();
            lsTagList = new ArrayList<InventoryTagMap>();
            myAdapter.notifyDataSetChanged();
        }
    }

    private void rePrint(View view) {
        if (usbDevice == null) {
            playAlarmSound(this);
            Toast.makeText(ScanMode.this, "打印机未连接，无法入库", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lastInbound.getBarCode() != null && !lastInbound.getBarCode().isEmpty() && lastInbound.getType() != null) {
            printQRCode(lastInbound.getBarCode(), lastInbound.getType() == 0 ? lastInbound.getHotelName() == null ? "" : lastInbound.getHotelName() : SHARE);
            return;
        }
        playAlarmSound(this);
        Toast.makeText(ScanMode.this, "无上次打印条码", Toast.LENGTH_SHORT).show();
    }

    private void renewTagMap(List<InventoryTagMap> lsTagList) {
        for (InventoryTagMap inventoryTagMap : lsTagList) {
            Rfid rfid = new Rfid(inventoryTagMap);
            epcMap.put(inventoryTagMap.strEPC, rfid);
        }
    }

    private String sendInboundInfo(InboundEntity inboundEntity) {
        Call<Resp<BarCode>> responseCall = this.apiService.inBound(inboundEntity);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            try {
                Response<Resp<BarCode>> response = responseCall.execute();
                if (response.isSuccessful() && response.body() != null && response.body().getSuccess()) {
                    return response.body().getData().getBarCode();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });

        try {
            String barCode = future.get();
            if (barCode != null && !barCode.isEmpty()) {
                return barCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

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
                    dtIndexMap = new LinkedHashMap<String, Integer>();
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

    private void setSelectSpin(Spinner hotelSpin, Spinner typeSpin, Spinner classificationSpin, TextView hotelText) {
        // 设置下拉列表
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ScanMode.this, android.R.layout.simple_spinner_item, hotelOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hotelSpin.setAdapter(adapter);
        // 监听用户选择
        hotelSpin.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                lsTagList.stream().filter(tag -> !tag.isRegistered).forEach(inventoryTagMap -> {
                    inventoryTagMap.hotelId = hotelMap.get(hotelOptions[position]); // 保存用户选择的值
                    inventoryTagMap.hotel = hotelNameMap.get(inventoryTagMap.hotelId);
                });
                mHandler.removeMessages(MSG_UPDATE_LISTVIEW);
                mHandler.sendEmptyMessage(MSG_UPDATE_LISTVIEW);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // 设置下拉列表
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(ScanMode.this, android.R.layout.simple_spinner_item, typeOptions);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpin.setAdapter(adapter1);

        // 监听用户选择
        typeSpin.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 保存用户选择的值
                if (position == 0) {
                    return;
                }
                lsTagList.stream().filter(tag -> !tag.isRegistered).forEach(inventoryTagMap -> {
                    inventoryTagMap.type = typeMap.get(typeOptions[position]);
                });
                //tradition
                if (position == 1) {
                    showHotelSpin(hotelSpin, hotelText);
                } else if (position == 2) {
                    hideHotelSpinAndClearHotel(hotelSpin, hotelText);
                }
                mHandler.removeMessages(MSG_UPDATE_LISTVIEW);
                mHandler.sendEmptyMessage(MSG_UPDATE_LISTVIEW);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        // 设置下拉列表
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(ScanMode.this, android.R.layout.simple_spinner_item, classificationOptions);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classificationSpin.setAdapter(adapter2);
        // 监听用户选择
        classificationSpin.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 保存用户选择的值
                if (position == 0) {
                    return;
                }
                lsTagList.stream().filter(tag -> !tag.isRegistered).forEach(inventoryTagMap -> {
                    inventoryTagMap.classification = classificationMap.get(classificationOptions[position]);
                });
                mHandler.removeMessages(MSG_UPDATE_LISTVIEW);
                mHandler.sendEmptyMessage(MSG_UPDATE_LISTVIEW);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void hideTextViewAndShowSpin(MyAdapter.ItemView iv) {
        iv.spType.setVisibility(View.VISIBLE);
        iv.tvType.setVisibility(View.GONE);
        iv.spClassification.setVisibility(View.VISIBLE);
        iv.tvClassification.setVisibility(View.GONE);
    }

    private void showHotelSpin(Spinner spHotel, TextView tvHotel) {
        spHotel.setVisibility(View.VISIBLE);
        tvHotel.setVisibility(View.GONE);
    }

    private void hideHotelSpinAndClearHotel(MyAdapter.ItemView iv, InventoryTagMap inventoryTagMap) {
        assert inventoryTagMap != null;
        //改一个全部改
        iv.spHotel.setVisibility(View.GONE);
        iv.tvHotel.setVisibility(View.VISIBLE);
        iv.tvHotel.setText("");
        inventoryTagMap.hotelId = null;
        inventoryTagMap.hotel = "";
    }

    private void hideHotelSpinAndClearHotel(Spinner hotelSpin, TextView hotelText) {
        //改一个全部改
        hotelSpin.setVisibility(View.GONE);
        hotelText.setVisibility(View.VISIBLE);
        hotelText.setText("");
        lsTagList.forEach(inventoryTagMap -> {
            inventoryTagMap.hotelId = null;
            inventoryTagMap.hotel = "";
        });

    }

    public class MsgCallback implements TagCallback {

        @Override
        public void tagCallback(ReadTag arg0) {
            // TODO Auto-generated method stub
            String epc = arg0.epcId.toUpperCase();
            String memid = arg0.memId.toUpperCase();
            String DevName = arg0.DevName;
            InventoryTagMap m;
            Integer findIndex = dtIndexMap.get(epc + "," + memid);
            Rfid rfid = epcMap.get(epc);
            //已存在 不管
            if (findIndex != null) {
                return;
            }
            dtIndexMap.put(epc + "," + memid, dtIndexMap.size());
            m = new InventoryTagMap();
            tagMap.put(epc, m);
            m.strEPC = epc;
            m.strMem = arg0.memId;
            if (rfid != null) {
                m.isRegistered = true;
                m.classification = rfid.getClassification();
                m.classificationName = rfid.getClassificationName();
                m.type = rfid.getType();
                if (m.type == null) {
                    m.type = 1;
                } else if (m.type == 0) {
                    m.hotelId = rfid.getHotelId();
                    m.hotel = hotelNameMap.get(rfid.getHotelId());
                }
                m.typeName = rfid.getTypeName();
                m.remark = rfid.getRemark();
            }

            m.nReadCount = 1;
            lsTagList.add(m);
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

    ;

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
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
        unregisterReceiver(usbReceiver);
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
                iv.tvRemark = (EditText) view.findViewById(R.id.remark_text);
                iv.tvHotel = (TextView) view.findViewById(R.id.hotel_text);
                iv.tvType = (TextView) view.findViewById(R.id.type_text);
                iv.tvClassification = (TextView) view.findViewById(R.id.classification_text);
                iv.spClassification = (Spinner) view.findViewById(R.id.spinner_classification);
                iv.spHotel = (Spinner) view.findViewById(R.id.spinner_hotel);
                iv.spType = (Spinner) view.findViewById(R.id.spinner_type);
                view.setTag(iv);
            } else {
                iv = (ItemView) view.getTag();
            }

            String epc = inventoryTagMap.strEPC;
            String memid = inventoryTagMap.strMem;
            ItemView finalIv1 = iv;
            iv.tvRemark.setOnClickListener(v -> {
                finalIv1.tvRemark.requestFocus();
//                    showKeyboard(v);
            });
//             **编辑完成后保存**

            iv.tvRemark.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
//                        hideKeyboard(v);
//                        finalIv1.tvRemark.setFocusableInTouchMode(false);
                    inventoryTagMap.remark = finalIv1.tvRemark.getText().toString();
                }
            });

            iv.tvId.setText(String.valueOf(position + 1));
            iv.tvRemark.setText(inventoryTagMap.remark);
            if (memid == null || memid.length() == 0)
                iv.tvEpc.setText(epc);
            else
                iv.tvEpc.setText(epc + "," + memid);
//                iv.tvTime.setText(String.valueOf(mList.get(position).nReadCount));
            if (inventoryTagMap.isRegistered) {
                //如果已登记过的，就把 酒店，类型，型号，酒店直接展示出来就好了
                iv.tvType.setText(inventoryTagMap.typeName);
                iv.tvClassification.setText(inventoryTagMap.classificationName);
                iv.tvHotel.setText(inventoryTagMap.hotel);
                showTextViewAndHideSpin(iv);
            } else {
                //默认共享
                setTypeSpin(iv, inventoryTagMap);
                setHotelSpin(iv, inventoryTagMap);
                setClassificationSpin(iv, inventoryTagMap);
//                clearSpin(iv);
                iv.tvHotel.setText("");
                hideTextViewAndShowSpin(iv);
            }

            return view;
        }

        private void clearSpin(ItemView iv) {
            iv.spType.setSelection(0);
            iv.spClassification.setSelection(0);
            iv.spClassification.setSelection(0);
        }

        private void showTextViewAndHideSpin(ItemView iv) {
            iv.spType.setVisibility(View.GONE);
            iv.tvType.setVisibility(View.VISIBLE);
            iv.spClassification.setVisibility(View.GONE);
            iv.tvClassification.setVisibility(View.VISIBLE);
        }


        private void setTypeSpin(ItemView iv, InventoryTagMap inventoryTagMap) {
            assert inventoryTagMap != null;
            // 设置下拉列表
            ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, typeOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            iv.spType.setAdapter(adapter);
            // 防止重复触发监听器，先移除旧的监听
            iv.spType.setOnItemSelectedListener(null);

            // 设置 Spinner 默认选项（确保 UI 正确回显数据）
            if (inventoryTagMap.type != null) {
                iv.spType.setSelection(inventoryTagMap.type + 1);
            } else {
                iv.spType.setSelection(0);
            }

            // 监听用户选择
            iv.spType.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // 保存用户选择的值
                    if (position == 0) {
                        return;
                    }
                    inventoryTagMap.type = typeMap.get(typeOptions[position]);
                    //改一个全部未注册过的都改
//                    mList.stream().filter(tag -> tag.isRegistered).forEach(tag -> tag.type = typeMap.get(typeOptions[position]));

//                    assert inventoryTagMap.type != null;
                    //tradition
                    if (inventoryTagMap.type == null || 1 == inventoryTagMap.type) {
                        hideHotelSpinAndClearHotel(iv, inventoryTagMap);
                    } else if (0 == inventoryTagMap.type) {
                        showHotelSpin(iv.spHotel, iv.tvHotel);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        private void setClassificationSpin(ItemView iv, InventoryTagMap inventoryTagMap) {
            // 设置下拉列表
            ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, classificationOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            iv.spClassification.setAdapter(adapter);
            // 防止重复触发监听器，先移除旧的监听
            iv.spClassification.setOnItemSelectedListener(null);
            if (inventoryTagMap.classification != null) {
                iv.spClassification.setSelection(inventoryTagMap.classification + 1);
            }
            // 监听用户选择
            iv.spClassification.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0){return;}
                    // 保存用户选择的值
                    inventoryTagMap.classification = classificationMap.get(classificationOptions[position]);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }


        private void setHotelSpin(ItemView iv, InventoryTagMap inventoryTagMap) {
            // 设置下拉列表
            ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, hotelOptions);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            iv.spHotel.setAdapter(adapter);
            // 防止重复触发监听器，先移除旧的监听
            iv.spHotel.setOnItemSelectedListener(null);
            if (inventoryTagMap.hotel != null && hotelIndexMap.get(inventoryTagMap.hotel) != null) {
                iv.spHotel.setSelection(hotelIndexMap.get(inventoryTagMap.hotel));
            }
            // 监听用户选择
            iv.spHotel.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0){
                        return;
                    }
                    inventoryTagMap.hotelId = hotelMap.get(hotelOptions[position]); // 保存用户选择的值
                    inventoryTagMap.hotel = hotelNameMap.get(inventoryTagMap.hotelId);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }


        public class ItemView {
            TextView tvId;
            TextView tvEpc;
            TextView tvTime;
            EditText tvRemark;
            Spinner spHotel;

            TextView tvHotel;
            Spinner spType;

            TextView tvType;
            Spinner spClassification;

            TextView tvClassification;

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

    // 生成二维码
    private void generateQRCode(String barCode) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(barCode, BarcodeFormat.QR_CODE, 200, 200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            qrCodeBitmap = barcodeEncoder.createBitmap(bitMatrix);
        } catch (WriterException e) {
            Toast.makeText(this, "生成二维码失败", Toast.LENGTH_SHORT).show();
            qrCodeBitmap = null;
        }
    }

    // 打印二维码
    private boolean printQRCode(String barCode, String tag) {
        // 检查usb是否在线
        if (usbDevice == null) {
            playAlarmSound(this);
            Toast.makeText(this, "打印机未连接，无法打印", Toast.LENGTH_SHORT).show();
            return false;
        }
        try {
            // 发送打印
            boolean print = printer.print(barCode, tag);
            if (print) {
                Toast.makeText(this, "打印成功", Toast.LENGTH_SHORT).show();
            } else {
                playAlarmSound(this);
                Toast.makeText(this, "打印失败: ", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            playAlarmSound(this);
            Toast.makeText(this, "打印失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        }
        return true;

    }

    private Bitmap generateQRCode(String text, int width, int height) throws Exception {
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return bitmap;
    }

    // 将 Bitmap 转换为打印机字节数组（简化为黑白图像）
    private byte[] bitmapToPrinterBytes(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte[] data = new byte[8 + height * ((width + 7) / 8)];

        // ESC/POS 打印位图命令
        data[0] = 0x1D; // GS
        data[1] = 0x76; // v
        data[2] = 0x30; // 0
        data[3] = 0x00; // 模式
        data[4] = (byte) ((width + 7) / 8); // 宽度字节数
        data[5] = 0x00;
        data[6] = (byte) (height % 256);    // 高度低位
        data[7] = (byte) (height / 256);    // 高度高位

        int offset = 8;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x += 8) {
                byte b = 0;
                for (int k = 0; k < 8 && (x + k) < width; k++) {
                    int pixel = bitmap.getPixel(x + k, y);
                    if (pixel == 0xFF000000) { // 黑色
                        b |= (byte) (1 << (7 - k));
                    }
                }
                data[offset++] = b;
            }
        }
        return data;
    }

    private byte[] concatenateByteArrays(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }
        byte[] result = new byte[totalLength];
        int currentPos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, currentPos, array.length);
            currentPos += array.length;
        }
        return result;
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // 权限已授予，可以连接打印机
                        }
                    } else {
                        // 权限被拒绝
                    }
                }
            }
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                v.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void showKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public void playAlarmSound(Context context) {
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Ringtone ringtone = RingtoneManager.getRingtone(context, alarmUri);
        ringtone.play();

        // 2 秒后停止
        new Handler().postDelayed(ringtone::stop, 2000);
    }


    public void showErrorFeedback(Context context, View view) {
        playAlarmSound(context);  // 声音
    }

}
