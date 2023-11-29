/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.things.lowpan.transmitter;

import android.os.Build;

class Utils {

  static String getUartConnection() {
    String DEVICE_RPI3 = "rpi3";
    String DEVICE_IMX7D_PICO = "imx7d_pico";
    if (Build.DEVICE.equals(DEVICE_RPI3)) {
      return "USB1-1.4:1.1";
    } else if (Build.DEVICE.equals(DEVICE_IMX7D_PICO)) {
      return "USB1-1:1.1";
    }
    return "unknown";
  }
}
