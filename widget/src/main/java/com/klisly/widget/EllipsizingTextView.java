package com.klisly.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class EllipsizingTextView extends TextView {
    private String subsitution = "...";
    // 省略号的位置
    public static int ELLIPSIZE_TYPE_NONE = 0;
    public static int ELLIPSIZE_TYPE_SSTART = 1;
    public static int ELLIPSIZE_TYPE_MIDDLE = 2;
    public static int ELLIPSIZE_TYPE_END = 3;
    private final List<EllipsizeListener> ellipsizeListeners = new ArrayList<EllipsizeListener>();
    private boolean isEllipsized = true;
    private boolean isStale;
    private boolean programmaticChange;
    private String fullText;
    private int maxLines = -1;
    private int ellipsizeType = ELLIPSIZE_TYPE_NONE;
    private float lineSpacingMultiplier = 1.0f;
    private float lineAdditionalVerticalPadding = 0.0f;

    public EllipsizingTextView(Context context) {
        super(context);
    }

    public EllipsizingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.etv);
        String tmpSub = typedArray.getString(R.styleable.etv_substitution);
        if(tmpSub != null){
            subsitution = tmpSub;
        }
        maxLines = typedArray.getInteger(R.styleable.etv_maxlines, -1);
        ellipsizeType = typedArray.getInteger(R.styleable.etv_ellipsize, ELLIPSIZE_TYPE_NONE);

        typedArray.recycle();
    }

    public EllipsizingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void addEllipsizeListener(EllipsizeListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        ellipsizeListeners.add(listener);
    }

    public void removeEllipsizeListener(EllipsizeListener listener) {
        ellipsizeListeners.remove(listener);
    }

    public boolean isEllipsized() {
        return isEllipsized;
    }

    public int getMaxLines() {
        return maxLines;
    }

    @Override
    public void setMaxLines(int maxLines) {
        super.setMaxLines(maxLines);
        this.maxLines = maxLines;
        isStale = true;
    }

    public int getEllipsizeType() {
        return ellipsizeType;
    }

    public void setEllipsizeType(int ellipsizeType) {
        this.ellipsizeType = ellipsizeType;
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        this.lineAdditionalVerticalPadding = add;
        this.lineSpacingMultiplier = mult;
        super.setLineSpacing(add, mult);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        if (!programmaticChange) {
            fullText = text.toString();
            isStale = true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isStale) {
            super.setEllipsize(null);
            resetText();
        }
        super.onDraw(canvas);
    }

    private void resetText() {
        int maxLines = getMaxLines();
        String workingText = fullText;
        boolean ellipsized = false;
        if (maxLines != -1) {
            Layout layout = createWorkingLayout(workingText);
            if (layout.getLineCount() > maxLines) {
                if (ellipsizeType == ELLIPSIZE_TYPE_END) {
                    workingText = fullText.substring(0, layout.getLineEnd(maxLines - 1)).trim();
                    while (createWorkingLayout(workingText + subsitution).getLineCount() > maxLines) {
                        int lastSpace = workingText.lastIndexOf(' ');
                        if (lastSpace == -1) {
                            break;
                        }
                        workingText = workingText.substring(0, lastSpace);
                    }
                    workingText = workingText + subsitution;
                } else if (ellipsizeType == ELLIPSIZE_TYPE_MIDDLE) {
                    try {
                        int index = layout.getLineEnd(maxLines - 1);
                        int span = fullText.length() - index;
                        String pre = "";
                        String end = "";
                        if (fullText.length() / 2 - (span+1) / 2  >= 0) {
                            pre = fullText.substring(0, fullText.length() / 2 - (span +1)/ 2);
                        }
                        if (fullText.length() / 2 + (span +1)/ 2  < fullText.length()) {
                            end = fullText.substring(fullText.length() / 2 + (span +1)/ 2 , fullText.length());
                        }
                        workingText = pre.concat(subsitution).concat(end);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (ellipsizeType == ELLIPSIZE_TYPE_SSTART) {
                    try {
                        Log.i(EllipsizingTextView.class.getSimpleName(), " begin");
                        workingText = fullText.substring(fullText.length() - layout.getLineEnd(maxLines - 1),
                                layout.getLineEnd(maxLines - 1)).trim();
                        workingText = subsitution.concat(workingText);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ellipsized = true;
            }
        }
        if (!workingText.equals(getText())) {
            programmaticChange = true;
            try {
                setText(workingText);
            } finally {
                programmaticChange = false;
            }
        }
        isStale = false;
        if (ellipsized != isEllipsized) {
            isEllipsized = ellipsized;
            for (EllipsizeListener listener : ellipsizeListeners) {
                listener.ellipsizeStateChanged(ellipsized);
            }
        }
    }

    private Layout createWorkingLayout(String workingText) {
        return new StaticLayout(workingText, getPaint(), getWidth() - getPaddingLeft() - getPaddingRight(),
                Layout.Alignment.ALIGN_NORMAL, lineSpacingMultiplier, lineAdditionalVerticalPadding, false);
    }

    @Override
    public void setEllipsize(TextUtils.TruncateAt where) {
        // Ellipsize settings are not respected
    }

    public interface EllipsizeListener {
        void ellipsizeStateChanged(boolean ellipsized);
    }
}