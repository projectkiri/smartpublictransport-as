package travel.kiri.smarttransportapp;

import android.app.ProgressDialog;
import android.content.Context;

public class LoadingDialog extends ProgressDialog {

	public LoadingDialog(Context context) {
		super(context);
		setMessage(context.getResources().getText(R.string.please_wait));
		setIndeterminate(true);
		setCancelable(true);
	}
}
