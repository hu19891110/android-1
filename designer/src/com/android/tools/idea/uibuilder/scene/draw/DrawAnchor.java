/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.uibuilder.scene.draw;

import android.support.constraint.solver.widgets.Animator;
import com.android.tools.idea.uibuilder.scene.SceneContext;
import com.android.tools.sherpa.drawing.ColorSet;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Draws an Anchor
 */
public class DrawAnchor extends DrawRegion {
  public static final int TYPE_NORMAL = 0;
  public static final int TYPE_BASELINE = 1;
  public static final int NORMAL = 0;
  public static final int OVER = 1;
  int myMode;
  boolean myIsConnected;
  int myType = TYPE_NORMAL;

  public DrawAnchor(String s) {
    String[] sp = s.split(",");
    int c = 0;
    c = super.parse(sp, c);
    myMode = Integer.parseInt(sp[c++]);
    myIsConnected = Boolean.parseBoolean(sp[c++]);
  }

  public DrawAnchor(int x, int y, int width, int height, int type, boolean isConnected, int mode) {
    super(x, y, width, height);
    myMode = mode;
    myIsConnected = isConnected;
    myType = type;
  }

  private int getPulseAlpha(int deltaT) {
    int v = (int)Animator.EaseInOutinterpolator((deltaT % 1000) / 1000.0, 0, 255);
    return v;
  }

  @Override
  public int getLevel() {
     return TARGET_LEVEL;
  }

  @Override
  public void paint(Graphics2D g, SceneContext sceneContext) {
    if (myType == TYPE_BASELINE) {
      paintBaseline(g, sceneContext);
      return;
    }
    ColorSet colorSet = sceneContext.getColorSet();
    Color background = colorSet.getComponentObligatoryBackground();
    Color color = colorSet.getFrames();

    if (myMode == OVER) {
      if (myIsConnected) {
        g.setColor(colorSet.getAnchorDisconnectionCircle());
      }
      else {
        g.setColor(colorSet.getAnchorConnectionCircle());
      }
      int delta = width / 3;
      int delta2 = delta * 2;
      g.fillRoundRect(x - delta, y - delta, width + delta2, height + delta2, width + delta2, height + delta2);
      g.drawRoundRect(x - delta, y - delta, width + delta2, height + delta2, width + delta2, height + delta2);
    }

    g.setColor(background);
    g.fillRoundRect(x, y, width, height, width, height);
    g.setColor(color);
    g.drawRoundRect(x, y, width, height, width, height);
    int delta = width / 4;
    int delta2 = delta * 2;
    if (myIsConnected) {
      g.fillRoundRect(x + delta, y + delta, width - delta2, height - delta2, width - delta2, height - delta2);
      g.drawRoundRect(x + delta, y + delta, width - delta2, height - delta2, width - delta2, height - delta2);
    }
    if (myMode == OVER) {
      int alpha = getPulseAlpha((int)(sceneContext.getTime() % 1000));
      Composite comp = g.getComposite();
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
      if (myIsConnected) {
        g.setColor(colorSet.getAnchorDisconnectionCircle());
      }
      else {
        g.setColor(colorSet.getAnchorConnectionCircle());
      }
      g.fillRoundRect(x, y, width, height, width, height);
      sceneContext.repaint();
      g.setComposite(comp);
    }
  }

  public void paintBaseline(Graphics2D g, SceneContext sceneContext) {
    int inset = width / 10;
    ColorSet colorSet = sceneContext.getColorSet();
    Color background = colorSet.getComponentObligatoryBackground();
    Color color = colorSet.getFrames();
    g.setColor(color);
    g.fillRect(x, y + height / 2, width, 1);
    int ovalX = x + inset;
    int ovalW = width - 2 * inset;
    g.setColor(background);
    g.fillRoundRect(ovalX, y, ovalW, height, height, height);
    g.setColor(color);
    g.drawRoundRect(ovalX, y, ovalW, height, height, height);
    int delta = 3;
    int delta2 = delta * 2;
    if (myIsConnected) {
      g.fillRoundRect(ovalX + delta, y + delta, ovalW - delta2, height - delta2, height - delta2, height -delta2);
      g.drawRoundRect(ovalX + delta, y + delta, ovalW - delta2, height - delta2, height - delta2, height -delta2);
    }
    if (myMode == OVER) {
      int alpha = getPulseAlpha((int)(sceneContext.getTime() % 1000));
      Composite comp = g.getComposite();
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
      if (myIsConnected) {
        g.setColor(colorSet.getAnchorDisconnectionCircle());
      }
      else {
        g.setColor(colorSet.getAnchorConnectionCircle());
      }
      g.fillRoundRect(ovalX, y, ovalW, height, height, height);
      g.drawRoundRect(ovalX, y, ovalW, height, height, height);
      sceneContext.repaint();
      g.setComposite(comp);
    }
  }

  @Override
  public String serialize() {
    return this.getClass().getSimpleName() + "," + x + "," + y + "," + width + "," + height + "," + myMode;
  }

  public static void add(@NotNull DisplayList list,
                         @NotNull SceneContext transform,
                         float left,
                         float top,
                         float right,
                         float bottom,
                         int type,
                         boolean isConnected,
                         int mode) {
    int l = transform.getSwingX(left);
    int t = transform.getSwingY(top);
    int w = transform.getSwingDimension(right - left);
    int h = transform.getSwingDimension(bottom - top);
    list.add(new DrawAnchor(l, t, w, h, type, isConnected, mode));
  }
}
