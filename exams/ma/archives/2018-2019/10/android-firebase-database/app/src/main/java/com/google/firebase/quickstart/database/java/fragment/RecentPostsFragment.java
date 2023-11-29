package com.google.firebase.quickstart.database.java.fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;

public class RecentPostsFragment extends PostListFragment {

  public RecentPostsFragment() {
  }

  @Override
  public Query getQuery(DatabaseReference databaseReference) {
    // Last 100 posts, these are automatically the 100 most recent
    // due to sorting by push() keys
    Query recentPostsQuery = databaseReference.child("posts")
        .limitToFirst(100);

    return recentPostsQuery;
  }
}
