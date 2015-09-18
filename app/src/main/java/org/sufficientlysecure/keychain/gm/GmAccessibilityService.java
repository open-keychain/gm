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
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

public class GmAccessibilityService extends AccessibilityService {

    private static final String LOG_TAG = "Keychain_gm";

    private static final String WEB_VIEW_CLASS_NAME = "android.webkit.WebView";
    private static final String VIEW_CLASS_NAME = "android.view.View";

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceConnected() {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        if (!(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED == event.getEventType())) {
            return;
        }
        Log.d(LOG_TAG, "TYPE_WINDOW_CONTENT_CHANGED");


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
        Log.i(LOG_TAG, "ACC::onAccessibilityEvent: nodeInfo=" + source);

        ArrayList<AccessibilityNodeInfo> pgpNodes = new ArrayList<>();
        findPgpNodeInfo(source, pgpNodes);
        for (AccessibilityNodeInfo node : pgpNodes) {

            // TODO: missing line breaks!!!
            // thus we need to inject javascript into webview and do it like
            // https://code.google.com/p/eyes-free/source/browse/trunk/accessibilityServices/talkback/src/com/google/android/marvin/talkback/ProcessorWebContent.java?r=829
            // ??????
            CharSequence content = node.getContentDescription();
            for (String line : ((String) content).split("/")) {
                Log.d(LOG_TAG, line);
            }
        }


        // TODO
        // - javascript injection for newlines?
        // -


//        AccessibilityNodeInfo root = getRootInActiveWindow();
//        ArrayDeque<AccessibilityNodeInfo> nodeQueue
//                = new ArrayDeque<AccessibilityNodeInfo>();
//        nodeQueue.add(root);
//
//        AccessibilityNodeInfo target = null;
//
//        for (AccessibilityNodeInfo node = nodeQueue.pollFirst(); node != null; node = nodeQueue.pollFirst()) {
//            Rect currRect = new Rect();
//
//            node.getBoundsInScreen(currRect);
////            if(!currRect.contains(eventX, eventY)) {
////                node.recycle();
////                continue;
////            }
//
//            if (target == null) {
//                target = node;
//                continue;
//            }
//
//            Rect targetRect = new Rect();
//            target.getBoundsInScreen(targetRect);
//            if (currRect.width() * currRect.height() <
//                    targetRect.width() * targetRect.height()) {
//                target.recycle();
//                target = node;
//            }
//        }
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
                    && !TextUtils.isEmpty(currentChild.getContentDescription())) {

                Log.d(LOG_TAG, "MATCHED");
                // TODO: match on -----BEGIN PGP MESSAGE-----

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
    }
}
