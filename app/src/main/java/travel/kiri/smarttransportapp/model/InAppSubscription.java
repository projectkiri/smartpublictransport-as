package travel.kiri.smarttransportapp.model;

import java.util.Calendar;

import travel.kiri.smarttransportapp.MainActivity;
import travel.kiri.smarttransportapp.R;
import travel.kiri.smarttransportapp.util.IabHelper;
import travel.kiri.smarttransportapp.util.IabResult;
import travel.kiri.smarttransportapp.util.Inventory;
import travel.kiri.smarttransportapp.util.Purchase;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.widget.Toast;

public class InAppSubscription {

	public static final String PREF_SKU = "travel.kiri.smarttransportapp.model.InAppSubscription.sku";
	public static final String PREF_PURCHASE_TIME = "travel.kiri.smarttransportapp.model.InAppSubscription.purchaseTime";
	
	private static final String base64EncodedPublicKey = "BAQADIQ4UAW7gqpzVYC5jiVvqOILLVAFRvJbiAUYjrwJH4aLjFr9TbaQ11T9ewxjLGxajGgjgXpu4ysZ6Uoor8CjU/Gk/GKiDu6OZut8eG+vWxmLZJPrt4OHpanVlO0iGrHHdIx1OxC7SCiIEdLa6MCHScAj+k6wQ1dkn1afvVVtcGblrpV/jQpoH/GuK+hgiPTltyS4JGVO2kUaPgQXUmX2+DVhjSN3vByw+sDuLft0vqtg3/FPPeqgs6e8WEq4tK0nLcvbYmJ5hWjD7gcU0d1p/+pmpmfCT5zPkcJrrMh95QJE+Si0m4QSQN0pPQqotPlZu5WCnTd3nlxh53UEgjFY+BQsAEQACKgCBIIMA8QACOAAFEQAB0w9GikhqkgBNAjIBIIM";
	
	public static final String SKU_1YEAR = "man_1year";

	public static final long UNIXTIME_1YEAR = MainActivity.DEBUG_MODE ? 86400000L : 31536000000L; 
	
	public IabHelper iabHelper;

	/** The singleton instance of this class. */
	private static InAppSubscription instance;
	
	private Activity mainActivity;
	
	private Long purchaseTime;
	
	public static InAppSubscription createInstance(Activity mainActivity) {
		if (instance == null) {
			instance = new InAppSubscription(mainActivity);
		}
		return instance;
	}

	public static InAppSubscription getInstance(Activity backupActivity) throws NullPointerException {
		if (instance == null) {
			if (backupActivity != null) {
				instance = createInstance(backupActivity);
			} else {
				throw new NullPointerException("createInstance must be called first!");
			}
		}
		return instance;
	}
	
	protected InAppSubscription(final Activity mainActivity) {
		// Set up in-app purchase
		this.mainActivity = mainActivity;
		String decoded = new StringBuilder(base64EncodedPublicKey).reverse().toString();
		iabHelper = new IabHelper(mainActivity, decoded);
		try {
			iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
				@Override
				public void onIabSetupFinished(IabResult result) {
					if (result.isSuccess()) {
						refreshPurchaseInfo(mainActivity);
					}
				}
			});
		} catch (NullPointerException npe) {
			// Seems that In-app is not available, just silent and will
			// show error when purchaes is performed.
		}
	}
	
	public void destroy() {
		if (iabHelper != null) {
			iabHelper.dispose();
		}
		iabHelper = null;
		instance = null;
	}
	
	public IabHelper getIabHelper() {
		return iabHelper;
	}
	
	public void savePurchaseTimeToStorage() {
		SharedPreferences saver = mainActivity.getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = saver.edit();
		if (purchaseTime == null || isExpired(purchaseTime)) {
			editor.remove(PREF_PURCHASE_TIME);
		} else {
			editor.putString(PREF_PURCHASE_TIME, purchaseTime.toString());
		}
		editor.apply();
	}
	
	public void loadPurchaseTimeFromStorage() {
		SharedPreferences saver = mainActivity.getPreferences(Context.MODE_PRIVATE);
		String purchaseTimeString = saver.getString(PREF_PURCHASE_TIME, null);
		purchaseTime = purchaseTimeString == null ? null : Long.valueOf(purchaseTimeString);
	}
	
	public void refreshPurchaseInfo(final Activity activity) {
		// 1st, check from local storage.
		loadPurchaseTimeFromStorage();
		
		// Then, update from GPlay
		iabHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
			
			@Override
			public void onQueryInventoryFinished(IabResult result, Inventory inv) {
				if (result.isFailure()) {
					Toast debugToast = Toast.makeText(activity, result.getMessage(), Toast.LENGTH_LONG);
					debugToast.show();
				} else {
					Purchase purchase = inv.getPurchase(SKU_1YEAR);
					if (purchase == null) {
						purchaseTime = null;
					} else {
						purchaseTime = inv.getPurchase(SKU_1YEAR).getPurchaseTime();
						savePurchaseTimeToStorage();
						if (isExpired(purchaseTime)) {
							consumeSubscription(new IabHelper.OnConsumeFinishedListener() {
								@Override
								public void onConsumeFinished(Purchase purchase, IabResult result) {
									if (result.isSuccess()) {
										AlertDialog.Builder builder = new AlertDialog.Builder(activity);
										builder.setTitle(R.string.subscription);
										builder.setMessage(R.string.subscription_expired);
										builder.setPositiveButton(R.string.ok, new OnClickListener() {
											@Override
											public void onClick(DialogInterface arg0, int arg1) {
												arg0.dismiss();
											}
										});
										builder.show();
									} else {
										throw new RuntimeException("Error: Consume fail/" + result.getMessage());
									}
								}
							});
						}
					}
				}
			}
		});

	}

	public void consumeSubscription(final IabHelper.OnConsumeFinishedListener listener) {
		iabHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
			
			@Override
			public void onQueryInventoryFinished(IabResult result, Inventory inv) {
				if (result.isSuccess()) {
					Purchase purchase = inv.getPurchase(SKU_1YEAR);
					if (purchase != null) {
						iabHelper.consumeAsync(inv.getPurchase(SKU_1YEAR), listener);
					} else {
						throw new RuntimeException("Warning: purchase is already consumed");						
					}
				} else {
					throw new RuntimeException("Error: Inventory Query Fail/" + result.getMessage());
				}
			}
		});
	}
	
	private static boolean isExpired(long purchaseTime) {
		return purchaseTime + UNIXTIME_1YEAR < Calendar.getInstance().getTimeInMillis();
	}
	
	public Long getExpiryTime() {
		if (purchaseTime == null) {
			return null;
		} else {
			return purchaseTime + UNIXTIME_1YEAR;
		}
	}
	
	public Long getPurchaseTime() {
		return purchaseTime;
	}
	
	public boolean isSubscriptionActive() {
		return purchaseTime != null && !isExpired(purchaseTime);
	}
}
