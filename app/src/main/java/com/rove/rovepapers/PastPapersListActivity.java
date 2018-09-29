package com.rove.rovepapers;

import android.Manifest;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.rove.rovepapers.Adaptors.PaperListAdaptor;
import com.rove.rovepapers.Misc.Common_Util;
import com.rove.rovepapers.Misc.Permission_Util;
import com.rove.rovepapers.Objects.FilterDataObject;
import com.rove.rovepapers.Objects.PaperObject;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import spencerstudios.com.bungeelib.Bungee;

public class PastPapersListActivity extends AppCompatActivity implements View.OnClickListener {

    private RecyclerView rv;
    private ArrayList<PaperObject> paperObjectsForAdaptor = new ArrayList<>();
    private ArrayList<PaperObject> paperObjectsAll = new ArrayList<>();
    private CollectionReference cr;
    private String subjectSelected;
    private String courseSelected;
    private PaperObject paperObject;
    private PaperListAdaptor rvAdaptor;
    private ImageView filterButton;
    private Common_Util cu = new Common_Util();
    private EditText searchField;
    private Button singlePaper;
    private Button multiViewButton;
    private long animationTime = 1000;
    private boolean multiViewActivated;
    private TextView subjectSelectedTextView;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_papers_list);
        GetExternalStoragePermission();
        MakePath();
        multiViewActivated=false;
        singlePaper = findViewById(R.id.singlepaper_button);
        multiViewButton = findViewById(R.id.multiview_button);
        singlePaper.setOnClickListener(this);
        multiViewButton.setOnClickListener(this);
        filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(this);
        rv = findViewById(R.id.rv_paperList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        subjectSelected = getIntent().getStringExtra("SubjectSelected");
        courseSelected = getIntent().getStringExtra("course");
        cr = FirebaseFirestore.getInstance().collection(courseSelected).document("Subjects").collection(subjectSelected);
        searchField = findViewById(R.id.searchfield);
        subjectSelectedTextView=findViewById(R.id.subject_selected);
        subjectSelectedTextView.setText(subjectSelected);
        getPapersFromCache();
        setFilterListener();
        addSearchListener();
        initializeMultiView();
    }

    @Override
    protected void onResume() {
        super.onResume();
if(rvAdaptor!=null) {
    rvAdaptor.multiViewActivated = multiViewActivated;
    rvAdaptor.multiViewPaperSelectedAlready = false;
}
}

    private void initializeMultiView() {
        RelativeLayout rl_MultiView = findViewById(R.id.rl_multiView);
        rl_MultiView.animate().translationX(1000);
        rl_MultiView.setAlpha(0);
    }

    private void setMultiViewListener() {
        rvAdaptor.multiViewListener = new MultiViewListener() {
            @Override
            public void GetSelectedPaperObject(PaperObject pp) {
                if (pp != null) {
                    activateMultiview(pp);
                } else {
                    DeActivateMultiView();
                }
            }
        };
    }

    private void addSearchListener() {
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = searchField.getText().toString().toLowerCase((Locale.getDefault()));
                filterDataBySearch(text);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }





    String TAG = "PastPapers";

    private void filterDataBySearch(String text ){


        if (text != null && text.length() > 0) {

             text = text.toUpperCase().trim();

            paperObjectsForAdaptor.clear();

            String[] firstsplit = text.split("\\s+");

            //Logs the total number of words.Including "Paper and Scheme" and I think mayber spaces too. Not sure but who cares?
            Log.i(TAG, Integer.toString(firstsplit.length));

            //Going to run through each word and check if paperObject contains it. Rest is self explanatory
            for (int x = 0; x < paperObjectsAll.size(); x++) {
                //Total words in the sentence excluding spaces and the word "PAPER" and "SCHEME" as they are follow ups to other words and hence are useless
                int count = 0;
                //The number of words that are found in paperObject
                int found = 0;

                for (String value : firstsplit) {
                    if (!value.trim().isEmpty()) {
                        if (!value.trim().equals("PAPER") || !value.trim().equals("SCHEME")) {
                            count++;
                        }
                        Boolean contains = itContains(value, paperObjectsAll.get(x));
                        if (contains) {
                            found++;
                        }

                    }

                }
                //If all words are found that means this specific paperObject is relevant to the user and hence add to the filteredList
                if (found == count) {
                    paperObjectsForAdaptor.add(paperObjectsAll.get(x));

                }

            }


            rvAdaptor.notifyDataSetChanged();
        } else {
            paperObjectsForAdaptor.addAll(paperObjectsAll);


            rvAdaptor.notifyDataSetChanged();
        }


    }

    //Method to check if the paperObject contains a specific word
    public Boolean itContains(String value, PaperObject paperObject) {
        try {
            if (paperObject.getVariant().toUpperCase().contains(value)
                    || paperObject.getYear().toUpperCase().contains(value)
                    || paperObject.getType().toUpperCase().contains(value)
                    || paperObject.getMonth().toUpperCase().contains(value)
                    ) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }




//    private void filterDataBySearch(String searchQuery) {
//        paperObjectsForAdaptor.clear();
//        if (searchQuery.length() == 0) {
//            paperObjectsForAdaptor.addAll(paperObjectsAll);
//            rvAdaptor.notifyDataSetChanged();
//        } else {
//            for (int i = 0; i < paperObjectsAll.size(); i++) {
//                boolean year, month, type, variant;
//                List<String> searchQueryArray = new LinkedList<String>(Arrays.asList(searchQuery.split("\\s+(?!paper)(?!scheme)")));
//                //it converts variant, any number (both sotred in different positions in array)
//                //to variant any number(stored in same postion of array)
//                for (int j = 0; j < searchQueryArray.size(); j++) {
//                    if (searchQueryArray.get(j).equals("variant") && searchQueryArray.size() > (j + 1)) {
//                        searchQueryArray.set(j, searchQueryArray.get(j) + " " + searchQueryArray.get(j + 1));
//                        searchQueryArray.remove(j + 1);
//                    }
//                }
//                // (?!\d)
//
//                String stringifiedPaperObject = paperObjectsAll.get(i).getYear() + ";" + paperObjectsAll.get(i).getMonth() + ";" + "variant " + paperObjectsAll.get(i).getVariant() + ";" + paperObjectsAll.get(i).getType() + ";";
//                Log.i("DATA", searchQueryArray.toString());
//                if (doesContains(stringifiedPaperObject.toLowerCase(), searchQueryArray)) {
//                    paperObjectsForAdaptor.add(paperObjectsAll.get(i));
//                }
//            }
//        }
//        //setAdaptor();
//        rvAdaptor.notifyDataSetChanged();
//    }

//    private boolean doesContains(String data, List<String> searchQueryArray) {
//        boolean returnVal = true;
//        for (int i = 0; i < searchQueryArray.size(); i++) {
//            if (!data.contains(searchQueryArray.get(i))) {
//                returnVal = false;
//            }
//        }
//        return returnVal;
//    }

    private void getPapersFromCache() {
        ArrayList<PaperObject> templist = new ArrayList<>();
        templist = (ArrayList<PaperObject>) cu.getUserDataCustomObjectArrayList(this, courseSelected + subjectSelected);
        if (templist == null) {
            getPapersFirebase();
        } else if (templist.isEmpty()) {
            getPapersFirebase();
        } else {
            Log.i("IFELSE", String.valueOf(templist.size()));
            paperObjectsForAdaptor.addAll(templist);
            paperObjectsAll.addAll(templist);
            setAdaptor();
        }
    }

    private void setFilterListener() {
        PaperFilter.applyListener = new ApplyListener() {
            @Override
            public void getFilterObject(FilterDataObject filterDataObject) {
                filterData(filterDataObject);
            }
        };
    }

    private void filterData(FilterDataObject filterDataObject) {
        paperObjectsForAdaptor.clear();
        boolean filterByRange = false;
        if (filterDataObject.getYearTypeSelected().equals("specific year")) {
            filterByRange = false;
        } else {
            filterByRange = true;
        }
        for (int i = 0; i < paperObjectsAll.size(); i++) {
            boolean yearFound = true;
            boolean monthFound = true;
            boolean typeFound = true;
            if (!filterDataObject.getYearFrom().equals("")) {
                yearFound = setYear(filterDataObject, i, filterByRange);
            }
            if (!filterDataObject.getMonthSelected().equals("all")) {
                monthFound = setMonth(filterDataObject, i);
            }
            if (!filterDataObject.getTypeSelected().equals("all")) {
                typeFound = setType(filterDataObject, i);
            }
            if (yearFound && monthFound && typeFound) {
                paperObjectsForAdaptor.add(paperObjectsAll.get(i));
            }
            Log.i("yearFound", String.valueOf(yearFound));
            Log.i("monthFound", String.valueOf(monthFound));
            Log.i("typeFound", String.valueOf(typeFound));
        }
        rvAdaptor.notifyDataSetChanged();
    }

    private boolean setYear(FilterDataObject filterDataObject, int counter, boolean filterByRange) {
        if (!filterByRange) {
            if (filterDataObject.getYearFrom().equals(paperObjectsAll.get(counter).getYear())) {
                return true;
            } else {
                return false;
            }
        } else {
            boolean returnVal = false;
            for (int i = Integer.parseInt(filterDataObject.getYearFrom()); i <= Integer.parseInt(filterDataObject.getYearTo()); i++) {
                String tempYear = String.valueOf(i);
                if (tempYear.equals(paperObjectsAll.get(counter).getYear())) {
                    returnVal = true;
                }
            }
            return returnVal;
        }
    }


    private boolean setMonth(FilterDataObject filterDataObject, int counter) {
        if (filterDataObject.getMonthSelected().equals(paperObjectsAll.get(counter).getMonth())) {
            // Log.i("RETUIRN VALUE","TRUE");
            return true;
        } else {
            //  Log.i("RETUIRN VALUE","False");
            return false;
        }
    }


    private boolean setType(FilterDataObject filterDataObject, int counter) {
        if (filterDataObject.getTypeSelected().equals(paperObjectsAll.get(counter).getType())) {
            return true;
        } else {
            return false;
        }
    }


    private void MakePath() {
        File path = new File(Environment.getExternalStorageDirectory() + "/RovePapers/");
        if (!path.exists())
            path.mkdirs();

    }


    private void GetExternalStoragePermission() {
        Permission_Util permission_util = new Permission_Util();
        String[] permissions = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        permission_util.getPermissions(this, permissions);
    }

    private void setAdaptor() {
        rvAdaptor = new PaperListAdaptor(paperObjectsForAdaptor, this, subjectSelected, courseSelected);
        rv.setAdapter(rvAdaptor);
        setMultiViewListener();
    }

    private void getPapersFirebase() {
        Log.i("IFELSE", "GETTING FROM FIREBASE JAANI");
        cr.orderBy("year").orderBy("month").orderBy("type", Query.Direction.DESCENDING).orderBy("variant").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    paperObject = new PaperObject();
                    if (validateData(document)) {
                        paperObject.setDocumentID(document.getId());
                        paperObject.setMonth((String) document.get("month"));
                        paperObject.setType((String) document.get("type"));
                        if (document.get("variant") == null || ((String) document.get("variant")).contains("+")) {
                            paperObject.setVariant("All");
                        } else {
                            paperObject.setVariant((String) document.get("variant"));
                        }
                        paperObject.setYear((String) document.get("year"));
                        paperObject.setOriginalName((String) document.get("name"));
                        if (paperObject.getType().equals("Question Paper")) {
                            paperObject.setMcqAnswers((ArrayList<String>) document.get("mcqAnswers"));
                        }
                        paperObjectsForAdaptor.add(paperObject);
                        paperObjectsAll.add(paperObject);
                    }
                }
                setAdaptor();
                cu.saveUserDataCustomObjectArrayList(PastPapersListActivity.this, paperObjectsAll, courseSelected + subjectSelected);
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i("ERROR", e.toString());
                        //TODO HANDLE THIS
                    }
                });
    }

    private boolean validateData(DocumentSnapshot document) {
        boolean returnVal = true;
        if (document.get("month") == null || document.get("month").equals("error")) {
            returnVal = false;
        }
        if (document.get("type") == null || document.get("type").equals("error")) {
            returnVal = false;
        }
      /*  if(document.get("variant")==null || document.get("variant").equals("error"){
            returnVal=false;
        }*/
        if (document.get("year") == null || document.get("year").equals("error")) {
            returnVal = false;
        }
        return returnVal;
    }


    private void activateMultiview(PaperObject pp) {
        RelativeLayout rl_multiView = findViewById(R.id.rl_multiView);
        TextView year_multiView = findViewById(R.id.year_textView_multiView);
        TextView month_multiView = findViewById(R.id.month_textView_multiView);
        TextView type_multiView = findViewById(R.id.type_textView_multiView);
        TextView variant_multiView = findViewById(R.id.variant_textView_multiView);
        TextView rating = findViewById(R.id.rating_textview_multiView);

        rl_multiView.setVisibility(View.VISIBLE);

        rl_multiView.animate().translationX(0).alpha(1).setDuration(animationTime);

        year_multiView.setText(pp.getYear());
        type_multiView.setText(pp.getType());
        month_multiView.setText(pp.getMonth());
        variant_multiView.setText("Variant " + pp.getVariant());
    }


    private void DeActivateMultiView() {
        RelativeLayout rl_multiView = findViewById(R.id.rl_multiView);
        rl_multiView.animate().translationX(1000).alpha(0).setDuration(animationTime);

    }


    @Override
    public void onBackPressed() {
        if (rvAdaptor != null) {
            if (rvAdaptor.multiViewPaperSelectedAlready) {
                rvAdaptor.multiViewPaperSelectedAlready = false;
                DeActivateMultiView();
            } else {
                super.onBackPressed();
                Bungee.swipeRight(this);
            }
        } else {
            super.onBackPressed();
            Bungee.swipeRight(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.filterButton) {
            startActivity(new Intent(PastPapersListActivity.this, PaperFilter
                    .class));
        } else if (v.getId() == R.id.singlepaper_button) {
            multiViewActivated=false;
            rvAdaptor.multiViewActivated = multiViewActivated;
        }
        else{
            multiViewActivated=true;
            rvAdaptor.multiViewActivated = multiViewActivated;
        }
    }
}
