package id.dhanar10.e_moneybalance;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.PendingIntent;
import android.nfc.NfcAdapter;
import android.widget.TextView;
import android.content.Intent;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends AppCompatActivity {
    public static final byte[] APDU_COMMAND_READ_CARD_BALANCE = new byte[] {
            (byte) 0x00, (byte) 0xb5, (byte) 0x00, (byte) 0x00, (byte) 0x0a};

    private NfcAdapter mNfcAdapter;
    private TextView mNfcTextView;
    private PendingIntent mPendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        this.mNfcTextView = (TextView) findViewById(R.id.nfcTextView);

        if ( this.mNfcAdapter == null) {
            this.mNfcTextView.setText("NFC adapter not found!");
        } else if ( this.mNfcAdapter.isEnabled()) {
            this.mNfcTextView.setText("Tap your card!");
            try {
                resolveIntent(getIntent());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(PendingIntent.FLAG_NO_CREATE), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (this.mNfcAdapter != null) {
            if (!this.mNfcAdapter.isEnabled()) {
                this.mNfcTextView.setText("NFC adapter disabled!");
            }
            this.mNfcAdapter.enableForegroundDispatch(this, this.mPendingIntent, null, null);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        try {
            resolveIntent(intent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resolveIntent(Intent intent) throws IOException {
        String action = intent.getAction();

        if (this.mNfcAdapter.isEnabled() && NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Tag card = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            IsoDep isoDep = IsoDep.get(card);
            if (isoDep == null) {
                this.mNfcTextView.setText("Card not supported!");
                throw new IOException(); // FIXME
            }
            isoDep.connect();
            try {
                byte[] resApdu = isoDep.transceive(this.APDU_COMMAND_READ_CARD_BALANCE);
                String hexBal = String.format("%02x%02x%02x", resApdu[2], resApdu[1], resApdu[0]);
                int decBal = Integer.parseInt(hexBal, 16);
                this.mNfcTextView.setText(String.format("Rp%,d", decBal)); // FIXME
            } catch (Exception e) {
                StringWriter stackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(stackTrace));
                this.mNfcTextView.setText(stackTrace.toString());
            }
            isoDep.close();
        }
    }
}
