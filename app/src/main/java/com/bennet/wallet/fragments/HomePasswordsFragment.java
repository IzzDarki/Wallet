package com.bennet.wallet.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;

import com.bennet.wallet.R;
import com.bennet.wallet.activities.passwords.EditPasswordActivity;
import com.bennet.wallet.activities.passwords.ShowPasswordActivity;
import com.bennet.wallet.adapters.PasswordPreviewListItemAdapter;
import com.bennet.wallet.preferences.CardPreferenceManager;
import com.bennet.wallet.preferences.PasswordPreferenceManager;
import com.bennet.wallet.utils.Utility;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class HomePasswordsFragment extends Fragment {

    // UI
    protected RecyclerView passwordsRecyclerView;
    protected FloatingActionButton plusButton;

    // variables
    protected List<PasswordPreview> passwords = new ArrayList<>();

    // preferences
    Utility.PreferenceArrayInt passwordIDs;

    // class for password preview properties
    public static class PasswordPreview {
        public int passwordID;
        public String passwordName;
        // TODO more?

        public PasswordPreview(int passwordID, String name) {
            this.passwordID = passwordID;
            this.passwordName = name;
        }
    }

    public HomePasswordsFragment() {
        super(R.layout.fragment_home_passwords);
    }

    // lifecycle
    @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        passwordsRecyclerView = view.findViewById(R.id.fragment_home_passwords_passwords_recycler_view);
        plusButton = view.findViewById(R.id.fragment_home_passwords_plus_button);

        passwordsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        PasswordPreviewListItemAdapter adapter = new PasswordPreviewListItemAdapter(passwords);
        adapter.setOnItemClickListener(this::onItemClick);
        passwordsRecyclerView.setAdapter(adapter);

        plusButton.setOnClickListener(v -> createNewPassword());
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePasswords();
    }

    // main functions
    protected void showPassword(int passwordID) {
        Intent intent = new Intent(requireContext(), ShowPasswordActivity.class);
        intent.putExtra(ShowPasswordActivity.EXTRA_PASSWORD_ID, passwordID);
        startActivity(intent);
    }

    protected void createNewPassword() {
        Intent intent = new Intent(requireContext(), EditPasswordActivity.class);
        intent.putExtra(EditPasswordActivity.EXTRA_CREATE_NEW_PASSWORD, true);
        startActivity(intent);
    }

    protected void updatePasswords() {
        passwordIDs = PasswordPreferenceManager.readAllPasswordIDs(requireContext());
        passwords.clear();
        for (int passwordID : passwordIDs) {
            String passwordName = CardPreferenceManager.readCardName(requireContext(), passwordID);
            passwords.add(new PasswordPreview(passwordID, passwordName));
        }
        passwordsRecyclerView.getAdapter().notifyDataSetChanged();
    }

    // RecyclerView callback
    protected void onItemClick(View view, int position) {
        showPassword(passwords.get(position).passwordID);
    }

}