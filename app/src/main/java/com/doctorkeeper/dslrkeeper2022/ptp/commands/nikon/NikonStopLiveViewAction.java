/**
 * Copyright 2013 Nils Assbeck, Guersel Ayaz and Michael Zoech
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
package com.doctorkeeper.dslrkeeper2022.ptp.commands.nikon;

import com.doctorkeeper.dslrkeeper2022.ptp.NikonCamera;
import com.doctorkeeper.dslrkeeper2022.ptp.PtpAction;
import com.doctorkeeper.dslrkeeper2022.ptp.PtpCamera.IO;
import com.doctorkeeper.dslrkeeper2022.ptp.PtpConstants.Operation;
import com.doctorkeeper.dslrkeeper2022.ptp.PtpConstants.Response;
import com.doctorkeeper.dslrkeeper2022.ptp.commands.SimpleCommand;

public class NikonStopLiveViewAction implements PtpAction {

    private final NikonCamera camera;
    private final boolean notifyUser;

    public NikonStopLiveViewAction(NikonCamera camera, boolean notifyUser) {
        this.camera = camera;
        this.notifyUser = notifyUser;
    }

    @Override
    public void exec(IO io) {
        SimpleCommand simpleCmd = new SimpleCommand(camera, Operation.NikonEndLiveView);
        io.handleCommand(simpleCmd);

        if (simpleCmd.getResponseCode() == Response.DeviceBusy) {
            camera.onDeviceBusy(this, true);
        } else {
            if (notifyUser) {
                camera.onLiveViewStopped();
            } else {
                camera.onLiveViewStoppedInternal();
            }
        }
    }

    @Override
    public void reset() {
    }
}
