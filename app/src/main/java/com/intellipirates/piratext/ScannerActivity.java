package com.intellipirates.piratext;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class ScannerActivity extends AppCompatActivity {

    private ImageView captureIV;
    private TextView resultTV;
    private Button snapBtn, detectBtn;
    private Bitmap imageBitmap;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    String idToken;
    FirebaseUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        captureIV = findViewById(R.id.idIVCaptureImage);
        resultTV = findViewById(R.id.idTVDetectedText);
        snapBtn = findViewById(R.id.idBtnSnap);
        detectBtn = findViewById(R.id.idBtnDetect);

        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectText();

            }
        });


        snapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()) {
                    captureImage();
                } else {
                    requestPermissions();
                }

            }
        });
    }

    private boolean checkPermissions() {
        int camerPermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);
        return camerPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        int PERMISSION_CODE = 2699;
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
    }

    private void captureImage() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0) {
            boolean cameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (cameraPermission) {
                Toast.makeText(this, "PERMISSIONS GRANTED", Toast.LENGTH_SHORT).show();
                captureImage();
            } else {
                Toast.makeText(this, "PERMISSIONS DENIED", Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            captureIV.setImageBitmap(imageBitmap);

        }
    }

    private void detectText() {
        InputImage image = InputImage.fromBitmap(imageBitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Task<Text> result = recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
            @Override
            public void onSuccess(@NonNull Text text) {
                StringBuilder result = new StringBuilder();
                for (Text.TextBlock block : text.getTextBlocks()) {
                    String blockText = block.getText();
                    Point[] blockCornerPoint = block.getCornerPoints();
                    Rect blockFrame = block.getBoundingBox();
                    for (Text.Line line : block.getLines()) {
                        String lineText = line.getText();
                        Point[] lineCornerPoint = line.getCornerPoints();
                        Rect lineRect = line.getBoundingBox();
                        for (Text.Element element : line.getElements()) {
                            String elementText = element.getText();
                            result.append(elementText);
                        }
                        String resultFinal = result.toString();
                        resultTV.setText(blockText);
                        resultTV.setMovementMethod(new ScrollingMovementMethod());
                        processTextBlock(resultFinal);


                    }

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(ScannerActivity.this, "FAIL TO DETECT TEXT FROM THIS IMAGE" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void processTextBlock(String resultText) {
        // [START mlkit_process_text_block]

        /*for (Text.TextBlock block : result.getTextBlocks()) {
            String blockText = block.getText();
            Point[] blockCornerPoints = block.getCornerPoints();
            Rect blockFrame = block.getBoundingBox();
            for (Text.Line line : block.getLines()) {
                String lineText = line.getText();
                Point[] lineCornerPoints = line.getCornerPoints();
                Rect lineFrame = line.getBoundingBox();
                for (Text.Element element : line.getElements()) {
                    String elementText = element.getText();
                    Point[] elementCornerPoints = element.getCornerPoints();
                    Rect elementFrame = element.getBoundingBox();
                }
            }
        }*/

        RequestQueue requestQueue = Volley.newRequestQueue(ScannerActivity.this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://piratext-be.herokuapp.com/new", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                if (response != null) {
                    Log.v("Your Array Response", response);
                } else {
                    Log.v("Your Array Response", "Data Null");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(ScannerActivity.this, "Successfully sent text to web interface at https://hardcore-williams-6b1a12.netlify.app/ ", Toast.LENGTH_SHORT).show();

            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Log.v("get header is called", "f");
                Map<String, String> params = new HashMap<String, String>();
                mUser = FirebaseAuth.getInstance().getCurrentUser();
                if (mUser != null) {
                    Log.v("Your Array Response", mUser.getEmail());
                    System.out.println("detail " + mUser.getEmail());
                    mUser.getIdToken(true)
                            /*.addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                                public void onComplete(@NonNull Task<GetTokenResult> task) {
                                    if (task.isSuccessful()) {
                                        idToken = task.getResult().getToken();
                                        // Send token to your backend via HTTPS
                                        // ...
                                    } else {
                                        idToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjQwMTU0NmJkMWRhMzA0ZDc2NGNmZWUzYTJhZTVjZDBlNGY2ZjgyN2IiLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiQ2hhcmNoaXQgQmFuc2FsIiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS9hL0FBVFhBSndXTi1KUnl5ek9TV2ZWTTBkOXU2dW5ta242N2ZyUklGX2h0ZWRZPXM5Ni1jIiwiaXNzIjoiaHR0cHM6Ly9zZWN1cmV0b2tlbi5nb29nbGUuY29tL3BpcmF0ZXh0LXBpcmF0ZWNhcmEiLCJhdWQiOiJwaXJhdGV4dC1waXJhdGVjYXJhIiwiYXV0aF90aW1lIjoxNjQyMjM0OTkyLCJ1c2VyX2lkIjoiMFQ0WmRhODdZQVBMWHQ4VlJzc3IwdXNpc3pNMiIsInN1YiI6IjBUNFpkYTg3WUFQTFh0OFZSc3NyMHVzaXN6TTIiLCJpYXQiOjE2NDIyNDAwMjksImV4cCI6MTY0MjI0MzYyOSwiZW1haWwiOiJjaGFyY2hpdDk1QGdtYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJmaXJlYmFzZSI6eyJpZGVudGl0aWVzIjp7Imdvb2dsZS5jb20iOlsiMTEzOTY2ODcyNzAwNTAwNjM2MDQ2Il0sImVtYWlsIjpbImNoYXJjaGl0OTVAZ21haWwuY29tIl19LCJzaWduX2luX3Byb3ZpZGVyIjoiZ29vZ2xlLmNvbSJ9fQ.CmqXp0chPZi_ulWvMOQGJ2rxOWthYw_Cg5uqdu8n43ztMHLdY6i7sWaCsPiJMs6oOCoUghkXQhBWJKQEEh5w9CB-Rq3mWhT2rfhjX-UIO1ZEAcVytjl2E8eZ_RVydHTzSVM6TuYS4Co5ZFObmEEwwfRakhOaiYBISc3eLlUohu7Kd0PxgbxhXRddSfPLaIpGtdJotmEHP0zmuX_MBjcqGz2tbq-ZkX7LS5-_mPnyp6VTCZfBlGNd5OOo-sPBkoMjlNwBRICCafJZY3H7XrM7njoN_g1_TeSQECUAyFM_nA0QhpYP1oqkS_JL2NfkfPNZlvsbWI7_MlYDLiYfLMqPfA";
                                        // Handle error -> task.getException();
                                    }
                                }
                            });*/
                            .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                                @Override
                                public void onSuccess(@NonNull GetTokenResult getTokenResult) {
                                    idToken = getTokenResult.getToken();
                                    Log.v("token :", idToken);
                                    params.put("Content-Type", "application/json; charset=UTF-8");
                                    params.put("Authorization", "Bearer " + idToken);
                                    Log.v("Your Array Response", String.valueOf(params));

                                }
                            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.v("error:", e.toString());
                        }
                    })
                    ;

                }
                else{
                    Log.v("message firebase :", "muser is null");
                }
                return params;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> paramMap = new HashMap<String, String>();
                paramMap.put("text", resultText);
                return paramMap;

            }
        };

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(stringRequest);
        // [END mlkit_process_text_block]
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        mUser = FirebaseAuth.getInstance().getCurrentUser();
//        Log.v("mesage muser", mUser.getDisplayName());
//    }
}

