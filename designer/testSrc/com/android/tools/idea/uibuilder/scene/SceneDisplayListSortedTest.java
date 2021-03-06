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
package com.android.tools.idea.uibuilder.scene;

import com.android.tools.idea.uibuilder.fixtures.ModelBuilder;
import com.android.tools.idea.uibuilder.scene.draw.DisplayList;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

import static com.android.SdkConstants.CONSTRAINT_LAYOUT;
import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.TEXT_VIEW;

public class SceneDisplayListSortedTest extends SceneTest {
  @Override
  @NotNull
  public ModelBuilder createModel() {
    return model("constraint.xml",
                 component(CONSTRAINT_LAYOUT)
                   .id("@+id/root")
                   .withBounds(0, 0, 1000, 1000)
                   .width("1000dp")
                   .height("1000dp")
                   .withAttribute("android:padding", "20dp")
                   .children(
                     component(LINEAR_LAYOUT)
                       .id("@+id/linear")
                       .withBounds(10, 10, 990, 20)
                       .width("980dp")
                       .height("20dp")
                       .children(
                         component(TEXT_VIEW)
                           .id("@+id/button1")
                           .withBounds(10, 10, 990, 20)
                           .width("100dp")
                           .height("20dp")
                       ),
                     component(LINEAR_LAYOUT)
                       .id("@+id/linear2")
                       .withBounds(10, 100, 990, 20)
                       .width("980dp")
                       .height("20dp")
                       .children(
                         component(TEXT_VIEW)
                           .id("@+id/button2")
                           .withBounds(10, 100, 990, 20)
                           .width("100dp")
                           .height("20dp")
                       )
                   ));
  }

  public void testBasicScene() {
    String simpleList = "DrawComponentFrame,0,0,1000,1000,1\n" +
                        "Clip,0,0,1000,1000\n" +
                        "DrawComponentBackground,10,10,990,20,1\n" +
                        "DrawComponentFrame,10,10,990,20,1\n" +
                        "Clip,10,10,990,20\n" +
                        "DrawComponentBackground,10,10,990,20,1\n" +
                        "DrawTextRegion,10,10,990,20,0,false,false,5,5,\"\"\n" +
                        "DrawComponentFrame,10,10,990,20,1\n" +
                        "UNClip\n" +
                        "DrawComponentBackground,10,100,990,20,1\n" +
                        "DrawComponentFrame,10,100,990,20,1\n" +
                        "Clip,10,100,990,20\n" +
                        "DrawComponentBackground,10,100,990,20,1\n" +
                        "DrawTextRegion,10,100,990,20,0,false,false,5,5,\"\"\n" +
                        "DrawComponentFrame,10,100,990,20,1\n" +
                        "UNClip\n" +
                        "UNClip\n";

    assertEquals(simpleList, myInteraction.getDisplayList().serialize());
    DisplayList disp = DisplayList.getDisplayList(simpleList);
    assertEquals(simpleList, DisplayList.getDisplayList(simpleList).serialize());
    //noinspection UndesirableClassUsage
    BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_ARGB);
    disp.paint(img.createGraphics(), SceneContext.get());
    assertEquals(17, disp.getCommands().size());
    String result = disp.generateSortedDisplayList(SceneContext.get());
    String sorted = "DrawComponentFrame,0,0,1000,1000,1\n" +
                    "Clip,0,0,1000,1000\n" +
                    "DrawComponentBackground,10,10,990,20,1\n" +
                    "DrawComponentFrame,10,10,990,20,1\n" +
                    "Clip,10,10,990,20\n" +
                    "DrawComponentBackground,10,10,990,20,1\n" +
                    "DrawComponentFrame,10,10,990,20,1\n" +
                    "DrawTextRegion,10,10,990,20,0,false,false,5,5,\"\"\n" +
                    "UNClip\n" +
                    "\n" +
                    "DrawComponentBackground,10,100,990,20,1\n" +
                    "DrawComponentFrame,10,100,990,20,1\n" +
                    "Clip,10,100,990,20\n" +
                    "DrawComponentBackground,10,100,990,20,1\n" +
                    "DrawComponentFrame,10,100,990,20,1\n" +
                    "DrawTextRegion,10,100,990,20,0,false,false,5,5,\"\"\n" +
                    "UNClip\n" +
                    "\n" +
                    "UNClip\n\n";
    assertEquals(sorted, result);
    disp.clear();
  }
}