package com.example.theodhor.speechapplication;



import android.content.ActivityNotFoundException;

import android.content.Intent;

import android.content.SharedPreferences;

import android.os.Build;

import android.provider.AlarmClock;

import android.speech.RecognizerIntent;

import android.speech.tts.TextToSpeech;

import android.speech.tts.UtteranceProgressListener;

import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;

import android.util.Log;

import android.view.View;

import android.widget.ImageButton;

import android.widget.Toast;



import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Date;

import java.util.Locale;

import java.util.Scanner;

import java.io.*;



public class MainActivity extends AppCompatActivity{





    private TextToSpeech tts;

    private ArrayList<String> questions;

    private String name, surname, age, asName;

    private String[] names = new String[118];

    private String[] results = new String[118];

    private String[] tags1 = new String[118];

    private String[] tags2= new String[118];

    private SharedPreferences preferences;

    private SharedPreferences.Editor editor;

    private static final String PREFS = "prefs";

    private static final String NEW = "new";

    private static final String NAME = "name";

    private static final String AGE = "age";

    private static final String AS_NAME = "as_name";



    @Override

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFS,0);

        editor = preferences.edit();







        /*try {

            DataInputStream file = new DataInputStream(getAssets().open("Disease_results.csv"));

            Scanner input = new Scanner(file);

            //PrintStream output = new PrintStream(System.out);

            PrintStream output = new PrintStream(new File("Cleaned_Data.csv"));

            cleanData(input, output);

        } catch (IOException e) {

            e.printStackTrace();

        }*/



        try {

            DataInputStream file = new DataInputStream(getAssets().open("Cleaned_Data.csv"));

            Scanner input = new Scanner(file);

            split(input);

        } catch (IOException e) {

            e.printStackTrace();

        }



        findViewById(R.id.microphoneButton).setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View v) {

                listen();

            }

        });

        loadQuestions();



        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {

            @Override

            public void onInit(int status) {

                if (status == TextToSpeech.SUCCESS) {

                    int result = tts.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {

                        Log.e("TTS", "This Language is not supported");

                    }

                    speak("Hello");



                } else {

                    Log.e("TTS", "Initilization Failed!");

                }

            }

        });



    }



    private void cleanData(Scanner input, PrintStream output) {

        while (input.hasNextLine()) {

            String line = input.nextLine();

            String cleanLine = "";

            cleanLine = line.replaceAll("\'", "");

            cleanLine = cleanLine.replaceAll("-", " ");

            cleanLine = cleanLine.replaceAll("/", " or ");

            if (cleanLine.contains("?")) {

                if (cleanLine.contains("Wolfram")) {

                    cleanLine = cleanLine.replaceAll("\\?", " ");

                } else if (cleanLine.contains("poprotein")) {

                    cleanLine = cleanLine.replaceAll("\\?", "beta ");

                }

            }

            output.println(cleanLine);

        }

    }



    private void split(Scanner input) {

        int index = 0;

        while(input.hasNextLine()) {

            String line = input.nextLine();

            String[] cells = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

            names[index] = cells[0];

            results[index] = cells[3];

            tags1[index] = cells[4];

            tags2[index] = cells[5];

            index++;

        }

    }



    private void loadQuestions(){

        questions = new ArrayList<>();

        questions.clear();

        questions.add("Hello, what is your name?");

        questions.add("What is your surname?");

        questions.add("How old are you?");

        questions.add("That's all I had, thank you ");

    }



    private void listen(){

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");



        try {

            startActivityForResult(i, 100);

        } catch (ActivityNotFoundException a) {

            Toast.makeText(MainActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();

        }

    }



    @Override

    public void onDestroy() {

        if (tts != null) {

            tts.stop();

            tts.shutdown();

        }

        super.onDestroy();

    }



    private void speak(String text){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);



        }else{

            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);

        }

    }



    @Override

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 100){

            if (resultCode == RESULT_OK && null != data) {

                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                String inSpeech = res.get(0);

                recognition(inSpeech);

            }

        }

    }



    private void recognition(String text) {

        Log.e("Speech",""+text);

        String[] speech = text.split(" ");

        if(text.contains("hello")){

            speak(questions.get(0));

        }

        //

        if(text.contains("my name is")){

            name = speech[speech.length-1];

            Log.e("THIS", "" + name);

            editor.putString(NAME,name).apply();

            speak(questions.get(2));

        }

        //This must be the age

        if(text.contains("years") && text.contains("old")){

            String age = speech[speech.length-3];

            Log.e("THIS", "" + age);

            editor.putString(AGE, age).apply();

        }



        if(text.contains("what time is it")){

            SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm");//dd/MM/yyyy

            Date now = new Date();

            String[] strDate = sdfDate.format(now).split(":");

            if(strDate[1].contains("00"))

                strDate[1] = "o'clock";

            speak("The time is " + sdfDate.format(now));



        }



        if(text.contains("wake me up at")){

            speak(speech[speech.length-1]);

            String[] time = speech[speech.length-1].split(":");

            String hour = time[0];

            String minutes = time[1];

            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);

            i.putExtra(AlarmClock.EXTRA_HOUR, Integer.valueOf(hour));

            i.putExtra(AlarmClock.EXTRA_MINUTES, Integer.valueOf(minutes));

            startActivity(i);

            speak("Setting alarm to ring at " + hour + ":" + minutes);

        }



        if(text.contains("thank you")){

            speak("Thank you too " + preferences.getString(NAME, null));

        }



        if(text.contains("how old am I")){

            speak("You are "+preferences.getString(AGE,null)+" years old.");

        }



        if(text.contains("what is your name")){

            String as_name = preferences.getString(AS_NAME,"");

            if(as_name.equals(""))

                speak("How do you want to call me?");

            else

                speak("My name is "+as_name);

        }



        if(text.contains("call you")){

            String name = speech[speech.length-1];

            editor.putString(AS_NAME,name).apply();

            speak("I like it, thank you "+preferences.getString(NAME,null));

        }



        if(text.contains("what is my name")){

            speak("Your name is "+preferences.getString(NAME,null));

        }

        if(text.contains("cancer") || text.contains("do I have a risk for cancer")){
            int cancer = 0;
            for(int i = 0; i < results.length; i++) {
                if (!results[i].equalsIgnoreCase("negative") && names[i].toLowerCase().contains("cancer")) {
                    cancer++;
                    speak("You are a " + results[i] + "of " + names[i]);
                }
                try {
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(cancer == 0){
                speak("You are not a pathogenic carrier of any types of cancer on the list");
            }
        }


        if(text.contains("condition") || text.contains("disease")) {

            String symptom = speech[speech.length-2];

            int disease = 0;

            for(int i = 0; i < tags2.length; i++) {

                if(tags2[i].toLowerCase().contains(symptom)) {

                    if(!results[i].equalsIgnoreCase("negative")) {

                        disease++;

                        speak("You are a " + results[i] + " of " + names[i]);

                        try{

                            Thread.sleep(5000);

                        } catch (InterruptedException e) {

                            e.printStackTrace();

                        }

                    }

                }

            }

            if (disease == 0) {

                speak("You do not have any " + speech[speech.length-2] + speech[speech.length-1]);

            }

        }

    }

}