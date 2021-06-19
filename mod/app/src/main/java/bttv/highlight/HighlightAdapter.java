package bttv.highlight;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import bttv.Res;
import bttv.ResUtil;

public class HighlightAdapter extends ArrayAdapter<String> {
    public static final String TAG = "LBTTVHighlAdapter";
    private final Context context;
    private final int res;
    private final List<String> words;

    public HighlightAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
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
        TextView tv = view.findViewById(ResUtil.getResourceId(context, Res.ids.bttv_highlight_dia_list_text));
        tv.setText(getItem(position));

        ImageButton btn = view.findViewById(ResUtil.getResourceId(context, Res.ids.bttv_highlight_dia_list_btn));
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String word = words.get(position);
                Highlight.remove(word);
                words.remove(position);
                notifyDataSetChanged();
                Log.i(TAG, "onClick: " + word);
            }
        });
        return view;
    }


}
