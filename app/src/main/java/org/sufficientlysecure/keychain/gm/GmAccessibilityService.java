/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.gm;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import org.sufficientlysecure.keychain.intents.OpenKeychainIntents;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class GmAccessibilityService extends AccessibilityService {

    private static final String WEB_VIEW_CLASS_NAME = "android.webkit.WebView";
    private static final String VIEW_CLASS_NAME = "android.view.View";

    private static final String BEGIN_PGP_MESSAGE = "-----BEGIN PGP MESSAGE-----";
    private static final String END_PGP_MESSAGE = "-----END PGP MESSAGE-----";
    private static final int CHECKSUM_LENGTH = 5;

    private WindowManager mWindowManager;
    private FrameLayout mOverlay;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceConnected() {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (!(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED == event.getEventType())) {
            return;
        }
        Log.d(Constants.TAG, "TYPE_WINDOW_CONTENT_CHANGED");

        // This AccessibilityNodeInfo represents the view that fired the
        // AccessibilityEvent. The following code will use it to traverse the
        // view hierarchy, using this node as a starting point.
        //
        // NOTE: Every method that returns an AccessibilityNodeInfo may return null,
        // because the explored window is in another process and the
        // corresponding View might be gone by the time your request reaches the
        // view hierarchy.
//        AccessibilityNodeInfo source = event.getSource();

        // TODO: start with "com.google.android.gm:id/conversation" ?
        AccessibilityNodeInfo source = getRootInActiveWindow();
        if (source == null) {
            return;
        }
        closeOverlay();

        ArrayList<AccessibilityNodeInfo> pgpNodes = new ArrayList<>();
        findPgpNodeInfo(source, pgpNodes);
        for (final AccessibilityNodeInfo node : pgpNodes) {
            Log.d(Constants.TAG, "node=" + node);

            Rect currRect = new Rect();
            node.getBoundsInScreen(currRect);
            drawOverlay(currRect, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    decryptWithOpenKeychain(node);
                }
            });
        }
    }

    private void decryptWithOpenKeychain(AccessibilityNodeInfo node) {
        closeOverlay();

        try {
            Uri dateUri = readToTempFile(fixContentDescription(node));

            Intent i = new Intent(OpenKeychainIntents.DECRYPT_DATA);
            i.setData(dateUri);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        } catch (IOException e) {
            Log.e(Constants.TAG, "read to temp failed!", e);
        }
    }

    @Nullable
    public Uri readToTempFile(String text) throws IOException {
        Uri tempFile = TemporaryStorageProvider.createFile(this);
        OutputStream outStream = getContentResolver().openOutputStream(tempFile);
        if (outStream == null) {
            return null;
        }

        outStream.write(text.getBytes());
        outStream.close();
        return tempFile;
    }

    private String fixContentDescription(AccessibilityNodeInfo node) {
        String content = node.getContentDescription().toString();

        // NOTE: Unfortunately, line breaks are missing from content description, thus
        // we are reformatting now:

        // TODO: get charset from header?

        // find "hQ", start of pgp message (0x80 byte) and remove everything before
        content = content.replaceFirst(".*hQ", "hQ");

        StringBuilder builder = new StringBuilder(content);

        // re-add -----BEGIN PGP MESSAGE-----
        String header = BEGIN_PGP_MESSAGE + "\n\n";
        builder.insert(0, header);

        int indexOfEnd = builder.lastIndexOf(END_PGP_MESSAGE);
        // TODO: check if END pgp message is really inside string! if not -> gmail has cut it!
        builder.insert(indexOfEnd, "\n");
        int i = indexOfEnd - CHECKSUM_LENGTH;
        builder.insert(i, "\n");

        // split into 65 character chunks
        int currentIndex = header.length() + 64;
        while (currentIndex < indexOfEnd) {
            builder.insert(currentIndex, "\n");
            currentIndex += 65;

            // stop where checksum starts
            indexOfEnd = builder.lastIndexOf(END_PGP_MESSAGE) - CHECKSUM_LENGTH - 2;
        }

        content = builder.toString();

        if (Constants.DEBUG) {
            // split for long messages
            for (String line : content.split("\n")) {
                Log.d(Constants.TAG, line);
            }
        }

        return content;
    }

    private void drawOverlay(Rect currRect, View.OnClickListener onClickListener) {

        mOverlay = new FrameLayout(this);

        // must be encapsulated into FrameLayout for animation
        FrameLayout mAnimatedChild = new FrameLayout(this);
        LayoutInflater systemInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final Context contextThemeWrapper = new ContextThemeWrapper(this, R.style.FixedBottomSheetTheme);
        LayoutInflater inflater = systemInflater.cloneInContext(contextThemeWrapper);

        View child = inflater.inflate(R.layout.fixed_bottom_sheet, null);
        mAnimatedChild.addView(child);

        Button b = (Button) child.findViewById(R.id.fixed_bottom_sheet_button);
        b.setText(R.string.decrypt_with_openkeychain);
        b.setOnClickListener(onClickListener);

        ImageButton close = (ImageButton) child.findViewById(R.id.fixed_bottom_sheet_close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOverlay != null && ViewCompat.isAttachedToWindow(mOverlay)) {
                    mWindowManager.removeView(mOverlay);
                    mOverlay = null;
                }
            }
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                0,
                0,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.BOTTOM | Gravity.LEFT;
        params.windowAnimations = R.style.OverlayAnimation;
        mWindowManager.addView(mOverlay, params);
        mOverlay.addView(mAnimatedChild);
    }

    private void findPgpNodeInfo(AccessibilityNodeInfo parent,
                                 ArrayList<AccessibilityNodeInfo> pgpNodes) {

        for (int i = 0; i < parent.getChildCount(); i++) {
            AccessibilityNodeInfo currentChild = parent.getChild(i);

            /*
             WebView
             |- WebView
                |- View
                |- View
                |- View
             */
            if (WEB_VIEW_CLASS_NAME.equals(parent.getClassName())
                    && VIEW_CLASS_NAME.equals(currentChild.getClassName())
                    && !TextUtils.isEmpty(currentChild.getContentDescription())
                    && currentChild.getContentDescription().toString().startsWith(BEGIN_PGP_MESSAGE)) {

                pgpNodes.add(currentChild);
            } else {
                // recursive traversal
                findPgpNodeInfo(currentChild, pgpNodes);

                currentChild.recycle();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInterrupt() {
        /* do nothing */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        closeOverlay();
    }

    private void closeOverlay() {
        if (mOverlay != null && ViewCompat.isAttachedToWindow(mOverlay)) {
            mWindowManager.removeView(mOverlay);
            mOverlay = null;
        }
    }
}
