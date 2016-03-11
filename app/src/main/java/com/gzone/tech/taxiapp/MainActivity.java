package com.gzone.tech.taxiapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {


    Control control = new Control();
    ImageView banner;
    VideoView video;
    TextView fareView;
    TextView distanceView;
    TextView speedView;
    TextView waitingView;
    Button trigger;
    String vehicleno = "2748";

    //Video View Variable
    private int videoID;
    private MediaController mediaControls;
    File file[];
    String videopath;
    int videostatus = 0;
    String videoname;
    int listcount;
    Video v;
    ArrayList<String> videolist = null;

    //GPS Variable
    GPS gps;
    double lat_old;
    double lon_old;
    //   double lat_new;
    //   double lon_new;
    static double total_dectence = 0;
    static double distance;
    static boolean distence_initial = true;
    int fair_distence;
    static double fair_calc = 0;

    //Fare
    int fair_rs = 10;
    static int total_fair;
    int fairarry[];

    //Waiting Time Calculation
    private Handler custom_wait_Handler = new Handler();
    long timeInMilliseconds = 0;
    long timeSwapBuff = 0;
    long updatedTime = 0;
    private long startTime = 0;

    //Banner
    Banner b;
    String bannerid;
    ArrayList<String> bannerlist;


    //Fare
    Boolean hire = false;

    //Downloading
    ProgressBar pb;
    Dialog dialog;
    int downloadedSize = 0;
    int totalSize = 0;
    TextView cur_val;
    String download_file_video = "http://adminpanel.gzone.tech/videos/";
    String download_file_banner = "http://adminpanel.gzone.tech/banners/";
    int nooffiles = 0;
    static JSONObject json_data = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        control.init();
        banner = (ImageView) findViewById(R.id.banner);
        video = (VideoView) findViewById(R.id.videoView);
        fareView = (TextView) findViewById(R.id.fare);
        distanceView = (TextView) findViewById(R.id.distance);
        trigger = (Button) findViewById(R.id.trigger);
        speedView = (TextView) findViewById(R.id.speed);
        waitingView = (TextView) findViewById(R.id.waitingtime);

        //downloadFile(download_file_video+"1231.mp4");

        videopath = Environment.getExternalStorageDirectory() + "/OnlineTaxi/Videos/";
        openImage("0000");
        downloadFileVideo("1231");

        //Video Filelist to videoDirectory
        //localVideos- list of files
        //Videovideolist- List of VideoID in local path
        File f = new File(Environment.getExternalStorageDirectory() + "/OnlineTaxi/Videos/");
        Toast.makeText(this, videopath, Toast.LENGTH_LONG).show();
        File localvideos[] = f.listFiles();
        ArrayList<String> videoDirectory = new ArrayList<String>();
        for (int i = 0; i < localvideos.length; i++) {
            videoDirectory.add(localvideos[i].getName());
            Toast.makeText(this, localvideos[i].getName() + "Check", Toast.LENGTH_LONG).show();
        }
        //Banner FileList to BannerDirectory
        //localBanners = list of files
        //localbannerlist - List of BannerID

        f = new File(Environment.getExternalStorageDirectory() + "/OnlineTaxi/Videos/");
        Toast.makeText(this, videopath, Toast.LENGTH_LONG).show();
        File localbanners[] = f.listFiles();
        ArrayList<String> localbannerlist = new ArrayList<String>();
        for (int i = 0; i < localbanners.length; i++) {
            localbannerlist.add(localbanners[i].getName());
            Toast.makeText(this, localbanners[i].getName() + "Check", Toast.LENGTH_LONG).show();
        }
        //Getting Banners server to bannerlist

        try {
            Banner b = new Banner();
            b.execute();
            json_data = b.display();
            JSONArray jsonarry = json_data.optJSONArray("banner");
            Toast.makeText(this, jsonarry.getString(0), Toast.LENGTH_LONG).show();
            for (int i = 0; i < jsonarry.length(); i++) {
                JSONObject jsonObject = jsonarry.getJSONObject(i);
                Toast.makeText(this, "Getting Banner", Toast.LENGTH_LONG).show();
                bannerlist.add(i, jsonObject.optString("bannerid").toString());
                Toast.makeText(this, jsonObject.optString("bannerid").toString(), Toast.LENGTH_LONG).show();
                Log.e("bannerid", bannerlist.get(i) + "");
            }
        } catch (Exception e) {
            Log.e("Fail 3 banner", e.toString());
        }

        //Getting Videos Server to videolist

        try {
            Video v = new Video();
            v.execute();
            Toast.makeText(this, "Getting Videos", Toast.LENGTH_LONG).show();
            json_data = v.display();
            JSONArray jsonarry1 = json_data.optJSONArray("video");
            Log.e("Comparison of Video", "try");
            for (int i = 0; i < jsonarry1.length(); i++) {
                Log.e("Comparison of Video", "For Loop");
                JSONObject jsonObject = jsonarry1.getJSONObject(i);
                Toast.makeText(this, "Getting Video ", Toast.LENGTH_LONG).show();
                videolist.add(jsonObject.optString("videoid").toString());
                Toast.makeText(this, videolist.get(i), Toast.LENGTH_LONG).show();
                Log.e("videoid", videolist.get(i) + "");
            }
        } catch (Exception e) {
            Log.e("Fail getting Video", e.toString());
        }

        //Downloading non existing VIdeos
        try {
            for (int x = 0; x < videolist.size(); x++) {
                final String listname = videolist.get(x);
                if (!videoDirectory.contains(listname)) {
                    showProgress(download_file_video + listname + ".mp4");
                    new Thread(new Runnable() {
                        public void run() {
                            downloadFileVideo(listname);
                        }
                    }).start();
                    Toast.makeText(this, download_file_video + listname, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e("dwnload non existng", e.toString());
        }

        //Downloading Non existing Banner
        try {
            for (int x = 0; x < bannerlist.size(); x++) {
                final String listname = bannerlist.get(x);
                if (!localdirectory2.contains(listname)) {
                    showProgress(download_file_banner + listname + ".mp4");
                    new Thread(new Runnable() {
                        public void run() {
                            downloadFileVideo(listname);
                        }
                    }).start();
                    Toast.makeText(this, download_file_banner + listname, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e("dwnload non existng", e.toString());
        }

        //Get the Video Filelist
        f = new File(Environment.getExternalStorageDirectory() + "/OnlineTaxi/Videos/");
        Toast.makeText(this, videopath, Toast.LENGTH_LONG).show();
        localvideos = f.listFiles();
        ArrayList<String> localdirectory1 = new ArrayList<String>();
        for (int i = 0; i < localvideos.length; i++) {
            localdirectory1.add(localvideos[i].getName());
            Toast.makeText(this, localvideos[i].getName() + "Check", Toast.LENGTH_LONG).show();
        }

        //Get the Banner FileList
        f = new File(Environment.getExternalStorageDirectory() + "/OnlineTaxi/Videos/");
        Toast.makeText(this, videopath, Toast.LENGTH_LONG).show();
        final File localbanners[] = f.listFiles();
        ArrayList<String> localdirectory2 = new ArrayList<String>();
        for (int i = 0; i < localbanners.length; i++) {
            localdirectory2.add(localbanners[i].getName());
            Toast.makeText(this, localbanners[i].getName() + "Check", Toast.LENGTH_LONG).show();
        }


        //Video Start

        // Completed
        File f2 = new File(Environment.getExternalStorageDirectory() + "/OnlineTaxi/Videos/");
        nooffiles = f2.listFiles().length;
        try {
            videoname = localdirectory1[listcount].getName();
        } catch (Exception e) {
            Log.e("Videoname set", e.toString());
        }
        //Completed
        video.setVideoPath(videopath + videoname);
        try {
            trigger.setOnClickListener(new Button.OnClickListener() {
                                           @Override
                                           public void onClick(View v) {
                                               // 0 initial Start , 1 playing, 2 paused
                                               if (videostatus == 0) {
                                                   video.start();
                                                   videostatus = 1;
                                                   startFare();
                                                   hire = true;
                                                   trigger.setText("Stop");
                                               } else if (videostatus == 1) {
                                                   video.pause();
                                                   AlertDialog.Builder a_builder = new AlertDialog.Builder(MainActivity.this);
                                                   a_builder.setMessage("Finish the Ride?").setCancelable(false).
                                                           setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                               @Override
                                                               public void onClick(DialogInterface dialog, int which) {
                                                                   hire = false;
                                                                   stopFare();
                                                                   dialog.dismiss();

                                                                   trigger.setText("Start");
                                                               }
                                                           })
                                                           .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                               @Override
                                                               public void onClick(DialogInterface dialog, int which) {
                                                                   hire = true;
                                                                   video.start();
                                                                   dialog.cancel();
                                                               }
                                                           });
                                                   AlertDialog alert = a_builder.create();
                                                   alert.setTitle("Confirm Hire");
                                                   alert.show();
                                                   videostatus = 0;
                                               }
                                           }
                                       }
            );
        } catch (Exception e) {

        }
        //Completed
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                control.addcount(videoname.substring(0, videoname.indexOf(".")));
                listcount++;
                if (listcount == nooffiles) {
                    listcount = 0;
                }
                videoname = localdirectory[listcount].getName();
                video.setVideoPath(videopath + videoname);
                video.start();
            }
        });
    }

    void downloadFileBanner(String Filename) {

        try {
            URL url = new URL(download_file_banner + Filename + ".jpg");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);

            //connect
            urlConnection.connect();

            //set the path where we want to save the file
            File SDCardRoot = new File(Environment.getExternalStorageDirectory() + "/OnlineTaxi/Videos/");

            //create a new file, to save the  file
            File file = new File(SDCardRoot, Filename + ".jpg");

            //FileOutputStream fileOutput = new FileOutputStream(file);
            FileOutputStream fos = openFileOutput(Filename + ".jpg", Context.MODE_PRIVATE);
            //Stream used for reading the data from the internet
            InputStream inputStream = urlConnection.getInputStream();

            //this is the total size of the file which we are downloading
            totalSize = urlConnection.getContentLength();

            runOnUiThread(new Runnable() {
                public void run() {
                    pb.setMax(totalSize);
                }
            });

            //create a buffer...
            byte[] buffer = new byte[1024];
            int bufferLength = 0;

            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;
                // update the progressbar //
                runOnUiThread(new Runnable() {
                    public void run() {
                        pb.setProgress(downloadedSize);
                        float per = ((float) downloadedSize / totalSize) * 100;
                        cur_val.setText("Downloaded " + downloadedSize + "KB / " + totalSize + "KB (" + (int) per + "%)");
                    }
                });
            }
            //close the output stream when complete //
            fos.close();
            runOnUiThread(new Runnable() {
                public void run() {
                    // pb.<span id="IL_AD12" class="IL_AD">dismiss</span>(); // if you want close it..
                }
            });

        } catch (final MalformedURLException e) {
            showError("Error : MalformedURLException " + e);
            e.printStackTrace();
        } catch (final IOException e) {
            showError("Error : IOException " + e);
            e.printStackTrace();
        } catch (final Exception e) {
            showError("Error : Please check your internet connection</span> " + e);
        }
    }

    void downloadFileVideo(String Filename) {

        try {
            URL url = new URL(download_file_video + Filename + ".mp4");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);

            //connect
            urlConnection.connect();

            //set the path where we want to save the file
            File SDCardRoot = (File) Environment.getRootDirectory();

            //create a new file, to save the  file
            File file = new File(SDCardRoot, Filename + ".mp4");

            //FileOutputStream fileOutput = new FileOutputStream(file);
            FileOutputStream fos = openFileOutput(Filename + ".mp4", Context.MODE_PRIVATE);
            //Stream used for reading the data from the internet
            InputStream inputStream = urlConnection.getInputStream();

            //this is the total size of the file which we are downloading
            totalSize = urlConnection.getContentLength();

            runOnUiThread(new Runnable() {
                public void run() {
                    pb.setMax(totalSize);
                }
            });

            //create a buffer...
            byte[] buffer = new byte[1024];
            int bufferLength = 0;

            while ((bufferLength = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, bufferLength);
                downloadedSize += bufferLength;
                // update the progressbar //
                runOnUiThread(new Runnable() {
                    public void run() {
                        pb.setProgress(downloadedSize);
                        float per = ((float) downloadedSize / totalSize) * 100;
                        cur_val.setText("Downloaded " + downloadedSize + "KB / " + totalSize + "KB (" + (int) per + "%)");
                    }
                });
            }
            //close the output stream when complete //
            fos.close();
            runOnUiThread(new Runnable() {
                public void run() {
                    // pb.<span id="IL_AD12" class="IL_AD">dismiss</span>(); // if you want close it..
                }
            });

        } catch (final MalformedURLException e) {
            showError("Error : MalformedURLException " + e);
            e.printStackTrace();
        } catch (final IOException e) {
            showError("Error : IOException " + e);
            e.printStackTrace();
        } catch (final Exception e) {
            showError("Error : Please check your internet connection</span> " + e);
        }
    }

    void showError(final String err) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, err, Toast.LENGTH_LONG).show();
            }
        });
    }

    void showProgress(String file_path) {
        dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.myprogressdialog);
        dialog.setTitle("Download Progress");

        TextView text = (TextView) dialog.findViewById(R.id.tv1);
        text.setText("Downloading file from ... " + file_path);
        cur_val = (TextView) dialog.findViewById(R.id.cur_pg_tv);
        cur_val.setText("Starting download...");
        dialog.show();

        pb = (ProgressBar) dialog.findViewById(R.id.progress_bar);
        pb.setProgress(0);
        //  pb.setProgressDrawable(getResources().getDrawable(R.drawable.green_progress));
    }

    @Override
    public void onLocationChanged(Location location) {
        //Coordinates to upload
        String latitude = String.valueOf(location.getLatitude());
        String longitude = String.valueOf(location.getAltitude());
        String speed = String.valueOf(location.getSpeed());
        //Location Update
        new Control().execute(latitude.substring(0, 4), longitude.substring(0, 4), vehicleno);

        //Speed
        speedView.setText(speed);

        //Choosing the Banner and Displaying
        new Control().execute("1", latitude, longitude);
        bannerlist = control.getbannerl();
        for (int x = 0; x < bannerlist.size(); x++) {
            bannerid = bannerlist.get(x);
            openImage(bannerid);
            control.addcount(bannerid);
            try {
                wait(2000);
            } catch (Exception e) {
                Log.e("Fail to Change Image", e.toString());
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void openImage(String bannerid) {
        File imgFile = new File(Environment.getExternalStorageDirectory() + "/OnlineTaxi/Banners/" + bannerid + ".jpg");
        //File imgFile = new File(Environment.getExternalStorageDirectory() + "/OnlineTaxi/Banners/0001.jpg");
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            banner.setImageBitmap(myBitmap);
            Toast.makeText(getApplicationContext(), bannerid,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void startFare() {
        total_dectence = 0;
        //  custom_wait_Handler.postDelayed(restartTimerThread, 0);
        startTime = SystemClock.uptimeMillis();
        //  startTime = SystemClock.uptimeMillis();
        custom_wait_Handler.postDelayed(updateTimerThread, 0);
        total_fair = 0;
        timeSwapBuff = 0;
    }

    public void stopFare() {
        Toast.makeText(getApplicationContext(), +total_dectence + "", Toast.LENGTH_SHORT)
                .show();
        total_dectence = 0;
        timeSwapBuff += timeInMilliseconds;
        custom_wait_Handler.removeCallbacks(updateTimerThread);
        // timeSwapBuff += timeInMilliseconds;
        timeSwapBuff = 0;
        custom_wait_Handler.removeCallbacks(updateTimerThread);
        //    custom_wait_Handler.removeCallbacks( restartTimerThread);
        //stop=true;
    }

    private Runnable updateTimerThread = new Runnable() {

        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);
            waitingView.setText("" + mins + ":" + String.format("%02d", secs) + ":" + String.format("%03d", milliseconds));
            custom_wait_Handler.postDelayed(this, 0);
        }


    };

}
