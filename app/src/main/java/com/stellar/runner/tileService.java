package com.stellar.runner;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

@TargetApi(Build.VERSION_CODES.N)
public class tileService extends TileService {
    SharedPreferences sp;

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        if (tile == null) return;
        if (sp == null) sp = getSharedPreferences("tile", 0);
        boolean existc = sp.getString("content", null) == null || sp.getString("content", null).length() == 0;
        if (existc) {
            tile.setLabel("长按设置命令");
            tile.setState(Tile.STATE_INACTIVE);
        } else {
            tile.setLabel("点击执行命令");
        }
        tile.updateTile();
        super.onStartListening();
    }

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        if (tile == null) return;
        if (sp == null) sp = getSharedPreferences("tile", 0);
        String cmd = sp.getString("content", null);
        String cmd1 = sp.getString("content1", null);
        boolean switchTile = sp.getBoolean("switch", false);
        boolean existc = cmd == null || cmd.length() == 0;
        if (existc) {
            startActivityAndCollapse(new Intent(this, tileSetting.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            if (!switchTile) {
                startActivityAndCollapse(new Intent(this, ExecNoDisplay.class).putExtra("content", cmd).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                if (tile.getState() == Tile.STATE_INACTIVE) {
                    startActivityAndCollapse(new Intent(this, ExecNoDisplay.class).putExtra("content", cmd).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    tile.setState(Tile.STATE_ACTIVE);
                } else {
                    startActivityAndCollapse(new Intent(this, ExecNoDisplay.class).putExtra("content", cmd1).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    tile.setState(Tile.STATE_INACTIVE);
                }
                tile.updateTile();
            }
        }
        super.onClick();
    }

}
