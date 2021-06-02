package com.bennet.wallet.services;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.bennet.wallet.preferences.CardPreferenceManager;
import com.bennet.wallet.activities.cards.CardActivity;

public class DeleteCardService extends JobIntentService {
    private static final int JOB_ID = 2339;

    /**
     * Enqueues work for deleting card with given <code>ID</code> (also removes the card id from list of all cardIDs)
     * @param context context
     * @param intent intent to start service (no extras needed)
     * @param ID id of the card to get deleted
     */
    public static void enqueueWork(Context context, Intent intent, int ID) {
        intent.putExtra(CardActivity.EXTRA_CARD_ID, ID);
        enqueueWork(context, DeleteCardService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        int ID = intent.getIntExtra(CardActivity.EXTRA_CARD_ID, -1);
        CardPreferenceManager.deleteCard(this, ID); // Ctrl + Q
    }

}
