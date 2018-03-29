package com.example.zzj.speechapplication;

/*
 * Created by ZhengjieZhu on 3/6/2018.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.drive.query.*;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

@SuppressLint("Registered")
public class Login extends AppCompatActivity{

    private static EditText username;
    private static EditText password;
    private static Button login_button;
    private static GoogleSignInClient mGoogleSignInClient;
    private static int RC_SIGN_IN = 100;
    private static final String TAG = "Login";
    protected DriveClient mDriveClient;
    protected DriveResourceClient mDriveResourceClient;
    protected DriveId mDriveId;
    protected BufferedReader reader;
    protected Intent mainIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mainIntent = new Intent("com.example.zzj.speechapplication.MainActivity");
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Drive.SCOPE_APPFOLDER)
                .requestScopes(Drive.SCOPE_FILE)
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        LoginButton();
        GoogleButton();
    }

    public void LoginButton(){
        username = (EditText)findViewById(R.id.editText_user);
        password = (EditText)findViewById(R.id.editText_password);
        login_button = (Button)findViewById(R.id.button_login);

        login_button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(mainIntent);
                    }
                }
        );
    }

    public void GoogleButton(){
        findViewById(R.id.button_sign_in).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch (v.getId()) {
                            case R.id.button_sign_in:
                                signIn();
                                break;
                        }
                    }
                }
        );
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            // Signed in successfully, show authenticated UI.
            mDriveClient = Drive.getDriveClient(getApplicationContext(), account);
            mDriveResourceClient = Drive.getDriveResourceClient(getApplicationContext(), account);
            updateUI(account);
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            updateUI(null);
        }
    }

    private void updateUI(GoogleSignInAccount account) {
        final Context context = this;
        if (account != null) {
            Query query = new Query.Builder().addFilter(Filters.contains(SearchableField.TITLE, "Disease_results"))
                    .build();
            final Task<MetadataBuffer> queryTask = mDriveResourceClient.query(query);
            queryTask
                    .addOnSuccessListener(this, new OnSuccessListener<MetadataBuffer>() {
                                @Override
                                public void onSuccess(MetadataBuffer metadataBuffer) {
                                    // Handle results...
                                    Metadata metadata = metadataBuffer.get(0);
                                    mDriveId = metadata.getDriveId();
                                    //mainIntent.putExtra("DriveId", mDriveId);
                                    Task<DriveContents> openFileTask =
                                            mDriveResourceClient.openFile(mDriveId.asDriveFile(), DriveFile.MODE_READ_ONLY);

                                    // [START read_contents]
                                    openFileTask
                                            .continueWithTask(new Continuation<DriveContents, Task<Void>>() {
                                                @Override
                                                public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                                                    DriveContents contents = task.getResult();
                                                    // Process contents...
                                                    // [START_EXCLUDE]
                                                    // [START read_as_string]
                                                    try {
                                                        BufferedReader input = new BufferedReader(
                                                                new InputStreamReader(contents.getInputStream()));
                                                        StringBuilder data = new StringBuilder();
                                                        String line;
                                                        while ((line = input.readLine()) != null) {
                                                            data.append(line).append('\n');
                                                        }

                                                        try {
                                                            FileOutputStream output = openFileOutput("Disease_results.csv", Context.MODE_PRIVATE);
                                                            OutputStreamWriter writer = new OutputStreamWriter(output);
                                                            writer.write(data.toString());
                                                            writer.close();
                                                        } catch (IOException e) {
                                                            Log.e(TAG, "Failed to download file.");
                                                        }

                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    // [END read_as_string]
                                                    // [START discard_contents]
                                                    Task<Void> discardTask = mDriveResourceClient.discardContents(contents);
                                                    // [END discard_contents]
                                                    return discardTask;
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // Handle failure
                                                    // [START_EXCLUDE]
                                                    Log.e(TAG, "Unable to read contents", e);
                                                    finish();
                                                    // [END_EXCLUDE]
                                                }
                                            });
                                    // [END read_contents]
                                    Log.e("Drive", "Querying file" + mDriveId.encodeToString());
                                }
                            })
                    .addOnFailureListener(this, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Handle failure...
                            Log.e(TAG, "Unable to query", e);
                        }
                    });
            mainIntent.putExtra("ACCOUNT", account);
            startActivity(mainIntent);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        signOut();
        super.onPause();
    }

    private void signOut() {
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                    }
                });
    }
}
