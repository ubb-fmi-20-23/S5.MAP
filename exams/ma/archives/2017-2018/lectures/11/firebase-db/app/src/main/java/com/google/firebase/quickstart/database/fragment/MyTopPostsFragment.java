package com.google.firebase.quickstart.database.fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

public class MyTopPostsFragment extends PostListFragment {

  public MyTopPostsFragment() {
  }

  @Override
  public Query getQuery(DatabaseReference databaseReference) {
    // My top posts by number of stars
    String myUserId = getUid();

    return databaseReference.child("user-posts").child(myUserId)
        .orderByChild("starCount");
  }
}
