package tourguide.tourguide;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;


/**
 * Created by tanjunrong on 2/10/15.
 */
public class TourGuide {
    /**
     * This describes the animation techniques
     * */
    public enum Technique {
        Click, HorizontalLeft, HorizontalRight, VerticalUpward, VerticalDownward
    }

    /**
     * This describes the allowable motion, for example if you want the users to learn about clicking, but want to stop them from swiping, then use ClickOnly
     */
    public enum MotionType {
        AllowAll, ClickOnly, SwipeOnly
    }
    private Technique mTechnique;
    private View mHighlightedView;
//    private int mOverlayBackgroundColor = Color.parseColor("#AA000000");
//    private TourGuide.Overlay mOverlayStyle = Overlay.Circle;
//    private boolean mDisableClick = false;
    private Activity mActivity;
    private MotionType mMotionType;
    private FrameLayoutWithHole mFrameLayout;
    private View mToolTipViewGroup;
    private ToolTip mToolTip;
    private Pointer mPointer;
    private Overlay mOverlay;

    /******
     *
     * Public API
     *
     *******/

    /* Static builder */
    public static TourGuide init(Activity activity){
        return new TourGuide(activity);
    }

    /* Constructor */
    public TourGuide(Activity activity){
        mActivity = activity;
    }

    /**
     * Setter for the animation to be used
     * @param technique Animation to be used
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide with(Technique technique){
        mTechnique = technique;
        return this;
    }

    /**
     * Sets which motion type is motionType
     * @param motionType
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide motionType(MotionType motionType){
        mMotionType = motionType;
        return this;
    }

    /**
     * Sets the duration
     * @param view the view in which the tutorial button will be placed on top of
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide playOn(View view){
        mHighlightedView = view;
        setupView();
        return this;
    }

    public TourGuide setOverlay(Overlay overlay){
        mOverlay = overlay;
        return this;
    }

    /**
     * Set the toolTip
     * @param toolTip
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide setToolTip(ToolTip toolTip){
        mToolTip = toolTip;
        return this;
    }
    /**
     * Set the Pointer
     * @param pointer
     * @return return AnimateTutorial instance for chaining purpose
     */
    public TourGuide setPointer(Pointer pointer){
        mPointer = pointer;
        return this;
    }
    /**
     * Clean up the tutorial that is added to the activity
     */
    public void cleanUp(){
        if (mFrameLayout.getParent()!=null){
            ((ViewGroup)mFrameLayout.getParent()).removeView(mFrameLayout);
            ((ViewGroup) mActivity.getWindow().getDecorView()).removeView(mToolTipViewGroup);
        }
    }

    /******
     *
     * Private methods
     *
     *******/
    //TODO: move into Pointer
    private int getXBasedOnGravity(int width){
        int [] pos = new int[2];
        mHighlightedView.getLocationOnScreen(pos);
        int x = pos[0];
        if((mPointer.mGravity & Gravity.RIGHT) == Gravity.RIGHT){
            return x+mHighlightedView.getWidth()-width;
        } else if ((mPointer.mGravity & Gravity.LEFT) == Gravity.LEFT) {
            return x;
        } else { // this is center
            return x+mHighlightedView.getWidth()/2-width/2;
        }
    }
    //TODO: move into Pointer
    private int getYBasedOnGravity(int height){
        int [] pos = new int[2];
        mHighlightedView.getLocationInWindow(pos);
        int y = pos[1];
        Log.d("ddw-l","fab height: "+height);
        Log.d("ddw-l","mHighlightedView height: "+mHighlightedView.getHeight());
        Log.d("ddw-l","mHighlightedView.getLocationInWindow(): "+y);
        Log.d("ddw-l","mHighlightedView.getY(): "+mHighlightedView.getY());
        if((mPointer.mGravity & Gravity.BOTTOM) == Gravity.BOTTOM){
            return y+mHighlightedView.getHeight()-height;
        } else if ((mPointer.mGravity & Gravity.TOP) == Gravity.TOP) {
            return y;
        }else { // this is center
            return y+mHighlightedView.getHeight()/2-height/2;
        }
    }
//    final int description_enter_animation_duration = 1000;

    private void setupView(){
//        TODO: throw exception if either mActivity, mDuration, mHighlightedView is null
        checking();
        final ViewTreeObserver viewTreeObserver = mHighlightedView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // make sure this only run once
                mHighlightedView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Log.d("ddw", "HighlightedView.getHeight(): " + mHighlightedView.getHeight());
                Log.d("ddw", "HighlightedView.getWidth(): " + mHighlightedView.getWidth());

                /* Initialize a frame layout with a hole */
                mFrameLayout = new FrameLayoutWithHole(mActivity, mHighlightedView, mMotionType, mOverlay);

                /* handle click disable */
                handleDisableClicking(mFrameLayout);

                /* setup floating action button */
                if (mPointer!=null) {
                    FloatingActionButton fab = setupAndAddFABToFrameLayout(mFrameLayout);
                    performAnimationOn(fab);
                }
                setupFrameLayout();
                /* setup tooltip view */
                setupToolTip(mFrameLayout);
            }
        });
    }
    private void checking(){
        // There is not check for tooltip because tooltip can be null, it means there no tooltip will be shown

    }
    private void handleDisableClicking(FrameLayoutWithHole frameLayoutWithHole){
        if (mOverlay != null && mOverlay.mDisableClick) {
            frameLayoutWithHole.setViewHole(mHighlightedView);
            frameLayoutWithHole.setSoundEffectsEnabled(false);
            frameLayoutWithHole.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d("ddw", "disable, do nothing");
                }
            });
        }
    }
    private void setupToolTip(FrameLayoutWithHole frameLayoutWithHole){
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
//        layoutParams.setGravity = Gravity.BOTTOM;

        if (mToolTip != null) {
            LayoutInflater layoutInflater = mActivity.getLayoutInflater();
            mToolTipViewGroup = layoutInflater.inflate(R.layout.tooltip, null);

            View toolTipContainer = mToolTipViewGroup.findViewById(R.id.toolTip_container);
            TextView toolTipTitleTV = (TextView) mToolTipViewGroup.findViewById(R.id.title);
            TextView toolTipDescriptionTV = (TextView) mToolTipViewGroup.findViewById(R.id.description);

            toolTipContainer.setBackgroundColor(mToolTip.mBackgroundColor);
            toolTipTitleTV.setText(mToolTip.mTitle);
            toolTipDescriptionTV.setText(mToolTip.mDescription);

            mToolTipViewGroup.startAnimation(mToolTip.mEnterAnimation);

            // measure size of image to be placed
            mToolTipViewGroup.measure(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            int width = mToolTipViewGroup.getMeasuredWidth();
            int height = mToolTipViewGroup.getMeasuredHeight();
            Point point = getXYForToolTip(mToolTip.mGravity, width, height);
            layoutParams.setMargins(point.x, point.y, 0, 0);
            /* add setShadow if it's turned on */
            if (mToolTip.mShadow) {
                mToolTipViewGroup.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.drop_shadow));
            }
//            ((ViewGroup) mActivity.getWindow().getDecorView().findViewById(android.R.id.content)).addView(mToolTipViewGroup, layoutParams);
            ((ViewGroup) mActivity.getWindow().getDecorView()).addView(mToolTipViewGroup, layoutParams);
        }

    }
    private Point getXYForToolTip(int gravity, int width, int height) {
        Point point = new Point();
        int [] pos = new int[2];
        mHighlightedView.getLocationOnScreen(pos);
        int x = pos[0];
        int y = pos[1];

        float density = mActivity.getResources().getDisplayMetrics().density;
        float adjustment = 10 * density;
        // x calculation
        if ((gravity & Gravity.LEFT) == Gravity.LEFT){
            point.x = x - width + (int)adjustment;
        } else if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) {
            point.x = x + mHighlightedView.getWidth() - (int)adjustment;
        } else {
            point.x = x + mHighlightedView.getWidth() / 2 - width / 2;
        }

        // y calculation
        if ((gravity & Gravity.TOP) == Gravity.TOP) {

            if (((gravity & Gravity.LEFT) == Gravity.LEFT) || ((gravity & Gravity.RIGHT) == Gravity.RIGHT)) {
                point.y =  y - height + (int)adjustment;
            } else {
                point.y =  y - height - (int)adjustment;
            }
        } else { // this is center
            Log.d("ddw","gravity: "+gravity);
            if (((gravity & Gravity.LEFT) == Gravity.LEFT) || ((gravity & Gravity.RIGHT) == Gravity.RIGHT)) {
                point.y =  y + mHighlightedView.getHeight() - (int) adjustment;
            } else {
                point.y =  y + mHighlightedView.getHeight() + (int) adjustment;
            }
        }
        return point;
    }

    private FloatingActionButton setupAndAddFABToFrameLayout(FrameLayoutWithHole frameLayoutWithHole){
        FloatingActionButton fab = new FloatingActionButton(mActivity);
        fab.setSize(FloatingActionButton.SIZE_MINI);
//        fab.setColorNormalResId(R.color.LightBlue);
        fab.setColorNormal(mPointer.mColor);
//        fab.setIcon(R.drawable.ic_fab_star);
        fab.setStrokeVisible(false);

        fab.setClickable(false);
        // measure size of image to be placed
        fab.measure(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

        float mDensity = mActivity.getResources().getDisplayMetrics().density;
        int size = (int)(50 * mDensity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(getXBasedOnGravity(size), getYBasedOnGravity(size), 0, 0);

        fab.setLayoutParams(params);
        fab.getLayoutParams().height = size;
        fab.getLayoutParams().width = size;

        frameLayoutWithHole.addView(fab, params);

        return fab;
    }

    private void setupFrameLayout(){
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        ViewGroup contentArea = (ViewGroup) mActivity.getWindow().getDecorView().findViewById(android.R.id.content);
        int [] pos = new int[2];
        contentArea.getLocationOnScreen(pos);
        // frameLayoutWithHole's coordinates are calculated taking full screen height into account
        // but we're adding it to the content area only, so we need to offset it to the same Y value of contentArea
        layoutParams.setMargins(0,-pos[1],0,0);

        ((ViewGroup) mActivity.getWindow().getDecorView().findViewById(android.R.id.content)).addView(mFrameLayout, layoutParams);
    }

    private void performAnimationOn(final View view){
        AnimationSet animSet = new AnimationSet(true);

        if (mTechnique != null && mTechnique == Technique.HorizontalLeft){

            final AnimatorSet animatorSet = new AnimatorSet();
            final AnimatorSet animatorSet2 = new AnimatorSet();
            Animator.AnimatorListener lis1 = new Animator.AnimatorListener() {
                @Override public void onAnimationStart(Animator animator) {}
                @Override public void onAnimationCancel(Animator animator) {}
                @Override public void onAnimationRepeat(Animator animator) {}
                @Override
                public void onAnimationEnd(Animator animator) {
                    view.setScaleX(1f);
                    view.setScaleY(1f);
                    view.setTranslationX(0);
                    animatorSet2.start();
                }
            };
            Animator.AnimatorListener lis2 = new Animator.AnimatorListener() {
                @Override public void onAnimationStart(Animator animator) {}
                @Override public void onAnimationCancel(Animator animator) {}
                @Override public void onAnimationRepeat(Animator animator) {}
                @Override
                public void onAnimationEnd(Animator animator) {
                    view.setScaleX(1f);
                    view.setScaleY(1f);
                    view.setTranslationX(0);
                    animatorSet.start();
                }
            };

            long fadeInDuration = 800;
            long scaleDownDuration = 800;
            long goLeftXDuration = 2000;
            long fadeOutDuration = goLeftXDuration;
            float translationX = getScreenWidth()/2;

            final ValueAnimator fadeInAnim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            fadeInAnim.setDuration(fadeInDuration);
            final ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.85f);
            scaleDownX.setDuration(scaleDownDuration);
            final ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.85f);
            scaleDownY.setDuration(scaleDownDuration);
            final ObjectAnimator goLeftX = ObjectAnimator.ofFloat(view, "translationX", -translationX);
            goLeftX.setDuration(goLeftXDuration);
            final ValueAnimator fadeOutAnim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
            fadeOutAnim.setDuration(fadeOutDuration);

            final ValueAnimator fadeInAnim2 = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            fadeInAnim2.setDuration(fadeInDuration);
            final ObjectAnimator scaleDownX2 = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.85f);
            scaleDownX2.setDuration(scaleDownDuration);
            final ObjectAnimator scaleDownY2 = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.85f);
            scaleDownY2.setDuration(scaleDownDuration);
            final ObjectAnimator goLeftX2 = ObjectAnimator.ofFloat(view, "translationX", -translationX);
            goLeftX2.setDuration(goLeftXDuration);
            final ValueAnimator fadeOutAnim2 = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
            fadeOutAnim2.setDuration(fadeOutDuration);

            animatorSet.play(fadeInAnim);
            animatorSet.play(scaleDownX).with(scaleDownY).after(fadeInAnim);
            animatorSet.play(goLeftX).with(fadeOutAnim).after(scaleDownY);

            animatorSet2.play(fadeInAnim2);
            animatorSet2.play(scaleDownX2).with(scaleDownY2).after(fadeInAnim2);
            animatorSet2.play(goLeftX2).with(fadeOutAnim2).after(scaleDownY2);

            animatorSet.addListener(lis1);
            animatorSet2.addListener(lis2);
            animatorSet.start();

        } else if (mTechnique != null && mTechnique == Technique.HorizontalRight){

        } else if (mTechnique != null && mTechnique == Technique.VerticalUpward){

        } else if (mTechnique != null && mTechnique == Technique.VerticalDownward){

        } else { // do click for default case
            final AnimatorSet animatorSet = new AnimatorSet();
            final AnimatorSet animatorSet2 = new AnimatorSet();
            Animator.AnimatorListener lis1 = new Animator.AnimatorListener() {
                @Override public void onAnimationStart(Animator animator) {}
                @Override public void onAnimationCancel(Animator animator) {}
                @Override public void onAnimationRepeat(Animator animator) {}
                @Override
                public void onAnimationEnd(Animator animator) {
                    view.setScaleX(1f);
                    view.setScaleY(1f);
                    view.setTranslationX(0);
                    animatorSet2.start();
                }
            };
            Animator.AnimatorListener lis2 = new Animator.AnimatorListener() {
                @Override public void onAnimationStart(Animator animator) {}
                @Override public void onAnimationCancel(Animator animator) {}
                @Override public void onAnimationRepeat(Animator animator) {}
                @Override
                public void onAnimationEnd(Animator animator) {
                    view.setScaleX(1f);
                    view.setScaleY(1f);
                    view.setTranslationX(0);
                    animatorSet.start();
                }
            };

            long fadeInDuration = 800;
            long scaleDownDuration = 800;
            long fadeOutDuration = 800;
            long delay = 1000;

            final ValueAnimator delayAnim = ObjectAnimator.ofFloat(view, "translationX", 0);
            delayAnim.setDuration(delay);
            final ValueAnimator fadeInAnim = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            fadeInAnim.setDuration(fadeInDuration);
            final ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.85f);
            scaleDownX.setDuration(scaleDownDuration);
            final ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.85f);
            scaleDownY.setDuration(scaleDownDuration);
            final ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.85f, 1f);
            scaleUpX.setDuration(scaleDownDuration);
            final ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.85f, 1f);
            scaleUpY.setDuration(scaleDownDuration);
            final ValueAnimator fadeOutAnim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
            fadeOutAnim.setDuration(fadeOutDuration);

            final ValueAnimator delayAnim2 = ObjectAnimator.ofFloat(view, "translationX", 0);
            delayAnim2.setDuration(delay);
            final ValueAnimator fadeInAnim2 = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            fadeInAnim2.setDuration(fadeInDuration);
            final ObjectAnimator scaleDownX2 = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.85f);
            scaleDownX2.setDuration(scaleDownDuration);
            final ObjectAnimator scaleDownY2 = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.85f);
            scaleDownY2.setDuration(scaleDownDuration);
            final ObjectAnimator scaleUpX2 = ObjectAnimator.ofFloat(view, "scaleX", 0.85f, 1f);
            scaleUpX2.setDuration(scaleDownDuration);
            final ObjectAnimator scaleUpY2 = ObjectAnimator.ofFloat(view, "scaleY", 0.85f, 1f);
            scaleUpY2.setDuration(scaleDownDuration);
            final ValueAnimator fadeOutAnim2 = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
            fadeOutAnim2.setDuration(fadeOutDuration);
            view.setAlpha(0);
            animatorSet.setStartDelay(mToolTip != null ? mToolTip.mEnterAnimation.getDuration() : 0);
            animatorSet.play(fadeInAnim);
            animatorSet.play(scaleDownX).with(scaleDownY).after(fadeInAnim);
            animatorSet.play(scaleUpX).with(scaleUpY).with(fadeOutAnim).after(scaleDownY);
            animatorSet.play(delayAnim).after(scaleUpY);

            animatorSet2.play(fadeInAnim2);
            animatorSet2.play(scaleDownX2).with(scaleDownY2).after(fadeInAnim2);
            animatorSet2.play(scaleUpX2).with(scaleUpY2).with(fadeOutAnim2).after(scaleDownY2);
            animatorSet2.play(delayAnim2).after(scaleUpY2);

            animatorSet.addListener(lis1);
            animatorSet2.addListener(lis2);
            animatorSet.start();
        }
    }
    private int getScreenWidth(){
        if (mActivity!=null) {
            Display display = mActivity.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            return size.x;
        } else {
            return 0;
        }
    }
}
