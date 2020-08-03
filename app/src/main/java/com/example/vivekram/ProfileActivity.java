package com.example.vivekram;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private String receiverUid, senderUid, currentState;
    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button sendRequestButton, declineRequestButton;
    private DatabaseReference userRef, chatRequestRef, contactsRef, notificationRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        notificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");

        receiverUid = getIntent().getExtras().get("visit_uid").toString();
        senderUid = mAuth.getCurrentUser().getUid();

//        Toast.makeText(this, senderUid+"\n"+receiverUid, Toast.LENGTH_SHORT).show();

        userProfileImage = findViewById(R.id.visit_profile_image);
        userProfileName = findViewById(R.id.visit_username);
        userProfileStatus = findViewById(R.id.visit_status);
        sendRequestButton = findViewById(R.id.visit_send_button);
        declineRequestButton = findViewById(R.id.decline_request_button);
        currentState = "new";

        retrieveUserInfo();
    }

    private void retrieveUserInfo() {
        userRef.child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {


                    if ((dataSnapshot.exists()) && dataSnapshot.hasChild("image")) {
                        String userImage = dataSnapshot.child("image").getValue().toString();
                        String name = dataSnapshot.child("name").getValue().toString();
                        String status = dataSnapshot.child("status").getValue().toString();

                        Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(userProfileImage);
                        userProfileName.setText(name);
                        userProfileStatus.setText(status);

                        manageChatRequest();
                    } else {
                        String name = dataSnapshot.child("name").getValue().toString();
                        String status = dataSnapshot.child("status").getValue().toString();

                        userProfileName.setText(name);
                        userProfileStatus.setText(status);

                        manageChatRequest();
                    }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void manageChatRequest() {

        if (!senderUid.equals(receiverUid)){
            sendRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRequestButton.setEnabled(false);

                    if (currentState.equals("new")){
                        sendChatRequest();
                    }
                    if (currentState.equals("request_sent")){
                        cancelChatRequest();
                    }
                    if (currentState.equals("request_received")){
                        acceptChatRequest();
                    }
                    if (currentState.equals("friends")){
                        removeContact();
                    }

                }
            });

        }
        else{
            sendRequestButton.setVisibility(View.INVISIBLE);
        }

        chatRequestRef.child(senderUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(receiverUid)){
                            String request_type = dataSnapshot.child(receiverUid).child("request_type").getValue().toString();

                            if (request_type.equals("sent")){
                                currentState = "request_sent";
                                sendRequestButton.setText("Cancel Request");
                            }
                            else if (request_type.equals("received")){
                                currentState = "request_received";
                                sendRequestButton.setText("Accept Request");
                                declineRequestButton.setVisibility(View.VISIBLE);
                                declineRequestButton.setEnabled(true);

                                declineRequestButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        cancelChatRequest();
                                    }
                                });

                            }
                        }
                        else {
                            contactsRef.child(senderUid)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.hasChild(receiverUid)){
                                                currentState = "friends";
                                                sendRequestButton.setText("Remove Contact");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });



    }

    private void removeContact() {
        contactsRef.child(senderUid).child(receiverUid)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            contactsRef.child(receiverUid).child(senderUid)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                sendRequestButton.setEnabled(true);
                                                currentState = "new";
                                                sendRequestButton.setText("Send Request");

                                                declineRequestButton.setVisibility(View.INVISIBLE);
                                                declineRequestButton.setEnabled(false);
                                            }

                                        }
                                    });
                        }

                    }
                });
    }


    private void acceptChatRequest() {
        contactsRef.child(senderUid).child(receiverUid)
                .child("Contacts").setValue("saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            contactsRef.child(receiverUid).child(senderUid)
                                    .child("Contacts").setValue("saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                chatRequestRef.child(senderUid).child(receiverUid)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()){
                                                                    chatRequestRef.child(receiverUid).child(senderUid)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if (task.isSuccessful()){
                                                                                        sendRequestButton.setEnabled(true);
                                                                                        currentState = "friends";
                                                                                        sendRequestButton.setText("Remove Contact");

                                                                                        declineRequestButton.setVisibility(View.INVISIBLE);
                                                                                        declineRequestButton.setEnabled(false);

                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });

    }

    private void cancelChatRequest() {
        chatRequestRef.child(senderUid).child(receiverUid)
                    .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            chatRequestRef.child(receiverUid).child(senderUid)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                sendRequestButton.setEnabled(true);
                                                currentState = "new";
                                                sendRequestButton.setText("Send Request");

                                                declineRequestButton.setVisibility(View.INVISIBLE);
                                                declineRequestButton.setEnabled(false);
                                            }

                                        }
                                    });
                        }

                    }
                });
    }

    private void sendChatRequest() {
        chatRequestRef.child(senderUid).child(receiverUid)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            chatRequestRef.child(receiverUid).child(senderUid)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){

                                                HashMap<String, String> chatNotification = new HashMap<>();
                                                chatNotification.put("from", senderUid);
                                                chatNotification.put("type", "request");

                                                notificationRef.child(receiverUid).push()
                                                        .setValue(chatNotification)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()){
                                                            sendRequestButton.setEnabled(true);
                                                            currentState = "request_sent";
                                                            sendRequestButton.setText("Cancel Request");
                                                        }
                                                    }
                                                });


                                            }
                                        }
                                    });
                        }
                    }
                });
    }
}