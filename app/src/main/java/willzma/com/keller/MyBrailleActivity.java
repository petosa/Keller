package willzma.com.keller;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

/**
 * Created by ChaityaShah on 2/20/16.
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
public class MyBrailleActivity extends ListActivity {

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1001;

    private static final String FIREBASE_URL = "https://incandescent-heat-6659.firebaseio.com/";
    private final Context CONTEXT = this;
    private Firebase mFirebaseRef;
    private ValueEventListener mConnectedListener;
    private BrailleTextListAdapter mChatListAdapter;

    private FloatingActionButton captainFabulous;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_braille);
        Firebase.setAndroidContext(this);

        captainFabulous = (FloatingActionButton) findViewById(R.id.voice);


        // Make sure we have a mUsername

        
        // Setup our Firebase mFirebaseRef
        mFirebaseRef = new Firebase(FIREBASE_URL).child("braille");

    }

    @Override
    public void onStart() {
        super.onStart();
        // Setup our view and list adapter. Ensure it scrolls to the bottom as data changes
        final ListView listView = getListView();
        // Tell our list adapter that we only want 50 messages at a time
        mChatListAdapter = new BrailleTextListAdapter(mFirebaseRef.limit(50), this, R.layout.content_my_braille);
        listView.setAdapter(mChatListAdapter);
        listView.setLongClickable(true);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                new TTS(CONTEXT, false).execute((((TextView)((RelativeLayout)(listView.getAdapter().getView(pos, null, listView))).findViewById(R.id.english))).getText().toString());
                return true;
            }
        });
        mChatListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(mChatListAdapter.getCount() - 1);
            }
        });

        // Finally, a little indication of connection status
        mConnectedListener = mFirebaseRef.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                // No-op
            }
        });

        

        captainFabulous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak(v);
            }
        });

        captainFabulous.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                new TTS(CONTEXT, false).execute("Speak a word or phrase to add it to your Braille Book.");
                return false;
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mFirebaseRef.getRoot().child(".info/connected").removeEventListener(mConnectedListener);
        mChatListAdapter.cleanup();
    }

    public void learn(View v) {
        ((TextView)v.findViewById(R.id.english)).getText().toString();
        ((TextView)v.findViewById(R.id.ascii)).getText().toString();
        Intent myIntent = new Intent(MyBrailleActivity.this, LearnActivity.class);
        if(!((((TextView) v.findViewById(R.id.english)).getText().toString()).trim() == "")) {

            myIntent.putExtra("braille", (((TextView)v.findViewById(R.id.ascii)).getText().toString().trim().replaceAll(" ", "")));
            myIntent.putExtra("english", ((TextView) v.findViewById(R.id.english)).getText().toString().trim());
            myIntent.putExtra("current", new Integer(0));
            //myIntent.putExtra("next", 1);
            startActivity(myIntent);
        }
    }

    public void speak(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // Specify the calling package to identify your application
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass()
                .getPackage().getName());

        // Display an hint to the user about what he should say.
        //intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a word or phrase to ");
        // Given an hint to the recognizer about what the user is going to say
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE)

            //If Voice recognition is successful then it returns RESULT_OK
            if(resultCode == RESULT_OK) {
                ArrayList<String> textMatchList = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                Map<String, String> push = new HashMap<String, String>();
                push.put("english", textMatchList.get(0));
                mFirebaseRef.push().setValue(push);
                //Result code for various error.
            }else if(resultCode == RecognizerIntent.RESULT_AUDIO_ERROR){
                showToastMessage("Audio Error");
            }else if(resultCode == RecognizerIntent.RESULT_CLIENT_ERROR){
                showToastMessage("Client Error");
            }else if(resultCode == RecognizerIntent.RESULT_NETWORK_ERROR){
                showToastMessage("Network Error");
            }else if(resultCode == RecognizerIntent.RESULT_NO_MATCH){
                showToastMessage("No Match");
            }else if(resultCode == RecognizerIntent.RESULT_SERVER_ERROR){
                showToastMessage("Server Error");
            }
        super.onActivityResult(requestCode, resultCode, data);
    }
    void showToastMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        Intent in = new Intent(MyBrailleActivity.this, MainActivity.class);

        startActivity(in);

        finish();
    }
}