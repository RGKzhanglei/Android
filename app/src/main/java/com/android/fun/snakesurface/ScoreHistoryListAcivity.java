package com.android.fun.snakesurface;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by zhang.la on 2015/4/1.
 */
public class ScoreHistoryListAcivity extends Activity {

    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.score_history_layout);
        mListView = (ListView) findViewById(R.id.history_list);

        ListAdapter listAdapter = new ListAdapter(DBAdapter.getInstance(this).getHistoryRecords());
        mListView.setAdapter(listAdapter);
    }

    private class ListAdapter extends BaseAdapter {

        private ArrayList<ScoreHistoryModel> lists = new ArrayList<ScoreHistoryModel>();

        private ListAdapter(ArrayList<ScoreHistoryModel> models) {
            lists = models;
        }

        @Override
        public int getCount() {
            if (null == lists) return 0;

            return lists.size();
        }

        @Override
        public ScoreHistoryModel getItem(int position) {
            if (null == lists || lists.size() <= 0 || position >= lists.size()) return null;
            return lists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = new ViewHolder();
            if (null == convertView) {
                convertView = LayoutInflater.from(ScoreHistoryListAcivity.this).inflate(R.layout.score_history_item,null);
                viewHolder.sortNumTV = (TextView) convertView.findViewById(R.id.history_num);
                viewHolder.scoreNameTV = (TextView) convertView.findViewById(R.id.history_name);
                viewHolder.scoreNumTV = (TextView) convertView.findViewById(R.id.history_score);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.sortNumTV.setText(position+1 + "");
            viewHolder.scoreNameTV.setText(lists.get(position).scoreName);
            viewHolder.scoreNumTV.setText(lists.get(position).scoreNum + "");
            return convertView;
        }

        private class ViewHolder {
            TextView sortNumTV;
            TextView scoreNameTV;
            TextView scoreNumTV;
        }
    }

    @Override
    public void onBackPressed() {
        this.setResult(RESULT_OK);
        super.onBackPressed();
    }
}
