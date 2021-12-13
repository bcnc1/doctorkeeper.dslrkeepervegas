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

import java.nio.ByteBuffer;

import com.doctorkeeper.dslrkeeper2022.ptp.NikonCamera;
import com.doctorkeeper.dslrkeeper2022.ptp.PtpCamera.IO;
import com.doctorkeeper.dslrkeeper2022.ptp.PtpConstants.Operation;
import com.doctorkeeper.dslrkeeper2022.ptp.PtpConstants.Response;

public class NikonAfDriveDeviceReadyCommand extends NikonCommand {

    public NikonAfDriveDeviceReadyCommand(NikonCamera camera) {
        super(camera);
    }

    @Override
    public void exec(IO io) {
        io.handleCommand(this);
        if (getResponseCode() == Response.DeviceBusy) {
            reset();
            camera.enqueue(this, 200);
        } else {
            camera.onFocusEnded(getResponseCode() == Response.Ok);
        }
    }

    @Override
    public void encodeCommand(ByteBuffer b) {
        encodeCommand(b, Operation.NikonDeviceReady);
    }
}
