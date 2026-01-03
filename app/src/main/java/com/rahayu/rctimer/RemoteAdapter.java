package com.rahayu.rctimer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class RemoteAdapter extends RecyclerView.Adapter<RemoteAdapter.ViewHolder> {

    private List<RemoteModel> remoteList;
    private Context context;
    private ActionListener actionListener;

    public interface ActionListener {
        void onStartPause(int remoteId);
        void onReset(int remoteId);
        void onStopAlarm(int remoteId);
        void onUpdateDefaultTime(int remoteId, long newTimeMillis);
    }

    public RemoteAdapter(Context context, List<RemoteModel> remoteList, ActionListener listener) {
        this.context = context;
        this.remoteList = remoteList;
        this.actionListener = listener;
    }
    
    // Method to update list smoothly
    public void updateData(List<RemoteModel> newList) {
        this.remoteList = newList;
        notifyDataSetChanged(); // For 10 items, this is fine
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_remote, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RemoteModel model = remoteList.get(position);

        holder.tvName.setText(model.getName());

        // Format Timer
        long minutes = (model.getRemainingTimeMillis() / 1000) / 60;
        long seconds = (model.getRemainingTimeMillis() / 1000) % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        holder.tvTimer.setText(timeFormatted);

        // Status Dot
        if (model.isAlarming()) {
            holder.statusDot.setBackgroundResource(android.R.drawable.presence_busy); // Red-ish/Orange
            holder.btnStopAlarm.setVisibility(View.VISIBLE);
            holder.btnStart.setEnabled(false);
            holder.btnReset.setEnabled(false);
        } else if (model.isRunning()) {
            holder.statusDot.setBackgroundResource(android.R.drawable.presence_online); // Green
            holder.btnStart.setText("PAUSE");
            holder.btnStart.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFA000)); // Orange
            holder.btnStopAlarm.setVisibility(View.GONE);
            holder.btnStart.setEnabled(true);
            holder.btnReset.setEnabled(true);
        } else {
            holder.statusDot.setBackgroundResource(android.R.drawable.presence_invisible); // Grey
            holder.btnStart.setText("START");
            holder.btnStart.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF388E3C)); // Green
            holder.btnStopAlarm.setVisibility(View.GONE);
            holder.btnStart.setEnabled(true);
            holder.btnReset.setEnabled(true);
        }

        // Click Listeners
        holder.btnStart.setOnClickListener(v -> actionListener.onStartPause(model.getId()));
        holder.btnReset.setOnClickListener(v -> actionListener.onReset(model.getId()));
        holder.btnStopAlarm.setOnClickListener(v -> actionListener.onStopAlarm(model.getId()));
        
        // Long click to edit default time for this specific unit (Advanced feature)
        holder.itemView.setOnLongClickListener(v -> {
            showEditTimeDialog(model);
            return true;
        });
    }

    private void showEditTimeDialog(RemoteModel model) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Atur Waktu " + model.getName());
        
        final EditText input = new EditText(context);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Menit");
        builder.setView(input);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String txt = input.getText().toString();
            if(!txt.isEmpty()){
                int min = Integer.parseInt(txt);
                actionListener.onUpdateDefaultTime(model.getId(), min * 60 * 1000L);
            }
        });
        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public int getItemCount() {
        return remoteList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTimer;
        View statusDot;
        Button btnStart, btnReset, btnStopAlarm;

        public ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_remote_name);
            tvTimer = itemView.findViewById(R.id.tv_timer_display);
            statusDot = itemView.findViewById(R.id.status_dot);
            btnStart = itemView.findViewById(R.id.btn_start_pause);
            btnReset = itemView.findViewById(R.id.btn_reset);
            btnStopAlarm = itemView.findViewById(R.id.btn_stop_alarm);
        }
    }
}
