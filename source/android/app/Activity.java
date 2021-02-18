package android.app;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;

public class Activity extends Context {
    public void startActivity(Intent intent) {

    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return null;
    }

    public Intent getIntent() {
        return null;
    }

    public <T extends View> T findViewById(int id) {
        return null;
    }

    @Override
    public File getCacheDir() {
        // TODO Auto-generated method stub
        return null;
    }

}
