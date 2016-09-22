package com.example.xyzreader.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_CURRENT_POS = "extra_current_position";
    public static final String EXTRA_PREV_POS = "extra_previous_position";
    public static final String THUMBNAIL_TRANSITION = "thumbnailView";

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private int mCurrentPosition;
    private int mPreviousPosition;
    boolean mReentering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);


        final View toolbarContainerView = findViewById(R.id.appBar_container);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //To take care of the refresh icon so that it won't continually turn.
                if (mRecyclerView.getChildCount() > 0) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }
        });

//        setExitSharedElementCallback(sharedElementCallback);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }

    //Catch the Intent of the supportFinishAfterTransition() with it's results for animation purpose
    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        mReentering = true;
        //Get the intent for the current position and set it to mCurrentPosition
        mCurrentPosition = data.getIntExtra(EXTRA_CURRENT_POS, 0);
        mPreviousPosition = data.getIntExtra(EXTRA_PREV_POS, 0);
        Log.i("ActivityReenter pos", String.valueOf(mCurrentPosition));
        //Check the scroll position and make it be at the current position
        if (mCurrentPosition != mPreviousPosition) {
            mRecyclerView.scrollToPosition(mCurrentPosition);
        }
//        postponeEnterTransition();
//        scheduleStartPostponedTransition();
    }
//
//    //Get ViewTreeObserver to see when fragment view is ready for animation
//    //Make sure to call this method in the fragment, onLoadFinished or the app just hangs
//    public void scheduleStartPostponedTransition() {
//        mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
//                mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
//                startPostponedEnterTransition();
//                return true;
//            }
//        });
//    }
//
//    //Using this method for share element transition
//    SharedElementCallback sharedElementCallback = new SharedElementCallback() {
//        @Override
//        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
//            super.onMapSharedElements(names, sharedElements);
//            if (mReentering) {
//                String newTransitionName = THUMBNAIL_TRANSITION + mCurrentPosition;
////                String oldTransitionName = THUMBNAIL_TRANSITION + mPreviousPosition;
////                Log.i("oldTransitionName", oldTransitionName);
//                Log.i("newTransitionName", newTransitionName);
////            View newSharedView = mRecyclerView.findViewWithTag(newTransitionName);
////            if(newSharedView !=null){
//                RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(mCurrentPosition);
//
//                View view = holder.itemView.findViewById(R.id.thumbnail);
////                View view = mRecyclerView.findViewById(R.id.thumbnail);
////                view.setTransitionName(getString(R.string.transition_photo));
//                names.clear();
//                names.add(newTransitionName);
//                sharedElements.clear();
//                sharedElements.put(newTransitionName, view);
////            }
//                mReentering = false;
//            }
//        }
//    };
//
    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Build.VERSION.SDK_INT >= 21) {
                        //The imageView is found for this view, which is the shared element item
                        ImageView sharedView = (ImageView) view.findViewById(R.id.thumbnail);

                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                        Log.i("Shared name", sharedView.getTransitionName());
                        Log.i("Intent position", String.valueOf(vh.getAdapterPosition()));
                        //For scene transition animation
                        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(ArticleListActivity.this,
                                sharedView, sharedView.getTransitionName());

                        //Start activity with the intent and the options bundle
                        startActivity(intent, options.toBundle());
                    } else {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                    }
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            //Setting the transition name for the shared element transition and adding
            //the position to make the transition name unique.
            holder.thumbnailView.setTransitionName(getString(R.string.transition_photo) + position);
            Log.i("From ListActivity", getString(R.string.transition_photo) + position);
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
/**From Advanced android class video but not working**/
//            // This enables better animations, even if we lose state due to a device rotation,
//            // the animator can use this to re-find the original view
//            String transitionName = THUMBNAIL_TRANSITION + position;
//            ViewCompat.setTransitionName(holder.thumbnailView, transitionName);
//            holder.thumbnailView.setTag(transitionName);
//            Log.i("From ListActivity", transitionName);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}
