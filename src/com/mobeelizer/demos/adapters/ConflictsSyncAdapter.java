//
// ConflictsSyncAdapter.java
// 
// Copyright (C) 2012 Mobeelizer Ltd. All Rights Reserved.
// 
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License. You may obtain a copy 
// of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
// License for the specific language governing permissions and limitations under
// the License.
// 

package com.mobeelizer.demos.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.mobeelizer.demos.R;
import com.mobeelizer.demos.activities.BaseActivity.UserType;
import com.mobeelizer.demos.custom.EntityState;
import com.mobeelizer.demos.custom.MyArrayAdapter;
import com.mobeelizer.demos.model.ConflictsEntity;

/**
 * An adapter for a list view in "Conflicts" example.
 * 
 * @see MyArrayAdapter
 * @see ArrayAdapter
 * @see ConflictsEntity
 */
public class ConflictsSyncAdapter extends MyArrayAdapter<ConflictsEntity> {

    private LayoutInflater mInflater = null;

    private final LayoutParams mParams;

    private final Drawable mStar;

    public ConflictsSyncAdapter(final Context context, final List<ConflictsEntity> objects) {
        super(context, R.layout.itemized_list_item, R.id.listItemLowerText, objects);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mStar = context.getResources().getDrawable(R.drawable.ic_star);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        ViewHolder vh = null;
        // check whether the view can be reused
        if (convertView == null) {
            // if not inflate the new one
            convertView = mInflater.inflate(R.layout.itemized_list_item, null, false);

            // obtain references to child elements
            vh = new ViewHolder();
            vh.text = (TextView) convertView.findViewById(R.id.listItemText);
            vh.star = (LinearLayout) convertView.findViewById(R.id.listItemStar);
            vh.user = (ImageView) convertView.findViewById(R.id.listItemUser);
            vh.conflict = (ImageView) convertView.findViewById(R.id.listItemConflict);
            vh.overlay = convertView.findViewById(R.id.listItemOverlay);
            // and store them as a tag in the view for later reuse
            convertView.setTag(vh);

            vh.star.setVisibility(View.VISIBLE);
        } else {
            // if there is a view to reuse get child elements references
            vh = (ViewHolder) convertView.getTag();
        }
        // clear the ongoing animation so that on devices with Android prior to 3.0 overlay animation
        // wont be displayed on wrong elements after rapid items addition
        vh.overlay.clearAnimation();
        vh.overlay.setBackgroundColor(getContext().getResources().getColor(android.R.color.transparent));

        // get current entity
        ConflictsEntity ce = getItem(position);

        vh.text.setText(ce.getTitle());
        // show conflict indicator if necessary
        vh.conflict.setVisibility(ce.isConflicted() ? View.VISIBLE : View.GONE);

        vh.star.removeAllViews();
        // and an owner
        UserType user = UserType.valueOf(ce.getOwner());
        switch (user) {
            case A:
                vh.user.setImageResource(R.drawable.bt_user_a_small);
                break;
            case B:
                vh.user.setImageResource(R.drawable.bt_user_b_small);
                break;
        }

        // add stars to match the rating
        for (int i = 0; i < ce.getScore(); i++) {
            ImageView star = new ImageView(getContext());
            star.setLayoutParams(mParams);
            star.setImageDrawable(mStar);
            vh.star.addView(star);
        }

        // if the entity state has changed play the animation
        if (ce.getEntityState() != EntityState.NONE) {
            Animation anim = null;
            int delay = 0;

            switch (ce.getEntityState()) {
            // animation played when the item was added or removed by the user
                case NEW_A:
                case REMOVED_A:
                    anim = AnimationUtils.loadAnimation(getContext(), R.anim.list_item_overlay);
                    delay = 900;
                    break;
                // animation played when the item was synchronized
                case NEW_S:
                case REMOVED_S:
                    anim = AnimationUtils.loadAnimation(getContext(), R.anim.list_item_sync_overlay);
                    delay = 2000;
                    break;
            }
            int color = 0;
            switch (ce.getEntityState()) {
            // green overlay
                case NEW_A:
                case NEW_S:
                    color = getContext().getResources().getColor(R.color.listItemAddedOverlay);
                    break;
                // red overlay
                case REMOVED_A:
                case REMOVED_S:
                    color = getContext().getResources().getColor(R.color.listItemRemovedOverlay);
                    break;
            }

            // set the background
            vh.overlay.setBackgroundColor(color);
            // and animate it
            vh.overlay.startAnimation(anim);

            // when the animation finishes change the background back to transparent
            final View overlay = vh.overlay;
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    overlay.setBackgroundColor(getContext().getResources().getColor(android.R.color.transparent));
                }
            }, delay);

            ce.setEntityState(EntityState.NONE);
        }

        return convertView;
    }

    /**
     * Class that holds references for views contained by list item. It implements a ViewHolder pattern for efficient reuse of
     * views in {@link ListView}.
     */
    private class ViewHolder {

        public TextView text;

        public LinearLayout star;

        public ImageView user;

        public ImageView conflict;

        public View overlay;
    }
}
