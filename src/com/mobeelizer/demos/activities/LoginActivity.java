//
// LoginActivity.java
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

package com.mobeelizer.demos.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mobeelizer.demos.ApplicationStatus;
import com.mobeelizer.demos.R;
import com.mobeelizer.demos.activities.BaseActivity.UserType;
import com.mobeelizer.demos.utils.UIUtils;
import com.mobeelizer.java.api.MobeelizerOperationError;
import com.mobeelizer.mobile.android.Mobeelizer;
import com.mobeelizer.mobile.android.api.MobeelizerOperationCallback;

/**
 * Application starting point, allows the user to create or connect to an existing session.
 * 
 * @see MobeelizerOperationCallback
 */
public class LoginActivity extends Activity implements MobeelizerOperationCallback {

    private Button mCreateSessionButton, mConnectButton;

    private EditText mSessionCodeEditText;

    private Animation mShakeAnimation;

    private Dialog mLoginDialog = null;

    private SharedPreferences mSharedPrefs;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        try {
            Class.forName("android.os.AsyncTask");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_login);

        mShakeAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake);

        mCreateSessionButton = (Button) findViewById(R.id.loginCreateSession);
        mConnectButton = (Button) findViewById(R.id.loginSessionConnect);
        mSessionCodeEditText = (EditText) findViewById(R.id.loginSessionCode);

        mCreateSessionButton.setOnClickListener(getOnCreateSessionClickListenter());
        mConnectButton.setOnClickListener(getOnConnectClickListenter());

        UIUtils.prepareClip(mCreateSessionButton);
        UIUtils.prepareClip(mConnectButton);

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String code = mSharedPrefs.getString(BaseActivity.SESSION_CODE, null);

        // check for stored user data
        if (code != null) {

            mLoginDialog = new Dialog(this, R.style.MobeelizerDialogTheme);
            mLoginDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mLoginDialog.setContentView(R.layout.progress_dialog);
            ((TextView) mLoginDialog.findViewById(R.id.dialogText)).setText(R.string.loggingIn);
            mLoginDialog.setCancelable(false);
            mLoginDialog.show();

            // If present try to login using them
            UserType user = UserType.valueOf(mSharedPrefs.getString(BaseActivity.USER_TYPE, "A"));
            if (user == UserType.A) {
                Mobeelizer.login(code, getString(R.string.c_userALogin), getString(R.string.c_userAPassword), this);
            } else if (user == UserType.B) {
                Mobeelizer.login(code, getString(R.string.c_userBLogin), getString(R.string.c_userBPassword), this);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        ApplicationStatus.activityResumed(this);
        super.onResume();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        ApplicationStatus.activityPaused();
        super.onPause();
    }

    /**
     * Allows to display information or error dialog with specific text to the user. Round brackets contains the {@code id} value
     * required to show each dialog.
     * 
     * <p>
     * The custom dialog ({@code BaseActivity.D_CUSTOM}) can be shown. In this case additional information needs to be provided. <br/>
     * Custom dialog requires reference to {@link Bundle} object with the following fields:<br/>
     * - type of the dialog (information or error) - {@code BaseActivity.IS_INFO} as {@code boolean}<br/>
     * - text to display as resource id - {@code BaseActivity.TEXT_RES_ID} as {@code int}<br/>
     * - text to display as String object - {@code BaseActivity.CUSTOM_TEXT} as {@code String} <br/>
     * <br/>
     * The last two can be used interchangeably but when both are used resource id is preferred.
     */
    @Override
    protected Dialog onCreateDialog(final int id, final Bundle args) {
        Dialog dialog = null;
        TextView text = null;
        Button closeButton = null;

        if (id == BaseActivity.D_CUSTOM && args != null) {
            boolean isInfoDialog = args.getBoolean(BaseActivity.IS_INFO, true);
            int resId = args.getInt(BaseActivity.TEXT_RES_ID, -1);
            String customText = args.getString(BaseActivity.CUSTOM_TEXT);

            dialog = new Dialog(this, R.style.MobeelizerDialogTheme);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(isInfoDialog ? R.layout.info_dialog : R.layout.error_dialog);
            text = (TextView) dialog.findViewById(R.id.dialogText);
            closeButton = (Button) dialog.findViewById(R.id.dialogButton);

            if (resId != -1) {
                text.setText(resId);
            } else if (customText != null) {
                text.setText(customText);
            } else {
                dialog = null;
            }
        }

        if (dialog != null) {
            final Dialog tmp = dialog;
            closeButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(final View paramView) {
                    tmp.dismiss();
                }
            });
        }

        return dialog;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSuccess() {
        C2DMReceiver.performPushRegistration();

        // start explore activity
        Intent i = new Intent(getApplicationContext(), ExploreActivity.class);
        startActivity(i);
        // and close current one
        finish();
        if (mLoginDialog != null) {
            mLoginDialog.dismiss();
        }
    }

    @Override
    public void onFailure(final MobeelizerOperationError error) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Bundle err = new Bundle();
        err.putBoolean(BaseActivity.IS_INFO, false);
        err.putInt(BaseActivity.TEXT_RES_ID, R.string.e_cannotConnectToSession);
        showDialog(BaseActivity.D_CUSTOM, err);
        sp.edit().remove(BaseActivity.SESSION_CODE).remove(BaseActivity.USER_TYPE).commit();
        if (mLoginDialog != null) {
            mLoginDialog.dismiss();
        }
    }

    // =====================================================================================
    // ================================= ONCLICKS ==========================================
    // =====================================================================================

    /**
     * Returns {@link View.OnClickListener} for "Create Session" button. When the button has been clicked
     * {@link CreateSessionCodeActivity} is displayed.
     * 
     * @see CreateSessionCodeActivity
     */
    private View.OnClickListener getOnCreateSessionClickListenter() {
        return new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                Intent i = new Intent(getApplicationContext(), CreateSessionCodeActivity.class);
                startActivity(i);
                finish();
            }
        };
    }

    /**
     * Returns {@link View.OnClickListener} for "Connect" button. When the button has been clicked application tries to login as a
     * user B to provided session.
     */
    private View.OnClickListener getOnConnectClickListenter() {
        return new View.OnClickListener() {

            @Override
            public void onClick(final View v) {
                String sessionCode = mSessionCodeEditText.getText().toString();
                if ("".equals(sessionCode)) {
                    mSessionCodeEditText.startAnimation(mShakeAnimation);
                    return;
                }

                mLoginDialog = new Dialog(LoginActivity.this, R.style.MobeelizerDialogTheme);
                mLoginDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                mLoginDialog.setContentView(R.layout.progress_dialog);
                ((TextView) mLoginDialog.findViewById(R.id.dialogText)).setText(R.string.loggingIn);
                mLoginDialog.setCancelable(false);
                mLoginDialog.show();

                Mobeelizer.login(sessionCode, // session code - 1234
                        getString(R.string.c_userBLogin), // the user - B
                        getString(R.string.c_userBPassword), // user password
                        LoginActivity.this); // MobeelizerLoginCallback

                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                        .putString(BaseActivity.SESSION_CODE, sessionCode).putString(BaseActivity.USER_TYPE, UserType.B.name())
                        .commit();
            }
        };
    }
}
