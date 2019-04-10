import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.services.people.v1.PeopleScopes;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.kakao.auth.ApiResponseCallback;
import com.kakao.auth.AuthService;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.auth.network.response.AccessTokenInfoResponse;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.MeV2ResponseCallback;
import com.kakao.usermgmt.response.MeV2Response;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import com.nhn.android.naverlogin.ui.view.OAuthLoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import _Library._Util._SharedPreference;

public class ActivityLanding extends Activity implements View.OnClickListener{

    private static final String TAG = ActivityLanding.class.getSimpleName();

    Button mSignIn;
    Button mSignOut;
    Button mSignUp;

    //kakao login
    SessionCallback mSessionCallback;
    Button mKakao_SignIn;

    //naver login
    private OAuthLogin mOAuthLoginModule;
    private OAuthLoginButton mNaverLoginButton;

    //google login
    // 구글로그인 result 상수
    private static final int RC_SIGN_IN = 900;

    // 구글api클라이언트
    private GoogleSignInClient mGoogleSignInClient;

    private GoogleApiClient mGoogleApiClient;

    // 파이어베이스 인증 객체 생성
    private FirebaseAuth mFirebaseAuth;

    // 구글  로그인 버튼
    private SignInButton mGoogle_Signin;


    //facebook login
    CallbackManager mCallbackManager;
    LoginButton mFacebook_Signin;

    _SharedPreference mSharedPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        initView();
        initVariable();



        initKakaoLogin();
        initNaverLogin();
        initGoogleLogin();
        initFacebookLogin();

        checkAutomaticLogin();
    }

    public void checkAutomaticLogin(){
        if(mSharedPreference.getSNS().equals(Key.KAKAO)){
            requestAccessTokenInfo();
        }
        else if(mSharedPreference.getSNS().equals(Key.NAVER)){
            String naverState = mOAuthLoginModule.getState(this).toString();
            if(naverState.equals("OK")){
                callActivityMain();
            }else{
                naver_logout();
            }
        }else if(mSharedPreference.getSNS().equals(Key.FACEBOOK)){
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
            if(isLoggedIn){
                callActivityMain();
            }else{
                facebook_logout();
            }

            SimpleDateFormat df2 = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
            if(accessToken != null) {
                String dateText = df2.format(accessToken.getExpires());
                Log.d(TAG, "date   :   " + dateText);
            }
            Log.d(TAG,"face book isLoggedIn   :   "+isLoggedIn);
        }else if(mSharedPreference.getSNS().equals(Key.GOOGLE)){
            //  FirebaseAuth.getInstance().
            //signInSilently();
            if(isSignedIn()){
                callActivityMain();
            }else{
                google_signOut();
            }

        }else if(mSharedPreference.getSNS().equals(Key.HEARINGAID)){

        }
    }


    public void initView(){
        mSignIn = (Button)findViewById(R.id.ActivityLanding_Button_SignIn);
        mSignOut = (Button)findViewById(R.id.ActivityLanding_Button_SignOut);
        mSignUp = (Button)findViewById(R.id.ActivityLanding_Button_SignUp);
        mSignIn.setOnClickListener(this);
        mSignOut.setOnClickListener(this);
        mSignUp.setOnClickListener(this);


    }

    public void initVariable(){
        mSharedPreference = AppData.getInstance().sharedPreference ;
    }

    public void initKakaoLogin(){
        mSessionCallback = new SessionCallback();
        Session.getCurrentSession().addCallback(mSessionCallback);
    }

    public void initNaverLogin(){
        mNaverLoginButton = (OAuthLoginButton)findViewById(R.id.ActivityLanding_Button_naver);
        mOAuthLoginModule = OAuthLogin.getInstance();
        mOAuthLoginModule.init(
                ActivityLanding.this
                ,"g1SVplz3kQ1MVlU3fnPz"
                ,"l7TXQZiUZK"
                ,"HearingAidShop"
                //,OAUTH_CALLBACK_INTENT
                // SDK 4.1.4 버전부터는 OAUTH_CALLBACK_INTENT변수를 사용하지 않습니다.
        );




        mNaverLoginButton.setOAuthLoginHandler(mOAuthLoginHandler);
    }

    public void initFacebookLogin(){
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        mCallbackManager = CallbackManager.Factory.create();



        mFacebook_Signin = (LoginButton) findViewById(R.id.ActivityLanding_Button_facebook);
        mFacebook_Signin.setReadPermissions("email");
        // If using in a fragment
       // mFacebook_Signin.setFragment(this);

        // Callback registration
        mFacebook_Signin.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                Log.d(TAG,"facebook login success   :   "+loginResult.getAccessToken().toString());
                GraphRequest request = GraphRequest.newMeRequest(//데이터를 받아내기 위해서
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {//데이터를 받아낸 후(네트워크 연결 후)의 콜백
                            @Override
                            public void onCompleted(//완료되었을때 실행된다.
                                                    JSONObject object,
                                                    GraphResponse response) {
                                try {
                                    Log.d(TAG,"face book login result   :   "+object.toString());
                                    String email = object.getString("email");
                                    Toast.makeText(getApplicationContext(), email, Toast.LENGTH_SHORT).show();
                                    mSharedPreference.setSNS(Key.FACEBOOK);
                                }

                                catch (Exception e){
                                    e.printStackTrace();
                                }
                                startActivity(new Intent(ActivityLanding.this,ActivityMain.class));
                                // Application code
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "email,name,gender,birthday,age_range");//데이터를 전부 받아오지 않고 email만 받아온다. "email,name,age" 의 형식으로 받아올수 있음.
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                // App code
                Log.d(TAG,"facebook login onCancel");
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Log.d(TAG,"facebook login onError   :   "+exception.toString());
            }
        });

        /*
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
boolean isLoggedIn = accessToken != null && !accessToken.isExpired();



LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile"));


         */
    }


    public void initGoogleLogin(){
        findViewById(R.id.ActivityLanding_Button_google).setOnClickListener(this);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestServerAuthCode(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestScopes(new Scope(PeopleScopes.USERINFO_PROFILE))
                .build();
        // [END config_signin]


        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // [START initialize_auth]
        mFirebaseAuth = FirebaseAuth.getInstance();
    }

    private void google_signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void google_signOut() {
        // Firebase sign out
        mFirebaseAuth.signOut();

        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        //updateUI(null);
                        Log.d(TAG,"sign out google login");
                    }
                });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);

                SimpleDateFormat df2 = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

                    String dateText = df2.format(account.getExpirationTimeSecs()*1000L);
                    Log.d(TAG, "date   :   " + dateText);

                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // [START_EXCLUDE]
              //  updateUI(null);
                // [END_EXCLUDE]
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        // [START_EXCLUDE silent]
        //showProgressDialog();
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mFirebaseAuth.getCurrentUser();
                            Log.d(TAG,"google   :    "+user.toString()+"                         "+user.getDisplayName()+"     "+user.getEmail()+"    "+user.getProviders().toString());
                            mSharedPreference.setSNS(Key.GOOGLE);
                            startActivity(new Intent(ActivityLanding.this,ActivityMain.class));
                           // updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                          //  Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                         //   updateUI(null);
                        }

                        // [START_EXCLUDE]
                       // hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch(id){
            case R.id.ActivityLanding_Button_SignIn:
                startActivity(new Intent(ActivityLanding.this,ActivityMain.class));
                break;

            case R.id.ActivityLanding_Button_SignOut:
                mSharedPreference.setSNS(Key.NOT_LOGIN);
                kakao_logout();
                naver_logout();
                google_signOut();
                facebook_logout();
                break;

            case R.id.ActivityLanding_Button_SignUp:
                startActivity(new Intent(ActivityLanding.this,ActivitySignUp.class));
                break;

            case R.id.ActivityLanding_Button_google:
                google_signIn();
                break;
        }
    }

    public void kakao_logout(){
        UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
            @Override
            public void onCompleteLogout() {
                //redirectLoginActivity();
                // Toast.makeText(ActivityLanding.this,"Kakao logout",Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void facebook_logout(){
        new GraphRequest(AccessToken.getCurrentAccessToken(), "/me/permissions/", null, HttpMethod.DELETE, new GraphRequest
                .Callback() {
            @Override
            public void onCompleted(GraphResponse graphResponse) {
                FacebookSdk.sdkInitialize(getApplicationContext());
                LoginManager.getInstance().logOut();

            }
        }).executeAsync();
    }

    public void naver_logout(){
        new Thread(){
            public void run(){
               // mOAuthLoginModule.logout(getApplicationContext());
                mOAuthLoginModule.logoutAndDeleteToken(getApplicationContext());
                Log.d(TAG, "errorCode:" + mOAuthLoginModule.getLastErrorCode(getApplicationContext()));
                Log.d(TAG, "errorDesc:" + mOAuthLoginModule.getLastErrorDesc(getApplicationContext()));

            }
        }.start();

    }

   // https://openapi.naver.com/v1/nid/me

    OAuthLoginHandler mOAuthLoginHandler = new OAuthLoginHandler() {
        @Override
        public void run(boolean success) {
            Log.d(TAG,"end");
            if (success) {
                new Thread(){
                    public void run(){
                        String accessToken = mOAuthLoginModule.getAccessToken(getApplicationContext());
                        String refreshToken = mOAuthLoginModule.getRefreshToken(getApplicationContext());
                        long expiresAt = mOAuthLoginModule.getExpiresAt(getApplicationContext());
                        String tokenType = mOAuthLoginModule.getTokenType(getApplicationContext());
                        Date date=new Date(expiresAt* 1000L);
                        SimpleDateFormat df2 = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
                        String dateText = df2.format(date);
                        Log.d(TAG,"date   :   "+dateText);
                        String data = mOAuthLoginModule.requestApi(getApplicationContext(),accessToken,"https://openapi.naver.com/v1/nid/me");
                        try{
                            JSONObject result = new JSONObject(data);

                            String name = result.getJSONObject("response").getString("name");
                            String email = result.getJSONObject("response").getString("email");
                            String gender = result.getJSONObject("response").getString("gender");
                            String age = result.getJSONObject("response").getString("age");
                            Log.i(TAG,"name : "+name+"\n  email : "+email+"\n   gender : "+gender+"\n  age : "+age);
                            mSharedPreference.setSNS(Key.NAVER);
                            startActivity(new Intent(ActivityLanding.this,ActivityMain.class));
                        }catch (JSONException e){
                            e.printStackTrace();
                        }


                        Log.i(TAG,"AccessToken : "+accessToken+"\n  refershToken : "+refreshToken+"\n   expiresAt : "+expiresAt+"\n  tokenType : "+tokenType);

                    }
                }.start();

            } else {
                String errorCode = mOAuthLoginModule.getLastErrorCode(getApplicationContext()).getCode();
                String errorDesc = mOAuthLoginModule.getLastErrorDesc(getApplicationContext());
                Toast.makeText(getApplicationContext(), "errorCode:" + errorCode
                        + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void requestAccessTokenInfo() {
        AuthService.getInstance().requestAccessTokenInfo(new ApiResponseCallback<AccessTokenInfoResponse>() {
            @Override
            public void onSessionClosed(ErrorResult errorResult) {
               // redirectLoginActivity(self);
                Log.d(TAG,"kakao login is unsuccessful");
                kakao_logout();
            }

            @Override
            public void onNotSignedUp() {
                // not happened
                Log.d(TAG,"kakao login is unsuccessful");
               // kakao_logout();
            }

            @Override
            public void onFailure(ErrorResult errorResult) {
                Logger.e("failed to get access token info. msg=" + errorResult);
                Log.d(TAG,"kakao login is failed");
            }

            @Override
            public void onSuccess(AccessTokenInfoResponse accessTokenInfoResponse) {
                long userId = accessTokenInfoResponse.getUserId();
                Log.d(TAG,"this access token is for userId=" + userId);
                Log.d(TAG,"kakao login is successful");
                long expiresInMilis = accessTokenInfoResponse.getExpiresInMillis();
                Log.d(TAG,"this access token expires after " + expiresInMilis + " milliseconds.");
                callActivityMain();
            }
        });
    }
    private void kakao_requestMe(){
        List<String> keys = new ArrayList<>();
        keys.add("properties.nickname");
        keys.add("properties.profile_image");
        keys.add("kakao_account.email");
        keys.add("kakao_account.age_range");
        keys.add("kakao_account.gender");
        UserManagement.getInstance().me(keys,new MeV2ResponseCallback() {
            @Override
            public void onSessionClosed(ErrorResult errorResult) {

            }

            @Override
            public void onSuccess(MeV2Response result) {
                Log.d("UserProfile",result.toString());
                mSharedPreference.setSNS(Key.KAKAO);
                callActivityMain();
            }
        });

    }

    public void callActivityMain(){
        startActivity(new Intent(ActivityLanding.this,ActivityMain.class));
    }

    private class SessionCallback implements ISessionCallback {

        @Override
        public void onSessionOpened() {
           // redirectSignupActivity();
           kakao_requestMe();
        }

        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if(exception != null) {
                Logger.e(exception);
            }
        }
    }

    private boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(this) != null;
    }


}