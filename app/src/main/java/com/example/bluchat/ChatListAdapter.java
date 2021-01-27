package com.example.bluchat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;


public class ChatListAdapter extends BaseAdapter {
    // Member Variables
    private final Activity mActivity;
    private final String mAuthor;
    private final ArrayList<ChatMessage> mChatMessages;

    // Constructor
    public ChatListAdapter(Activity activity, String author, ArrayList<ChatMessage> chatMessages) {
        mActivity = activity;
        mAuthor = author;
        mChatMessages = chatMessages;
    }

    // Inner Class
    private static class ViewHolder {
        TextView authorName;
        TextView body;
        LinearLayout.LayoutParams params;
    }

    // Methods
    @Override
    public int getCount() {
        return mChatMessages.size();
    }

    @Override
    public ChatMessage getItem(int i) {
        return mChatMessages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.activity_chat_bubble, viewGroup, false);

            final ViewHolder holder = new ViewHolder();
            holder.authorName = view.findViewById(R.id.author);
            holder.body = view.findViewById(R.id.message);
            holder.params = (LinearLayout.LayoutParams) holder.authorName.getLayoutParams();

            view.setTag(holder);
        }

        final ChatMessage chatMessage = getItem(i);

        final ViewHolder holder = (ViewHolder) view.getTag();

        boolean isMe = chatMessage.getAuthor().equals(mAuthor);
        setChatRowAppearance(isMe, holder);

        String author = chatMessage.getAuthor();
        holder.authorName.setText(author);

        String msg = chatMessage.getMessage();
        holder.body.setText(msg);

        return view;
    }

    private void setChatRowAppearance(boolean isItMe, ViewHolder holder){
        if (isItMe){
            holder.authorName.setTextColor(Color.GREEN);
            holder.body.setBackgroundResource(R.drawable.rightbubble);
            holder.params.gravity = Gravity.END;
        }
        else{
            holder.authorName.setTextColor(Color.BLUE);
            holder.body.setBackgroundResource(R.drawable.leftbubble);
            holder.params.gravity = Gravity.START;
        }

        holder.authorName.setLayoutParams(holder.params);
        holder.body.setLayoutParams(holder.params);
    }

}
