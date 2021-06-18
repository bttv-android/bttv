package bttv.highlight;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import bttv.Res;
import bttv.Util;

public class HighlightAdapter extends ArrayAdapter<String> {
    public static final String TAG = "LBTTVHighlAdapter";
    private final Context context;
    private final int res;
    private final String[] words;

    public HighlightAdapter(@NonNull Context context, int resource, @NonNull String[] objects) {
        super(context, resource, objects);
        this.context = context;
        this.res = resource;
        this.words = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable final View convertView, @NonNull ViewGroup parent) {
        View view0 = convertView;
        if(view0 == null) {
            view0 = LayoutInflater.from(getContext()).inflate(res, parent, false);
        }
        final View view = view0;
        TextView tv = view.findViewById(Util.getResourceId(context, Res.ids.bttv_highlight_dia_list_text));
        tv.setText(getItem(position));

        Button btn = view.findViewById(Util.getResourceId(context, Res.ids.bttv_highlight_dia_list_btn));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Highlight.remove(words[position]);
                // view.setVisibility(View.GONE);
                Log.i(TAG, "onClick: " + words[position]);
            }
        });
        return view;
    }


}
