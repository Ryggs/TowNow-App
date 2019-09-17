package com.ryggs.towapp.riderapp.Common;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.ryggs.towapp.riderapp.Interfaces.IFCMService;
import com.ryggs.towapp.riderapp.Messages.Errors;
import com.ryggs.towapp.riderapp.Messages.Message;
import com.ryggs.towapp.riderapp.Messages.Messages;
import com.ryggs.towapp.riderapp.Model.FCMResponse;
import com.ryggs.towapp.riderapp.Model.Notification;
import com.ryggs.towapp.riderapp.Model.Pickup;
import com.ryggs.towapp.riderapp.Model.Sender;
import com.ryggs.towapp.riderapp.Model.Token;
import com.ryggs.towapp.riderapp.Model.User;
import com.ryggs.towapp.riderapp.Retrofit.GoogleMapsAPI;
import com.ryggs.towapp.riderapp.Retrofit.IFCMClient;
import com.ryggs.towapp.riderapp.Retrofit.IGoogleAPI;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Common {
    public static final String driver_tbl="Drivers";
    public static final String user_driver_tbl="DriversInformation";
    public static final String history_rider = "RiderHistory";
    public static final String user_rider_tbl="RidersInformation";
    public static final String pickup_request_tbl="PickupRequest";
    public static final String CHANNEL_ID_ARRIVED="ARRIVED";
    public static String token_tbl="Tokens";
    public static String rate_detail_tbl="RateDetails";
    public static final int PICK_IMAGE_REQUEST = 9999;

    public static User currentUser=new User();
    public static String userID;

    public static boolean driverFound=false;
    public static String driverID="";
    public static LatLng currenLocation;

    public static final String fcmURL="https://fcm.googleapis.com/";
    public static final String googleAPIUrl="https://maps.googleapis.com";

    private static double baseFare=2.55;
    private static double timeRate=0.35;
    private static double distanceRate=1.75;

    public static double getPrice(double km, int min){
        return (baseFare+(timeRate*min)+(distanceRate*km));
    }

    public static IFCMService getFCMService(){
        return IFCMClient.getClient(fcmURL).create(IFCMService.class);
    }
    public static IGoogleAPI getGoogleService(){
        return GoogleMapsAPI.getClient(googleAPIUrl).create(IGoogleAPI.class);
    }
    public static void sendRequestToDriver(final String driverID, final IFCMService mService, final Context context, final LatLng lastLocation) {
        DatabaseReference tokens=FirebaseDatabase.getInstance().getReference(Common.token_tbl);

        tokens.orderByKey().equalTo(driverID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapShot:dataSnapshot.getChildren()){
                    Token token=postSnapShot.getValue(Token.class);
                    Pickup pickup=new Pickup();
                    pickup.setLastLocation(lastLocation);
                    pickup.setID(userID);
                    pickup.setToken(token);
                    String json_pickup=new Gson().toJson(pickup);

                    String riderToken=FirebaseInstanceId.getInstance().getToken();
                    Notification data=new Notification("Pickup", json_pickup);
                    Sender content=new Sender(token.getToken(), data);

                    mService.sendMessage(content).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            if (response.body().success==1) Message.message(context, Messages.REQUEST_SUCCESS);
                            else Message.messageError(context, Errors.SENT_FAILED);
                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Log.d("ERROR", t.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
