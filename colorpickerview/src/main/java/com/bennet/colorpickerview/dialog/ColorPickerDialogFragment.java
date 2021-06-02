/*
 * Copyright (C) 2015 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bennet.colorpickerview.dialog;

import com.bennet.colorpickerview.R;
import com.bennet.colorpickerview.view.ColorPanelView;
import com.bennet.colorpickerview.view.ColorPickerView;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.fragment.app.DialogFragment;

public class ColorPickerDialogFragment extends DialogFragment {

	public interface ColorPickerDialogListener {
		void onColorSelected(int dialogId, int color);
		void onDialogDismissed(int dialogId);
	}

	private int mDialogId = -1;
	
	private ColorPickerView mColorPicker;
	private ColorPanelView mOldColorPanel;
	private ColorPanelView mNewColorPanel;
	private LinearLayout mButtonsLayout;
	private Button mOkButton;
	private Button mCustomButton;
	private String mCustomButtonText = null;
	private OnClickListener mCustomButtonOnClickListener = null;

	private ColorPickerDialogListener mListener;
	
	public static ColorPickerDialogFragment newInstance(int dialogId, int initialColor) {
		return newInstance(dialogId, null, null, initialColor, false);
	}
	
	public static ColorPickerDialogFragment newInstance(int dialogId, String title, String okButtonText, int initialColor, boolean showAlphaSlider) {
		
		ColorPickerDialogFragment fragment = new ColorPickerDialogFragment();
		Bundle args = new Bundle();
		args.putInt("id", dialogId);
		args.putString("title", title);
		args.putString("ok_button", okButtonText);
		args.putBoolean("alpha", showAlphaSlider);
		args.putInt("init_color", initialColor);
		
		fragment.setArguments(args);
		
		return fragment;
	}


	public void setCustomButton(final String text, final OnClickListener onClickListener) {
		/*
		if (BuildConfig.DEBUG && text.length() > 5)
			Log.w("ColorPickerDialogFrag.", "The text of the custom button should be quite short, because there's probably very little space. Otherwise the text will be displayed in multiple lines and the size of the color picker dialog gets bigger");
		*/

		if (mButtonsLayout != null) {
			mCustomButton.setText(text);
			mCustomButton.setOnClickListener(onClickListener);
			if (mButtonsLayout.indexOfChild(mCustomButton) == -1)
				mButtonsLayout.addView(mCustomButton, 0);
		}
		else {
			mCustomButtonText = text;
			mCustomButtonOnClickListener = onClickListener;
		}
	}

	public void removeCustomButton() {
		if (mButtonsLayout != null)
			mButtonsLayout.removeView(mCustomButton);
		else {
			mCustomButtonText = null;
			mCustomButtonOnClickListener = null;
		}
	}

	public void setColor(@ColorInt int color) {
		mColorPicker.setColor(color, true);
	}

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mDialogId = getArguments().getInt("id");
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Check for listener in parent activity
		try {
			mListener = (ColorPickerDialogListener) activity;
		} 
		catch (ClassCastException e) {
			e.printStackTrace();
			throw new ClassCastException("Parent activity must implement ColorPickerDialogListener to receive result.");
		}
	}


	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog d = super.onCreateDialog(savedInstanceState);
		
		
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, 
				ViewGroup.LayoutParams.WRAP_CONTENT);
		
		return d;
	}
		
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.colorpickerview__dialog_color_picker, container);
	
		TextView titleView = v.findViewById(android.R.id.title);
		mColorPicker = v.findViewById(R.id.colorpickerview__color_picker_view);
		mOldColorPanel = v.findViewById(R.id.colorpickerview__color_panel_old);
		mNewColorPanel = v.findViewById(R.id.colorpickerview__color_panel_new);
		mButtonsLayout = v.findViewById(R.id.colorpickerview__buttons_linear_layout);
		mCustomButton = v.findViewById(android.R.id.button1);
		mOkButton = v.findViewById(android.R.id.button2);

		if (mCustomButtonText == null)
			removeCustomButton(); // by default
		else {
			setCustomButton(mCustomButtonText, mCustomButtonOnClickListener);
			mCustomButtonText = null;
			mCustomButtonOnClickListener = null;
		}

		mColorPicker.setOnColorChangedListener(newColor -> mNewColorPanel.setColor(newColor));
		
		mOkButton.setOnClickListener(v1 -> {
			mListener.onColorSelected(mDialogId, mColorPicker.getColor());
			getDialog().dismiss();
		});
		
		
		String title = getArguments().getString("title");
		
		if(title != null) {
			titleView.setText(title);
		}
		else {
			titleView.setVisibility(View.GONE);
		}
		
			
		if(savedInstanceState == null) {
			mColorPicker.setAlphaSliderVisible(
					getArguments().getBoolean("alpha"));
			
			
			String ok = getArguments().getString("ok_button");
			if(ok != null) {
				mOkButton.setText(ok);
			}
			
			int initColor = getArguments().getInt("init_color");
			
			mOldColorPanel.setColor(initColor);
			mColorPicker.setColor(initColor, true);
		}
		
		
		return v;
	}


	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);		
		mListener.onDialogDismissed(mDialogId);
	}

}
