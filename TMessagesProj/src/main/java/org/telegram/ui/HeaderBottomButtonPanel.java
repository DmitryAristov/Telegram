package org.telegram.ui;
import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.lerp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HeaderBottomButtonPanel extends LinearLayout {

    private final List<ButtonModel> buttons = new ArrayList<>();
    private final List<HeaderButton> headerCurrentButtons = new ArrayList<>();
    private final int buttonHeight = dp(54);
    private int panelWidth = 0;
    private int buttonWidth = dp(54);

    private MessagesController.PeerColor peerColor;
    private int defaultBaseColor;
    private int normalColor;
    private int pressedColor;
    private int textColor = Color.WHITE;
    private ProfileActivity profileActivity;
    private Context context;
    private BaseFragment lastFragment;
    private long did;
    private int currentAvatarAverageColor;
    private float expandProgress;

    public HeaderBottomButtonPanel(Context context) {
        super(context);
        init(context);
    }

    public HeaderBottomButtonPanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HeaderBottomButtonPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setClickable(false);
        setFocusable(false);
        createButtons();
        this.setPadding(dp(5), 0, dp(5), 0);
    }

    public static final int message_button = 0;
    public static final int change_profile_picture = 1;
    public static final int join_channel = 2;
    public static final int mute_button = 3;
    public static final int discuss_button = 4;
    public static final int call = 5;
    public static final int voice_chat = 6;
    public static final int live_stream = 7;
    public static final int video_call = 8;
    public static final int send_gift = 9;
    public static final int share = 10;
    public static final int leave_button = 11;
    public static final int report_channel = 12;
    public static final int stop_bot = 13;
    public static final int add_story = 14;

    private void createButtons() {
        buttons.add(message_button, new ButtonModel(
        LocaleController.getString(R.string.Message),
                R.drawable.msg_profile_header_message,
                pair -> profileActivity.onWriteButtonClick()
        ));

        buttons.add(change_profile_picture, new ButtonModel(
                LocaleController.getString(R.string.AccDescrChangeProfilePicture),
                R.drawable.msg_profile_header_camera, // TODO
                pair -> profileActivity.onWriteButtonClick()
        ));

        buttons.add(join_channel, new ButtonModel(
                LocaleController.getString(R.string.VoipChatJoin),
                R.drawable.msg_profile_header_join, //TODO
                pair -> profileActivity.joinButtonPressed(context, lastFragment)
        ));

        buttons.add(mute_button, new ButtonModel(
                LocaleController.getString(R.string.Unmute),
                R.drawable.msg_profile_header_unmute,
                pair -> {
                    View v = pair.first;
                    MotionEvent event = pair.second;
                    float x = event.getX();
                    float y = event.getY();

                    profileActivity.muteButtonPressed(v, x, y, context, did);
                }
        ));

        buttons.add(discuss_button, new ButtonModel(
                LocaleController.getString(R.string.Discussion),
                R.drawable.msg_profile_header_message,
                pair -> profileActivity.openDiscussion()
        ));

        buttons.add(call, new ButtonModel(
                LocaleController.getString(R.string.Call),
                R.drawable.msg_profile_header_call,
                pair -> profileActivity.callButtonPressed(false)
        ));

        buttons.add(voice_chat, new ButtonModel(
                LocaleController.getString(R.string.VoipGroupVoiceChat),
                R.drawable.msg_profile_header_live_stream,
                pair -> profileActivity.callButtonPressed(false)
        ));

        buttons.add(live_stream, new ButtonModel(
                LocaleController.getString(R.string.VoipChannelVoiceChat),
                R.drawable.msg_profile_header_live_stream,
                pair -> profileActivity.callButtonPressed(false)
        ));

        buttons.add(video_call, new ButtonModel(
                LocaleController.getString(R.string.GroupCallCreateVideo),
                R.drawable.msg_profile_header_video,
                pair -> profileActivity.callButtonPressed(true)
        ));

        buttons.add(send_gift, new ButtonModel(
                LocaleController.getString(R.string.Gift2TitleProfile),
                R.drawable.msg_profile_header_gift,
                pair -> profileActivity.sendGiftButtonPressed()
        ));

        buttons.add(share, new ButtonModel(
                LocaleController.getString(R.string.BotShare),
                R.drawable.msg_profile_header_share,
                pair -> profileActivity.shareButtonPressed()
        ));

        buttons.add(leave_button, new ButtonModel(
                LocaleController.getString(R.string.VoipGroupLeave),
                R.drawable.msg_profile_header_leave,
                pair -> profileActivity.leaveChatPressed()
        ));

        buttons.add(report_channel, new ButtonModel(
                LocaleController.getString(R.string.ReportBot),
                R.drawable.msg_profile_header_report,
                pair -> profileActivity.reportChannelPressed()
        ));

        buttons.add(stop_bot, new ButtonModel(
                LocaleController.getString(R.string.BizBotStop),
                R.drawable.msg_profile_header_block,
                pair -> profileActivity.blockContactButtonPressed()
        ));

        buttons.add(add_story, new ButtonModel(
                LocaleController.getString(R.string.AddStory),
                R.drawable.msg_profile_header_story,
                pair -> profileActivity.addStoryButtonPressed()
        ));
    }

    private float currentCollapseProgress = 0f;

    public void animateCollapseAll(float progress) {
        currentCollapseProgress = progress;
        if (currentCollapseProgress == 0) {
            this.setVisibility(GONE);
        } else {
            this.setVisibility(VISIBLE);
        }

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof HeaderButton) {
                ((HeaderButton) child).animateCollapse(progress);
            }
        }
        if (currentCollapseProgress < 1) {
            setBackgroundColor(Color.TRANSPARENT);
        }
    }

    public void setCurrentAvatarAverageColor(int color) {
        this.currentAvatarAverageColor = color;
        }

    public void setExpandAnimationProgress(float progress) {
        this.expandProgress = progress;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof HeaderButton) {
                ((HeaderButton) child).invalidate();
            }
        }
    }

    public void setSendMessageButtonEnabled() {
        buttons.get(message_button).enabled = true;
    }

    public void setOpenDiscussionButtonEnabled() {
        buttons.get(discuss_button).enabled = true;
    }

    public void setChangeProfilePictureButtonEnabled() {
        buttons.get(change_profile_picture).enabled = true;
    }

    public void setMuteButtonEnabled(boolean muted) {
        if (muted) {
            buttons.get(mute_button).iconResId = R.drawable.msg_profile_header_unmute;
            buttons.get(mute_button).title = LocaleController.getString(R.string.Unmute);
        } else {
            buttons.get(mute_button).iconResId = R.drawable.msg_profile_header_mute;
            buttons.get(mute_button).title = LocaleController.getString(R.string.Mute);
        }
        buttons.get(mute_button).enabled = true;
    }

    public void setCallButtonEnabled() {
        buttons.get(call).enabled = true;
    }

    public void setVoiceChatButtonEnabled() {
        buttons.get(voice_chat).enabled = true;
    }

    public void setLiveStreamButtonEnabled() {
        buttons.get(live_stream).enabled = true;
    }

    public void setVideoCallButtonEnabled() {
        buttons.get(video_call).enabled = true;
    }

    public void setLeaveButtonEnabled() {
        buttons.get(leave_button).enabled = true;
    }

    public void setSendGiftPremiumButtonEnabled() {
        buttons.get(send_gift).enabled = true;
    }

    public void setJoinChannelButtonEnabled() {
        buttons.get(join_channel).enabled = true;
    }

    public void setShareButtonEnabled() {
        buttons.get(share).enabled = true;
    }

    public void setReportChannelButtonEnabled() {
        buttons.get(report_channel).enabled = true;
    }

    public void setBotStopButtonEnabled() {
        buttons.get(stop_bot).enabled = true;
    }

    public void setAddStoryButtonEnabled() {
        buttons.get(add_story).enabled = true;
    }

    public void setButtonsVisibility() {
        checkCornerCases();

        List<ButtonModel> enabledButtons = new ArrayList<>();
        for (ButtonModel button : buttons) {
            if (button != null && button.enabled) {
                enabledButtons.add(button);
            }
        }

        if (enabledButtons.size() > 4) {
            int toDisable = enabledButtons.size() - 4;
            for (int i = enabledButtons.size() - 1; i >= 0 && toDisable > 0; i--, toDisable--) {
                enabledButtons.get(i).enabled = false;
            }
        }

        for (ButtonModel button : buttons) {
            if (button != null) {
                button.visible = button.enabled;
            }
        }
    }

    private void checkCornerCases() {
        if (buttons.get(mute_button).enabled &&
                buttons.get(discuss_button).enabled &&
                buttons.get(send_gift).enabled &&
                buttons.get(share).enabled &&
                buttons.get(leave_button).enabled
        ) {
            buttons.get(send_gift).enabled = false;
        } else if (buttons.get(mute_button).enabled &&
                buttons.get(join_channel).enabled &&
                buttons.get(send_gift).enabled &&
                buttons.get(share).enabled &&
                buttons.get(report_channel).enabled
        ) {
            buttons.get(send_gift).enabled = false;
        } else if ((buttons.get(mute_button).enabled &&
                        buttons.get(live_stream).enabled &&
                        buttons.get(send_gift).enabled &&
                        buttons.get(add_story).enabled) ||
                (buttons.get(change_profile_picture).enabled)
        ) {
            buttons.get(mute_button).enabled = false;
        }
    }

    public void initializeHeaderButtons() {
        initializeColors();
        headerCurrentButtons.clear();
        removeAllViews();

        for (int i = 0; i < buttons.size(); i++) {
            ButtonModel button = buttons.get(i);
            if (button != null && button.visible) {
                HeaderButton headerButton = createHeaderButton(button, i);
                headerCurrentButtons.add(headerButton);
            }
        }
        if (!headerCurrentButtons.isEmpty()) {
            buttonWidth = (panelWidth - dp(10)) / headerCurrentButtons.size();
            headerCurrentButtons.forEach(button -> button.setPivotX(buttonWidth / 2f));
        }
        requestLayout();
    }

    private HeaderButton createHeaderButton(ButtonModel model, int i) {
        HeaderButton headerButton = new HeaderButton(getContext());
        headerButton.setIndex(i);
        headerButton.setIcon(model.iconResId);
        headerButton.setDescriptionText(model.title);
        headerButton.setOnTouchHandler(model.onTouchHandler);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, buttonHeight, 1.0f);
        params.setMargins(dp(4), dp(8), dp(4), dp(8));

        addView(headerButton, params);
        headerButton.animateCollapse(currentCollapseProgress);
        return headerButton;
    }

    void initializeColors() {
        if (expandProgress > 0.75f) {
            normalColor = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(ColorUtils.blendARGB(defaultBaseColor, currentAvatarAverageColor, (expandProgress - 0.75f) / 0.25f), Color.BLACK, 0.2f), 160);
            pressedColor = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(ColorUtils.blendARGB(defaultBaseColor, currentAvatarAverageColor, (expandProgress - 0.75f) / 0.25f), Color.WHITE, 0.4f), 160);
        } else {
            if (peerColor != null) {
                int baseColor = peerColor.getBgColor1(false);

                int darker = ColorUtils.blendARGB(baseColor, Color.BLACK, 0.3f);
                int lighter = ColorUtils.blendARGB(baseColor, Color.WHITE, 0.25f);

                normalColor = ColorUtils.setAlphaComponent(darker, 160);
                pressedColor = ColorUtils.setAlphaComponent(lighter, 160);
            } else {
                defaultBaseColor = Theme.getColor(Theme.key_avatar_backgroundActionBarBlue);
                normalColor = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(defaultBaseColor, Color.BLACK, 0.2f), 160);
                pressedColor = ColorUtils.setAlphaComponent(ColorUtils.blendARGB(defaultBaseColor, Color.WHITE, 0.4f), 160);
            }
        }
        this.textColor = Color.WHITE;
    }

    public void updateColors(MessagesController.PeerColor peerColor) {
        this.peerColor = peerColor;
        initializeHeaderButtons();
    }

    public void setWidth(int width) {
        if (panelWidth != width) {
            panelWidth = width;
            initializeHeaderButtons();
        }
    }

    public void setProfileActivity(ProfileActivity profileActivity) {
        this.profileActivity = profileActivity;
    }

    public void setLastFragment(BaseFragment lastFragment) {
        this.lastFragment = lastFragment;
    }

    public void setDid(long did) {
        this.did = did;
    }

    public void notifyMuteButtonChanged(boolean muted) {
        headerCurrentButtons.stream()
                .filter(button -> button.getIndex() == mute_button)
                .findFirst()
                .ifPresent(button -> {
                    button.setIcon(muted ? R.drawable.msg_profile_header_unmute : R.drawable.msg_profile_header_mute);
                    button.setDescriptionText(muted ? LocaleController.getString(R.string.Unmute) : LocaleController.getString(R.string.Mute));
                });
    }

    public void onJoinedChannel(boolean joined) {
        if (joined) {
            buttons.get(join_channel).enabled = false;
            buttons.get(report_channel).enabled = false;
            buttons.get(leave_button).enabled = true;
            buttons.get(send_gift).enabled = true;
            setButtonsVisibility();
            initializeHeaderButtons();
        }
    }

    public BaseFragment getLastFragment() {
        return lastFragment;
    }

    public long getDid() {
        return did;
    }

    public class HeaderButton extends FrameLayout {

        private int index;
        private ImageView imageView;
        private TextView textView;
        private ShapeDrawable shape;
        private Consumer<Pair<View, MotionEvent>> onTouchHandler;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ValueAnimator rippleAnimator = ValueAnimator.ofFloat(0f, 1f);
        private final Interpolator rippleInterpolator = new DecelerateInterpolator();
        private float rippleAnimatorProgress = 0f;
        private final RectF rippleRect = new RectF();
        private final Path ripplePath = new Path();

        private float touchX = 0f;
        private float rippleWidth = 0f;
        private int rippleAlpha = 0;

        public HeaderButton(Context context) {
            super(context);
            init();
        }

        public HeaderButton(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public HeaderButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        private void init() {
            setFocusable(true);
            setClickable(true);
            setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
            ));
            setClipToPadding(false);
            setClipChildren(false);
            setWillNotDraw(false);

            final float cornerRadius = dp(12);
            shape = new ShapeDrawable(
                    new RoundRectShape(
                            new float[] {
                                    cornerRadius, cornerRadius,
                                    cornerRadius, cornerRadius,
                                    cornerRadius, cornerRadius,
                                    cornerRadius, cornerRadius
                            },
                            null, null
                    )
            );
            shape.getPaint().setColor(normalColor);

            paint.setColor(normalColor);
            rippleAnimator.setDuration(300);
            rippleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

            rippleAnimator.addUpdateListener(animation -> {
                float progress = rippleAnimatorProgress = (float) animation.getAnimatedValue();

                rippleWidth = buttonWidth / 1.5f + progress * (getWidth() - buttonWidth / 1.5f);
                rippleAlpha = (int) ((1f - progress) * 120);

                invalidate();
            });

            rippleAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(@NonNull Animator animation) {
                    rippleWidth = buttonWidth / 2f;
                    rippleAlpha = 0;
                    invalidate();
                }

                @Override
                public void onAnimationCancel(@NonNull Animator animation) {
                    rippleWidth = buttonWidth / 2f;
                    rippleAlpha = 0;
                    invalidate();
                }
            });

            imageView = new ImageView(getContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            LayoutParams imageParams = new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(27));
            imageParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            imageParams.topMargin = dp(6);
            addView(imageView, imageParams);

            textView = new TextView(getContext());
            textView.setTextColor(textColor);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            textView.setGravity(Gravity.CENTER);
            LayoutParams textParams = new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(18));
            textParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            textParams.bottomMargin = dp(4);
            addView(textView, textParams);
        }

        public void setIcon(int iconResId) {
            Drawable icon = ContextCompat.getDrawable(getContext(), iconResId);
            imageView.setImageDrawable(icon);
        }

        public void setDescriptionText(String text) {
            textView.setText(text);
        }

        public void animateCollapse(float progress) {
            setAlpha(progress);
            setPivotY(buttonHeight);
            setScaleY(progress);

            if (imageView != null) {
                imageView.setScaleX(progress);
                imageView.setScaleY(progress);
                imageView.setAlpha(progress);
            }

            if (textView != null) {
                textView.setScaleX(progress);
                textView.setScaleY(progress);
                textView.setAlpha(progress);
            }
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);

            initializeColors();
            float scaleX, scaleY;
            float t = rippleInterpolator.getInterpolation(rippleAnimatorProgress <= 0.5f ? rippleAnimatorProgress * 2f : (rippleAnimatorProgress - 0.5f) * 2f);
            if (rippleAnimatorProgress <= 0.5f) {
                scaleX = lerp(1.0f, 0.8f, t);
                scaleY = lerp(1.0f, 0.85f, t);
            } else {
                scaleX = lerp(0.8f, 1.0f, t);
                scaleY = lerp(0.85f, 1.0f, t);
            }
            setScaleX(scaleX);
            if (currentCollapseProgress == 1 || currentCollapseProgress == 0) {
                setPivotY(buttonHeight / 2f);
                setScaleY(scaleY);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                float cornerRadius = dp(12);

                paint.setColor(normalColor);
                canvas.drawRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius, paint);

                if (rippleWidth > 0f && rippleAlpha > 0) {
                    paint.setColor(pressedColor);
                    paint.setAlpha(rippleAlpha);

                    float left = Math.max(0, touchX - rippleWidth / 2f);
                    float right = Math.min(getWidth(), touchX + rippleWidth / 2f);
                    float top = 0;
                    float bottom = getHeight();

                    float rx = cornerRadius;  // горизонтальный радиус
                    float ry = lerp(buttonHeight * 2f, cornerRadius, rippleWidth / getWidth()); // вертикальный радиус

                    rippleRect.left = left;
                    rippleRect.top = top;
                    rippleRect.right = right;
                    rippleRect.bottom = bottom;

                    ripplePath.reset();
                    ripplePath.addRoundRect(rippleRect, new float[]{
                            rx, ry,
                            rx, ry,
                            rx, ry,
                            rx, ry
                    }, Path.Direction.CW);

                    canvas.drawPath(ripplePath, paint);

                    paint.setAlpha(255);
                }
            }
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchX = event.getX();
                    rippleAnimator.cancel();
                    rippleWidth = buttonWidth / 2f;
                    rippleAlpha = 120;
                    rippleAnimator.start();
                    HeaderButton button = ((HeaderButton) this);
                    button.onTouchHandler.accept(Pair.create(this, event));
                    break;
                case MotionEvent.ACTION_UP:
                    performClick();
                    break;
            }

            return super.onTouchEvent(event);
        }

        public void setIndex(int i) {
            this.index = i;
        }

        public int getIndex() {
            return index;
        }

        public void setOnTouchHandler(Consumer<Pair<View, MotionEvent>> onTouchHandler) {
            this.onTouchHandler = onTouchHandler;
        }
    }

    public class ButtonModel {
        private final Consumer<Pair<View, MotionEvent>> onTouchHandler;
        private String title;
        private int iconResId;
        private boolean enabled;
        private boolean visible;

        public ButtonModel(
            String title,
            int iconResId,
            Consumer<Pair<View, MotionEvent>> onTouchHandler
        ) {
            this.title = title;
            this.iconResId = iconResId;
            this.onTouchHandler = onTouchHandler;
            this.enabled = false;
            this.visible = false;
        }
    }
}
