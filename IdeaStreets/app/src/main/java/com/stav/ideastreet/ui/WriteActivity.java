package com.stav.ideastreet.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.stav.ideastreet.R;
import com.stav.ideastreet.base.BaseApplication;
import com.stav.ideastreet.base.ParentWithNaviActivity;
import com.stav.ideastreet.bean.Avatar;
import com.stav.ideastreet.bean.MyUser;
import com.stav.ideastreet.bean.Post;
import com.stav.ideastreet.bean.Song;
import com.stav.ideastreet.util.DisplayUtils;
import com.stav.ideastreet.util.EmotionGvAdapter;
import com.stav.ideastreet.util.EmotionPagerAdapter;
import com.stav.ideastreet.util.EmotionUtils;
import com.stav.ideastreet.util.StringUtils;
import com.stav.ideastreet.util.Tools;
import com.stav.ideastreet.utils.CacheUtils;
import com.stav.ideastreet.utils.ConstantValue;
import com.stav.ideastreet.utils.PrefUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cn.addapp.pickers.listeners.OnItemPickListener;
import cn.addapp.pickers.listeners.OnSingleWheelListener;
import cn.addapp.pickers.picker.SinglePicker;
import cn.bmob.v3.BmobBatch;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BatchResult;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.ProgressCallback;
import cn.bmob.v3.listener.QueryListListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadBatchListener;
import cn.bmob.v3.listener.UploadFileListener;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

import static com.stav.ideastreet.base.BaseApplication.showToast;

/**
 * 发布微博
 * @author :stav
 * @date :2017-10-25
 */
public class WriteActivity extends ParentWithNaviActivity implements AdapterView.OnItemClickListener,View.OnClickListener {

    @ViewInject(R.id.write)
    private EditText write;
    @ViewInject(R.id.open_pic)
    private ImageView open_pic;
    @ViewInject(R.id.take_pic)
    private ImageView take_pic;
    @ViewInject(R.id.open_layout)
    private LinearLayout open_layout;
    @ViewInject(R.id.take_layout)
    private LinearLayout take_layout;
    @ViewInject(R.id.total_text_num)
    private TextView total_text_num = null;
    @ViewInject(R.id.ll_emotion_dashboard)
    private LinearLayout ll_emotion_dashboard;
    @ViewInject(R.id.vp_emotion_dashboard)
    private ViewPager vp_emotion_dashboard;
    @ViewInject(R.id.bt_select)
    private Button bt_select;
    @ViewInject(R.id.ib_add_emotion)
    private ImageButton ib_add_emotion;
    // 发送图片的路径
    private String image_path;
    private Bitmap bmp;
    private ArrayList<HashMap<String, Object>> imageItem;
    private SimpleAdapter simpleAdapter;     //适配器
    private Tools tools;
    private String dateTime;
    private EmotionPagerAdapter emotionPagerGvAdapter;
    private static final int REQUEST_CODE_ALBUM = 1;
    private static final int REQUEST_CODE_CAMERA = 2;
    String targeturl = null;
    private String imgUrl;

    /**
     * 设置actionBar
     * @return
     */
    @Override
    protected String title() {
        return "发布创意";
    }
    @Override
    public Object right() {
        return R.drawable.base_action_bar_publish_bg_selector;
    }
    @Override
    public ParentWithNaviActivity.ToolBarListener setToolBarListener() {
        return new ParentWithNaviActivity.ToolBarListener() {
            //退出该页面
            @Override
            public void clickLeft() {
                finish();
//                showToast(imgUrl+"");
            }

            //发表微博
            @Override
            public void clickRight() {
                publish();
            }
        };
    }

    private void uploadImg(final Post weibo){
        final BmobFile bmobFile = new BmobFile(new File(targeturl));
        bmobFile.uploadObservable(new ProgressCallback() {
            @Override
            public void onProgress(Integer integer, long l) {

            }
        }).doOnNext(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                final String url = bmobFile.getUrl();
                log("上传成功："+url+","+bmobFile.getFilename());
                weibo.setUpdownImg(url);
                weibo.update(new UpdateListener() {
                    @Override
                    public void done(BmobException e) {
                        if (e==null){
                            Log.d("stav1", "上传成功");
                        }
                    }
                });
            }
        }).concatMap(new Func1<Void, Observable<String>>() {//将bmobFile保存到movie表中
            @Override
            public Observable<String> call(Void aVoid) {
                return saveObservable(new Avatar(BmobUser.getCurrentUser().getUsername(),bmobFile));
            }
        }).concatMap(new Func1<String, Observable<String>>() {//下载文件
            @Override
            public Observable<String> call(String s) {
                return bmobFile.downloadObservable(new ProgressCallback() {
                    @Override
                    public void onProgress(Integer value, long total) {
                        log("download-->onProgress:"+value+","+total);
                    }
                });
            }
        }).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                log("--onCompleted--");
            }

            @Override
            public void onError(Throwable e) {
                log("--onError--:"+e.getMessage());
            }

            @Override
            public void onNext(String s) {
                log("download的文件地址："+s);
            }
        });
    }

    /**
     * save的Observable
     * @param obj
     * @return
     */
    private Observable<String> saveObservable(BmobObject obj){
        return obj.saveObservable();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.write);
        initNaviView();
        ViewUtils.inject(this);
        initData();
        //注册输入框内容监听器
        write.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            /**
             * 当输入框的内容变化的时候执行
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean flag = false;
                String mText = write.getText().toString();
                int len = mText.length();
                if (len > 140) {
                    total_text_num.setTextColor(Color.RED);
                } else {
                    total_text_num.setTextColor(Color.GREEN);
                }
                total_text_num.setText(String.valueOf(140 - len));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        //初始化表情包加载
        initEmotion();
    }

    //发布创意
    private void publish() {
        String mText = write.getText().toString();
        int len = mText.length();
        if (len == 0) {
            Toast.makeText(WriteActivity.this, "内容不能为空！", Toast.LENGTH_SHORT).show();
        } else if (len > 140) {
            Toast.makeText(WriteActivity.this, "超出字数限制！", Toast.LENGTH_SHORT).show();
        } else {
            publishWeibo(mText);
            //进入微博主界面
            Intent intent = new Intent(WriteActivity.this, MainActivity.class);
            startActivity(intent);
            WriteActivity.this.finish();
        }
    }


    //初始化数据
    private void initData() {
        // 隐藏或显示表情面板
        ib_add_emotion.setOnClickListener(this);
        open_layout.setOnClickListener(this);
        take_layout.setOnClickListener(this);
        open_pic.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {//        initGridView();
        // TODO Auto-generated method stub
        switch (v.getId()) {

            case R.id.ib_add_emotion:
                ll_emotion_dashboard.setVisibility(
                        ll_emotion_dashboard.getVisibility() == View.VISIBLE ?
                                View.GONE : View.VISIBLE);
                break;
            case R.id.open_layout:
                Date date1 = new Date(System.currentTimeMillis());
                dateTime = date1.getTime() + "";
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setDataAndType(MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, REQUEST_CODE_ALBUM);
                break;
            case R.id.take_layout:
                Date date = new Date(System.currentTimeMillis());
                dateTime = date.getTime() + "";
                File f = new File(CacheUtils.getCacheDirectory(this, true, "pic") + dateTime);
                if (f.exists()) {
                    f.delete();
                }
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Uri uri = Uri.fromFile(f);
                Log.e("uri", uri + "");

                Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                camera.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(camera, REQUEST_CODE_CAMERA);
                break;
            case R.id.open_pic:
                break;
            default:
                break;
        }
    }

    //选择发布创意的分类
    public void onOptionPicker(View view) {

        String[] asset = getApplicationContext().getResources().getStringArray(R.array.news_viewpage_arrays);
        List<String> list = Arrays.asList(asset);

        SinglePicker<String> picker = new SinglePicker<>(this, list);
        picker.setCanLoop(false);//不禁用循环
        picker.setLineVisible(true);
        picker.setShadowVisible(true);
        picker.setTextSize(18);
        picker.setSelectedIndex(1);
        picker.setWheelModeEnable(true);
        //启用权重 setWeightWidth 才起作用
        picker.setWeightEnable(true);
        picker.setWeightWidth(1);
        picker.setSelectedTextColor(0xFF279BAA);//前四位值是透明度
        picker.setUnSelectedTextColor(0xFF999999);
        picker.setOnSingleWheelListener(new OnSingleWheelListener() {
            @Override
            public void onWheeled(int index, String item) {
//                showToast("index=" + index + ", item=" + item);
                bt_select.setText(item);
            }
        });
        picker.setOnItemPickListener(new OnItemPickListener<String>() {
            @Override
            public void onItemPicked(int index, String item) {
//                showToast("index=" + index + ", item=" + item);
                bt_select.setText(item);
            }
        });
        picker.show();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("stav1", "get album:");
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_ALBUM:
                    String fileName = null;
                    if (data != null) {
                        Uri originalUri = data.getData();
                        ContentResolver cr = getContentResolver();
                        Cursor cursor = cr.query(originalUri, null, null, null, null);
                        if (cursor.moveToFirst()) {
                            do {
                                fileName = cursor.getString(cursor.getColumnIndex("_data"));
                                Log.i("stav1", "get album:" + fileName);
                            } while (cursor.moveToNext());
                        }
                        Bitmap bitmap = compressImageFromFile(fileName);
                        targeturl = saveToSdCard(bitmap);
                        open_pic.setBackgroundDrawable(new BitmapDrawable(bitmap));
                        take_layout.setVisibility(View.GONE);
                    }
                    break;
                case REQUEST_CODE_CAMERA:
                    String files = CacheUtils.getCacheDirectory(this, true, "pic") + dateTime;
                    File file = new File(files);
                    if (file.exists()) {
                        Bitmap bitmap = compressImageFromFile(files);
                        targeturl = saveToSdCard(bitmap);
                        take_pic.setBackgroundDrawable(new BitmapDrawable(bitmap));
                        open_layout.setVisibility(View.GONE);

                        Log.i("stav1", "get album:" + targeturl);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public String saveToSdCard(Bitmap bitmap) {
        String files = CacheUtils.getCacheDirectory(this, true, "pic") + dateTime + "_11.png";
        File file = new File(files);
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("stav1", file.getAbsolutePath());
        return file.getAbsolutePath();
    }
    private Bitmap compressImageFromFile(String srcPath) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;//只读边,不读内容
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);

        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        float hh = 800f;//
        float ww = 480f;//
        int be = 1;
        if (w > h && w > ww) {
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;
        newOpts.inSampleSize = be;//设置采样率

        newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;//该模式是默认的,可不设
        newOpts.inPurgeable = true;// 同时设置才会有效
        newOpts.inInputShareable = true;//。当系统内存不够时候图片自动被回收

        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
//		return compressBmpFromBmp(bitmap);//原来的方法调用了这个方法企图进行二次压缩
        //其实是无效的,大家尽管尝试
        return bitmap;
    }

    /**
     *  初始化表情面板内容
     */
    private void initEmotion() {
        // 获取屏幕宽度
        int gvWidth = DisplayUtils.getScreenWidthPixels(this);
        // 表情边距
        int spacing = DisplayUtils.dp2px(this, 8);
        // GridView中item的宽度
        int itemWidth = (gvWidth - spacing * 8) / 7;
        int gvHeight = itemWidth * 3 + spacing * 4;

        List<GridView> gvs = new ArrayList<GridView>();
        List<String> emotionNames = new ArrayList<String>();
        // 遍历所有的表情名字
        for (String emojiName : EmotionUtils.emojiMap.keySet()) {
            emotionNames.add(emojiName);
            // 每20个表情作为一组,同时添加到ViewPager对应的view集合中
            if (emotionNames.size() == 20) {
                GridView gv = createEmotionGridView(emotionNames, gvWidth, spacing, itemWidth, gvHeight);
                gvs.add(gv);
                // 添加完一组表情,重新创建一个表情名字集合
                emotionNames = new ArrayList<String>();
            }
        }

        // 检查最后是否有不足20个表情的剩余情况
        if (emotionNames.size() > 0) {
            GridView gv = createEmotionGridView(emotionNames, gvWidth, spacing, itemWidth, gvHeight);
            gvs.add(gv);
        }

        // 将多个GridView添加显示到ViewPager中
        emotionPagerGvAdapter = new EmotionPagerAdapter(gvs);
        vp_emotion_dashboard.setAdapter(emotionPagerGvAdapter);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(gvWidth, gvHeight);
        vp_emotion_dashboard.setLayoutParams(params);
    }

    /**
     * 创建显示表情的GridView
     */
    private GridView createEmotionGridView(List<String> emotionNames, int gvWidth, int padding, int itemWidth, int gvHeight) {
        // 创建GridView
        GridView gv = new GridView(this);
        gv.setBackgroundColor(Color.rgb(233, 233, 233));
        gv.setSelector(android.R.color.transparent);
        gv.setNumColumns(7);
        gv.setPadding(padding, padding, padding, padding);
        gv.setHorizontalSpacing(padding);
        gv.setVerticalSpacing(padding);
        LayoutParams params = new LayoutParams(gvWidth, gvHeight);
        gv.setLayoutParams(params);
        // 给GridView设置表情图片
        EmotionGvAdapter adapter = new EmotionGvAdapter(this, emotionNames, itemWidth);
        gv.setAdapter(adapter);
        gv.setOnItemClickListener(this);
        return gv;
    }

    /**
     * 点击表情添加在编辑框
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object itemAdapter = parent.getAdapter();

        if (itemAdapter instanceof EmotionGvAdapter) {
            // 点击的是表情
            EmotionGvAdapter emotionGvAdapter = (EmotionGvAdapter) itemAdapter;

            if (position == emotionGvAdapter.getCount() - 1) {
                // 如果点击了最后一个回退按钮,则调用删除键事件
                write.dispatchKeyEvent(new KeyEvent(
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            } else {
                // 如果点击了表情,则添加到输入框中
                String emotionName = emotionGvAdapter.getItem(position);

                // 获取当前光标位置,在指定位置上添加表情图片文本
                int curPosition = write.getSelectionStart();
                StringBuilder sb = new StringBuilder(write.getText().toString());
                sb.insert(curPosition, emotionName);

                // 特殊文字处理,将表情等转换一下
                write.setText(StringUtils.getEmotionContent(
                        this, write, sb.toString()));

                // 将光标设置到新增完表情的右侧
                write.setSelection(curPosition + emotionName.length());
            }

        }
    }

    /**
     * 发布微博，发表微博时关联了用户类型，是一对一的体现
     */

    private void publishWeibo(String content){

        MyUser user = BmobUser.getCurrentUser(MyUser.class);
        String mSelect = bt_select.getText().toString();
        if(user == null){
            showToast("发布微博前请先登陆");
            return;
        }else if(TextUtils.isEmpty(content)){
            showToast("发布内容不能为空");
            return;
        }
        // 创建微博信息
        final Post weibo = new Post();
        weibo.setContent(content);
        weibo.setSelector(mSelect);
        weibo.setLove(0);
        weibo.setUpdownImg(imgUrl);
        weibo.setPass(true);
        weibo.setComment(0);
        weibo.setAuthorName(user.getUsername());

        String[] str1 = new String[] {"创意饰品","创意美食","创意设计","创意陶瓷","创意礼物","创意家居","人才市场"};
        int[] intTemp = new int[str1.length];
        int num;
        for (int i = 0; i <str1.length; i++) {

            if (mSelect.equals(str1[i])){
                num = i;
                weibo.setTest(num);
            }
        }

        weibo.setAuthor(user);
        weibo.save(new SaveListener<String>() {

            @Override
            public void done(String s, BmobException e) {
                if(e==null){
                    showToast("发布成功");
                    uploadImg(weibo);
                }else{
                    Log.e("tag", "done: "+(e));
                }
            }
        });
    }

}

