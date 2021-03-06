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
package com.android.tools.adtui.common.datareducer;

import com.android.tools.adtui.chart.linechart.LineConfig;
import org.jetbrains.annotations.NotNull;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

/**
 * The {@link LineChartReducer} is used by {@code LineChart} component to be able
 * to render faster by reducing its data.
 * The basic idea behind this algorithm is to reduce number of points to available pixels.
 * For every pixel it draws 4 points: the first point, the last point,
 * the points with minimum and maximum Y coordinates within a pixel.
 * It draws similar shape with the original, because of the fact that width of a line is 1px.
 */
public class LineChartReducer implements DataReducer {
  private static final float EPS  = 1e-6f;

  /**
   * Number 6 is needed by {@link PathIterator#currentSegment(float[])}.
   */
  private static int PATH_ITERATOR_COORDS_COUNT = 6;

  @Override
  public Path2D reduce(@NotNull Path2D path, @NotNull LineConfig config) {
    if (path.getCurrentPoint() == null) {
      return path;
    }

    Path2D resultPath = new Path2D.Float();
    float[] coords = new float[PATH_ITERATOR_COORDS_COUNT];
    float pixel = -1;
    float minX = -1, minY = -1;
    float maxX = -1, maxY = -1;
    float curX = -1, curY = -1;
    int minIndex = -1, maxIndex = -1;
    int curIndex = 0;

    PathIterator iterator = path.getPathIterator(null);
    while (!iterator.isDone()) {
      int segType = iterator.currentSegment(coords);
      assert segType == PathIterator.SEG_MOVETO || segType == PathIterator.SEG_LINETO;

      float lastX = curX;
      float lastY = curY;
      curX = coords[0];
      curY = coords[1];

      if (curIndex > 0 && curX < lastX) {
        // This can happen only for a filled line
        // The second last point must be with maximum Y
        assert equals(maxX, lastX) && equals(maxY, lastY);
        break;
      }

      if (curIndex == 0 || curX >= pixel) {
        // We entered into a new pixel

        if (curIndex > 0) {
          // Add min and max points from the previous pixel
          addMinMaxPoints(resultPath, config, minIndex, minX, minY, maxIndex, maxX, maxY);

          // Add the last point from the previous pixel
          addToResultPath(resultPath, config, lastX, lastY);
        }

        pixel = (float)Math.floor(curX) + 1;
        minX = maxX = curX;
        minY = maxY = curY;
        minIndex = maxIndex = curIndex;

        // Add the first point from the current pixel
        addToResultPath(resultPath, config, curX, curY);
      } else {
        // We are in the same pixel

        if (minY > curY) {
          minIndex = curIndex;
          minX = curX;
          minY = curY;
        }

        if (maxY <= curY) {
          maxIndex = curIndex;
          maxX = curX;
          maxY = curY;
        }
      }

      iterator.next();
      curIndex++;
    }

    addMinMaxPoints(resultPath, config, minIndex, minX, minY, maxIndex, maxX, maxY);
    addToResultPath(resultPath, config, curX, curY);

    if (config.isStepped()) {
      // The last point won't be added if Y value is the same with previous point, so let's add it
      if (resultPath.getCurrentPoint() == null || equals((float)resultPath.getCurrentPoint().getY(), curY)) {
        addToPath(resultPath, curX, curY);
      }
    }
    return resultPath;
  }

  private static void addMinMaxPoints(@NotNull Path2D path,
                               @NotNull LineConfig config,
                               int minIndex, float minX, float minY,
                               int maxIndex, float maxX, float maxY) {
    if (minIndex < maxIndex) {
      addToResultPath(path, config, minX, minY, maxX, maxY);
    } else {
      addToResultPath(path, config, maxX, maxY, minX, minY);
    }
  }

  private static void addToResultPath(@NotNull Path2D path, @NotNull LineConfig config, float ...coords) {
    assert coords.length % 2 == 0;
    for (int i = 0; i < coords.length; i += 2) {
      float x = coords[i], y = coords[i + 1];
      if (config.isStepped()) {
        addToSteppedLinePath(path, x, y);
      } else {
        addToPath(path, x, y);
      }
    }
  }

  private static void addToSteppedLinePath(@NotNull Path2D path, float x, float y) {
    if (path.getCurrentPoint() == null) {
      path.moveTo(x, y);
    } else {
      if (!equals(y, (float)path.getCurrentPoint().getY())) {
        addToPath(path, x, (float)path.getCurrentPoint().getY());
        addToPath(path, x, y);
      }
    }
  }

  private static void addToPath(@NotNull Path2D path, float x, float y) {
    if (path.getCurrentPoint() == null) {
      path.moveTo(x, y);
    } else {
      // Don't repeat the current point
      if (!equals((float)path.getCurrentPoint().getX(), x) || !equals((float)path.getCurrentPoint().getY(), y)) {
        path.lineTo(x, y);
      }
    }
  }

  private static boolean equals(float a, float b) {
    return Math.abs(a - b) <= EPS;
  }
}
