package com.stellar.runner;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.LayoutAnimationController;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import roro.stellar.Stellar;
import roro.stellar.StellarHelper;

public class MainActivity extends Activity {


    class adapter extends BaseAdapter {
        private final int[] data;
        private final boolean isRoot;

        public adapter(int[] data, boolean isRoot) {

            //设置adapter需要接收两个参数：上下文、int数组
            super();
            this.data = data;
            this.isRoot = isRoot;
        }


        //固定的写法
        public int getCount() {
            return data.length;
        }

        //固定的写法
        @Override
        public Object getItem(int position) {
            return null;
        }

        //固定的写法
        @Override
        public long getItemId(int position) {
            return position;
        }


        //此函数定义每一个item的显示
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.r, null);
                holder = new ViewHolder();
                holder.texta = convertView.findViewById(R.id.a);
                holder.textb = convertView.findViewById(R.id.b);
                holder.imageButton = convertView.findViewById(R.id.c);
                holder.layout = convertView.findViewById(R.id.l);
                convertView.setTag(holder);
            } else {

                //对于已经加载过的item就直接使用，不需要再次加载了，这就是ViewHolder的作用
                holder = (ViewHolder) convertView.getTag();
            }

            //获得用户对于这个格子的设置
            SharedPreferences sharedPreferences = getSharedPreferences(String.valueOf(data[position]), 0);
            init(holder, sharedPreferences);
            return convertView;
        }

        class ViewHolder {
            TextView texta;
            TextView textb;
            ImageButton imageButton;
            LinearLayout layout;
        }

        void init(ViewHolder holder, SharedPreferences sharedPreferences) {


            //用户是否设置了命令内容
            boolean existc = sharedPreferences.getString("content", "").length() == 0;

            //用户是否设置了命令名称
            boolean existn = sharedPreferences.getString("name", "").length() == 0;

            //这个点击事件是点击编辑命令
            View.OnClickListener voc = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    View v = View.inflate(MainActivity.this, R.layout.dialog, null);
                    final CheckBox cb = v.findViewById(R.id.cb);
                    cb.setChecked(sharedPreferences.getBoolean("shell", false));
                    cb.setVisibility(isRoot ? View.VISIBLE : View.GONE);
                    final EditText editText = v.findViewById(R.id.e);
                    final ImageView undo = v.findViewById(R.id.undo);
                    final ImageView redo = v.findViewById(R.id.redo);
                    editText.setText(sharedPreferences.getString("content", null));

                    editText.setOnKeyListener(new View.OnKeyListener() {
                        @Override
                        public boolean onKey(View view, int i, KeyEvent keyEvent) {

                            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                                sharedPreferences.edit().putString("content", editText.getText().toString()).putBoolean("shell", cb.isChecked()).apply();
                                init(holder, sharedPreferences);
                            }


                            return false;
                        }
                    });
                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                        }

                        @Override
                        public void afterTextChanged(Editable editable) {
                            undo.setVisibility(View.VISIBLE);
                        }
                    });
                    undo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            editText.onTextContextMenuItem(android.R.id.undo);
                            redo.setVisibility(View.VISIBLE);
                        }
                    });

                    redo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            editText.onTextContextMenuItem(android.R.id.redo);
                        }
                    });
                    final EditText editText1 = v.findViewById(R.id.a);
                    editText1.setText(sharedPreferences.getString("name", null));
                    //回车键保存，方便手表用户和连接物理键盘的用户使用
                    editText1.setOnKeyListener(new View.OnKeyListener() {
                        @Override
                        public boolean onKey(View view, int i, KeyEvent keyEvent) {
                            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                                sharedPreferences.edit().putString("name", editText1.getText().toString()).putBoolean("shell", cb.isChecked()).apply();
                                init(holder, sharedPreferences);
                            }
                            return false;
                        }
                    });
                    editText.requestFocus();
                    editText.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(editText, 0);
                        }
                    }, 200);
                    new AlertDialog.Builder(MainActivity.this).setTitle("编辑命令").setView(v).setPositiveButton("完成", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sharedPreferences.edit().putString("content", editText.getText().toString()).putString("name", editText1.getText().toString()).putBoolean("shell", cb.isChecked()).apply();
                            init(holder, sharedPreferences);
                        }
                    }).show();
                }
            };

            //如果用户还没设置命令内容，则显示加号，否则显示运行符号
            holder.imageButton.setImageResource(existc ? R.drawable.plus : R.drawable.run);

            //如果用户还没设置命令内容，则点击时将编辑命令，否则点击将运行命令
            holder.imageButton.setOnClickListener(!existc ? new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (b && c)
                        //这里会根据用户是否勾选了降权，来执行不同的命令
                        startActivity(new Intent(MainActivity.this, Exec.class).putExtra("content", sharedPreferences.getBoolean("shell", false) ? "whoami|grep root &> /dev/null && echo '提示:已将root降权至shell' 1>&2;" + getApplicationInfo().nativeLibraryDir + "/libchid.so 2000 " + sharedPreferences.getString("content", " ") + " || " + sharedPreferences.getString("content", " ") : sharedPreferences.getString("content", " ")));
                    else
                        check();
                }
            } : voc);
            holder.texta.setText(existn ? "空" : sharedPreferences.getString("name", "空"));
            holder.texta.setTextColor(existc ? getColor(R.color.b) : getColor(R.color.a));
            holder.textb.setText(existc ? "空" : sharedPreferences.getString("content", "空"));
            holder.layout.setOnClickListener(voc);
            holder.layout.setOnLongClickListener(existc ? null : new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", sharedPreferences.getString("content", "ls -l")));
                    Toast.makeText(MainActivity.this, "已复制该条命令至剪贴板:\n" + sharedPreferences.getString("content", "ls -l"), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });

        }

    }

    Button B, C;
    int m;
    float density;
    ListView d, e;
    ImageView iv;
    SharedPreferences sp;
    boolean isRoot = false, isNight, b = true, c = false;
    //Stellar监听授权结果
    private final Stellar.OnRequestPermissionResultListener RL = new Stellar.OnRequestPermissionResultListener() {
        @Override
        public void onRequestPermissionResult(int requestCode, boolean allowed, boolean onetime) {
            check();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //根据系统深色模式自动切换软件的深色/亮色主题

        isNight = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
        if (!isNight)
            setTheme(android.R.style.Theme_DeviceDefault_Light_Dialog);
        sp = getSharedPreferences("data", 0);
        //如果是初次开启，则展示help界面
        if (sp.getBoolean("first", true)) {
            showPrivacy();

        }
        //读取用户设置“是否隐藏后台”，并进行隐藏后台
        ((ActivityManager) getSystemService(Service.ACTIVITY_SERVICE)).getAppTasks().get(0).setExcludeFromRecents(sp.getBoolean("hide", true));
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        //限定一下横屏时的窗口宽度,让其不铺满屏幕。否则太丑
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            getWindow().getAttributes().width = (getWindowManager().getDefaultDisplay().getHeight());


        B = findViewById(R.id.b);
        C = findViewById(R.id.c);
        density = getResources().getDisplayMetrics().density;
        ShapeDrawable oval = new ShapeDrawable(new RoundRectShape(new float[]{40 * density, 40 * density, 40 * density, 40 * density, 40 * density, 40 * density, 40 * density, 40 * density}, null, null));
        oval.getPaint().setColor(getResources().getColor(R.color.a));
        C.setBackground(oval);
        C.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showHelp();
            }
        });

        iv = findViewById(R.id.iv);
        iv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    BigAnimation(view);
                    return true;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    PopFragment popFragment = new PopFragment();
                    popFragment.show(getFragmentManager(), "frag");
                }
                return true;
            }
        });
        //Stellar返回授权结果时将执行RL函数
        Stellar.INSTANCE.addRequestPermissionResultListener(RL);

        //m用于保存Stellar状态显示按钮的初始颜色（int类型哦），为的是适配安卓12的莫奈取色，方便以后恢复颜色时用
        m = B.getCurrentTextColor();

        //检查Shizuk是否运行，并申请Stellar权限
        check();
        d = findViewById(R.id.list);
        e = findViewById(R.id.lista);

        //为两列listView适配每个item的具体样式和总item数
        initlist();
    }

    private void showPrivacy() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("隐私政策")
                .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sp.edit().putBoolean("first", false).apply();
                    }
                })
                .setCancelable(false)
                .setMessage("本应用不会收集您的任何信息，且完全不包含任何联网功能。继续使用则代表您同意上述隐私政策。\n")
                .setNegativeButton("退出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .create().show();
    }

    private void showHelp() {
        //展示帮助界面
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("使用帮助")
                .setMessage(Html.fromHtml("<br>&nbsp;&nbsp;--使用本APP需要您的设备已安装并激活Stellar。您可以去酷安或其他网站搜索Shiuzku激活教程。激活成功后，您就可以使用本APP来以Stellar的身份执行命令。<br><br>&nbsp;&nbsp;--点击编辑某个栏目的命令内容和命令标题(标题可以不填)；长按复制该栏目中保存的命令。<br><br>&nbsp;&nbsp;--点击猫猫图案，可以一次性地运行命令。<br><br>&nbsp;&nbsp;--您可以点击本界面下方的设置按钮探索更多功能哦！"))
                .setPositiveButton("OK", null)

                .setNeutralButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                        dialog.getWindow().getAttributes().alpha = 0.85f;
                        dialog.getWindow().setGravity(Gravity.BOTTOM);

                        View v = View.inflate(MainActivity.this, R.layout.set, null);
                        Switch S = v.findViewById(R.id.s);

                        S.setChecked(sp.getBoolean("hide", true));
                        S.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                sp.edit().putBoolean("hide", b).apply();
                                ((ActivityManager) getSystemService(Service.ACTIVITY_SERVICE)).getAppTasks().get(0).setExcludeFromRecents(b);
                            }
                        });
                        final EditText editText = v.findViewById(R.id.editNum);

                        editText.setText(String.format(Locale.getDefault(), "%d", sp.getInt("number", 5)));
                        editText.setOnKeyListener(new View.OnKeyListener() {
                            @Override
                            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                                    if (editText.getText().length() > 0) {
                                        sp.edit().putInt("number", Integer.parseInt(editText.getText().toString())).apply();
                                        initlist();
                                    }
                                }
                                return false;
                            }
                        });
                        final Button btnOut = v.findViewById(R.id.btnOut);
                        btnOut.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ScrollView scrollView = new ScrollView(MainActivity.this);
                                TextView textView = new TextView(MainActivity.this);
                                for (int j = 0; j < 2 * sp.getInt("number", 5); j++) {
                                    SharedPreferences sharedPreferences = getSharedPreferences(String.valueOf(j), 0);
                                    //用户是否设置了命令内容
                                    boolean existc = sharedPreferences.getString("content", "").length() == 0;

                                    //用户是否设置了命令名称
                                    boolean existn = sharedPreferences.getString("name", "").length() == 0;

                                    if (!existc)
                                        textView.append(j + "." + (!existn ? sharedPreferences.getString("name", "空") : "空") + "\n" + sharedPreferences.getString("content", "") + "\n\n");
                                }
                                textView.setTextAppearance(R.style.t_1);
                                textView.setPadding(100, 0, 100, 0);
                                textView.setTextSize(18f);
                                textView.setTextIsSelectable(true);
                                textView.setSelectAllOnFocus(true);
                                scrollView.addView(textView);
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("导出备份")
                                        .setView(scrollView)
                                        .setNegativeButton("复制", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", textView.getText().toString()));
                                                Toast.makeText(MainActivity.this, "已复制上述内容至剪切板", Toast.LENGTH_SHORT).show();

                                            }
                                        })
                                        .show();

                            }
                        });
                        final Button btnIn = v.findViewById(R.id.btnIn);
                        btnIn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                final EditText editText1 = new EditText(MainActivity.this);

                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("导入备份")
                                        .setView(editText1)
                                        .setNegativeButton("导入", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                String str = editText1.getText().toString();
                                                if (str.length() < 1) {
                                                    Toast.makeText(MainActivity.this, "导入内容为空", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    int position = 0;
                                                    String[] arrOfStr = str.split("\n");
                                                    String name = "";
                                                    for (String string : arrOfStr) {
                                                        if (string.length() < 1) continue;
                                                        if (string.matches("\\d+\\..*")) {
                                                            name = string.substring(string.indexOf(".") + 1);
                                                        } else {
                                                            while (true) {
                                                                SharedPreferences sharedPreferences = getSharedPreferences(String.valueOf(position), 0);
                                                                if (sharedPreferences.getString("content", "").length() == 0) {
                                                                    sharedPreferences.edit().putString("name", name).putString("content", string).apply();
                                                                    break;
                                                                }
                                                                position++;
                                                            }
                                                        }
                                                    }
                                                    Toast.makeText(MainActivity.this, "导入成功！", Toast.LENGTH_SHORT).show();
                                                    initlist();
                                                }
                                            }
                                        })
                                        .show();
                            }
                        });
                        dialog.setView(v);
                        dialog.show();
                    }
                })
                .create().show();

    }

    private void check() {
        isRoot = false;
        //本函数用于检查Stellar状态，b代表stellar是否运行，c代表Stellar是否授权
        b = true;
        c = false;
        
        // 检查 Stellar 管理器是否安装
        if (!StellarHelper.INSTANCE.isManagerInstalled(this)) {
            b = false;
            Toast.makeText(this, "Stellar未安装", Toast.LENGTH_SHORT).show();
            B.setText("Stellar\n未安装");
            B.setTextColor(0xaaff0000);
            C.setText("查看帮助");
            return;
        }
        
        // 检查 Stellar 服务是否运行
        if (!Stellar.INSTANCE.pingBinder()) {
            b = false;
            Toast.makeText(this, "Stellar未运行", Toast.LENGTH_SHORT).show();
            B.setText("Stellar\n未运行");
            B.setTextColor(0xaaff0000);
            C.setText("查看帮助");
            return;
        }
        
        // 检查权限
        try {
            if (!Stellar.INSTANCE.checkSelfPermission("stellar")) {
                Stellar.INSTANCE.requestPermission("stellar", 0);
            } else {
                c = true;
            }
        } catch (Exception e) {
            Toast.makeText(this, "权限检查失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        
        B.setText(String.format("Stellar\n%s", b ? c ? "已激活" : "未授权" : "未运行"));
        B.setTextColor(b & c ? m : 0xaaff0000);
        if (b & c) {
            StellarHelper.ServiceInfo info = StellarHelper.INSTANCE.getServiceInfo();
            if (info != null) {
                isRoot = info.isRoot();
                C.setText(String.format("%s模式", isRoot ? "root" : "adb"));
            }
        } else {
            C.setText("查看帮助");
        }
    }

    @Override
    protected void onDestroy() {
        //在APP退出时，取消注册Stellar授权结果监听，这是Stellar的要求
        Stellar.INSTANCE.removeRequestPermissionResultListener(RL);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //在点击返回键时直接退出APP，因为APP比较轻量，没必要双击返回退出或者设置什么退出限制
        finish();
    }

    public void ch(View view) {
        //本函数绑定了主界面两个显示Shizuk状态的按钮的点击事件
        check();
    }

    public static class PopFragment extends DialogFragment {


        @Override
        public void onActivityCreated(Bundle savedInstanceState) {

            Window window = getDialog().getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            super.onActivityCreated(savedInstanceState);
        }

        @Override
        public void onStart() {
            Window window = getDialog().getWindow();
            window.setLayout(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? getResources().getDisplayMetrics().heightPixels : getResources().getDisplayMetrics().widthPixels - 150, -2);
            super.onStart();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            return dialog;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            boolean isNight = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) == Configuration.UI_MODE_NIGHT_YES;
            float density = getResources().getDisplayMetrics().density;
            View view = inflater.inflate(R.layout.pop, container);
            ShapeDrawable oval = new ShapeDrawable(new RoundRectShape(new float[]{20 * density, 20 * density, 20 * density, 20 * density, 20 * density, 20 * density, 20 * density, 20 * density}, null, null));
            oval.getPaint().setColor(isNight ? Color.BLACK : Color.WHITE);
            view.setBackground(oval);
            final EditText e = view.findViewById(R.id.e);
            e.requestFocus();
            e.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(e, 0);
                    } catch (Exception ignored) {
                    }
                }
            }, 200);
            e.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {

                        if (e.getText().length() > 0)
                            startActivity(new Intent(getActivity(), Exec.class).putExtra("content", e.getText().toString()));
                    }
                    return false;
                }
            });
            ImageButton imageButton = view.findViewById(R.id.ib);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (e.getText().length() > 0)
                        startActivity(new Intent(getActivity(), Exec.class).putExtra("content", e.getText().toString()));
                }
            });
            return view;
        }
    }


    private void BigAnimation(View view) {
        //BigAnimation是一个轻量级的动画，很有趣哦
        ScaleAnimation a2 = new ScaleAnimation(0.65f, 1f, 0.65f, 1f, view.getWidth() / 2f, view.getHeight() / 2f);
        a2.setDuration(100);
        view.startAnimation(a2);
    }

    public void initlist() {
        int length = sp.getInt("number", 5);
        int[] e1 = new int[length];
        int[] d1 = new int[length];


        //根据用户设置，选择展示10个格子或者更多格子
        for (int i = 0; i < length; i++) {
            e1[i] = 10 * (i / 5) + i % 5 + 5;
            d1[i] = 10 * (i / 5) + i % 5;
        }
        e.setAdapter(new adapter(e1, isRoot));
        d.setAdapter(new adapter(d1, isRoot));

        //加一点动画，非常的丝滑~~
        TranslateAnimation animation = new TranslateAnimation(-50f, 0f, -30f, 0f);
        animation.setDuration(500);
        LayoutAnimationController controller = new LayoutAnimationController(animation, 0.1f);
        controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
        d.setLayoutAnimation(controller);
        animation = new TranslateAnimation(50f, 0f, -30f, 0f);
        animation.setDuration(500);
        controller = new LayoutAnimationController(animation, 0.1f);
        e.setLayoutAnimation(controller);
    }
}
